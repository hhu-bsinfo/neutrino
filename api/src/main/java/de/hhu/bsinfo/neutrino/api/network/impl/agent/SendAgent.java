package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.impl.NetworkResources;
import de.hhu.bsinfo.neutrino.api.network.impl.NeutrinoOutbound;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentTerminationException;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

@Slf4j
public class SendAgent implements Agent, NeutrinoOutbound {

    private static final AtomicReferenceFieldUpdater<SendAgent, BufferPublisher[]> PUBLISHER =
            AtomicReferenceFieldUpdater.newUpdater(SendAgent.class, BufferPublisher[].class, "publishers");

    private volatile BufferPublisher[] publishers = new BufferPublisher[0];

    private static final int MAX_BATCH_SIZE = 100;

    private final BufferPool bufferPool;

    private final QueuePair queuePair;

    private final Connection connection;

    private final CompletionQueue completionQueue;

    private final WorkRequestAggregator aggregator;

    private final int maxRequests;

    private int pendingRequests = 0;

    /**
     * An array of work completions used for polling the completion queue.
     */
    private final CompletionQueue.WorkCompletionArray completions;

    public SendAgent(NetworkResources resources, Connection connection) {
        this.connection = connection;
        bufferPool = connection.getBufferPool();
        queuePair = connection.getQueuePair();
        completionQueue = resources.sendCompletionQueue();
        completions = new CompletionQueue.WorkCompletionArray(completionQueue.getMaxElements());

        var attributes = queuePair.queryAttributes(QueuePair.AttributeFlag.CAP);
        maxRequests = attributes.capabilities.getMaxSendWorkRequests();
        aggregator = new WorkRequestAggregator(maxRequests);
    }

    @Override
    public Mono<Void> send(Publisher<ByteBuf> data) {
        return Mono.defer(() -> {
            var subscriber = new BufferPublisher(this, MAX_BATCH_SIZE);
            data.subscribe(subscriber);
            return subscriber.onDispose();
        });
    }

    @Override
    public int doWork() {

        // Return early if no publisher is registered
        var currentPublishers = publishers;
        if (currentPublishers.length == 0 && pendingRequests == 0) {
            return 0;
        }

        // Reset the aggregator
        aggregator.reset();

        // Iterate over all publishers and post their buffers in form of send work requests
        // onto the queue pair's send work request queue.
        for (BufferPublisher publisher : currentPublishers) {

            // Calculate the next batch size
            var batchSize = maxRequests - pendingRequests;
            if (batchSize == 0) {
                break;
            }

            // Remove the current publisher if it has completed
            if (publisher.hasCompleted()) {
                publisher.hookOut();
                continue;
            }

            // Drain the current publishers buffer queue
            var bufferCount = publisher.drain(aggregator, Math.min(batchSize, MAX_BATCH_SIZE));
            pendingRequests += bufferCount;

            // Request more buffers
            if (bufferCount > 0) {
                publisher.request(bufferCount);
            }
        }

        // Post the aggregated work requests to the queue pair
        var postedWorkRequests = aggregator.commit(queuePair);

        // Process work completions
        var processedCompletions = processCompletionQueue();
        pendingRequests -= processedCompletions;

        return postedWorkRequests + processedCompletions;
    }

    @Override
    public void onStart() {
        log.debug("Starting agent");
    }

    @Override
    public void onClose() {
        log.debug("Stopping agent");
    }

    @Override
    public String roleName() {
        return "send";
    }

    private int processCompletionQueue() {
        completionQueue.poll(completions);
        completions.forEach(this::handleSendCompletion);
        return completions.getLength();
    }

    private void handleSendCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
        }

        bufferPool.release((int) id);
    }

    private static final class BufferPublisher extends BaseSubscriber<ByteBuf> {

        private enum Status {
            NONE, SUBSCRIBE, CANCEL, ERROR, COMPLETE
        }

        private static final AtomicReferenceFieldUpdater<BufferPublisher, Status> STATUS =
                AtomicReferenceFieldUpdater.newUpdater(BufferPublisher.class, Status.class, "status");

        /**
         * The agent this processor is linked with.
         */
        private final SendAgent agent;

        /**
         * Emits a sinal on disposal.
         */
        private final MonoProcessor<Void> onDispose = MonoProcessor.create();

        /**
         * This publisher's current status.
         */
        private volatile Status status = Status.NONE;

        private final ManyToOneConcurrentArrayQueue<BufferPool.IndexedByteBuf> buffers;

        private BufferPublisher(SendAgent agent, int capacity) {
            this.agent = agent;
            buffers = new ManyToOneConcurrentArrayQueue<>(capacity);
            hookIn();
        }

        private Mono<Void> onDispose() {
            return onDispose;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            subscription.request(buffers.capacity());
        }

        @Override
        protected void hookOnNext(ByteBuf buffer) {
            var target = agent.bufferPool.leaseNext();

            // Remember number of bytes to send
            var messageSize = buffer.readableBytes();

            // Copy bytes into send buffer
            target.writeBytes(buffer);
            buffer.release();

            if (!buffers.offer(target)) {
                throw Exceptions.failWithOverflow();
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            onDispose.onError(throwable);
        }

        @Override
        protected void hookOnComplete() {
            STATUS.set(this, Status.COMPLETE);
        }

        private int drain(final Consumer<BufferPool.IndexedByteBuf> byteBufConsumer, int limit) {
            return buffers.drain(byteBufConsumer, limit);
        }

        private int drain(final Consumer<BufferPool.IndexedByteBuf> byteBufConsumer){
            return buffers.drain(byteBufConsumer);
        }

        private boolean hasCompleted() {
            return status == Status.COMPLETE && buffers.isEmpty();
        }

        private boolean isEmpty() {
            return buffers.isEmpty();
        }

        private void hookIn() {
            BufferPublisher[] oldArray;
            BufferPublisher[] newArray;

            do {
                oldArray = agent.publishers;
                newArray = ArrayUtil.add(oldArray, this);
            } while (!PUBLISHER.compareAndSet(agent, oldArray, newArray));
        }

        private void hookOut() {
            BufferPublisher[] oldArray;
            BufferPublisher[] newArray;

            do {
                oldArray = agent.publishers;
                newArray = ArrayUtil.remove(oldArray, this);
            } while (!PUBLISHER.compareAndSet(agent, oldArray, newArray));

            onDispose.onComplete();
        }
    }

    private static final class WorkRequestAggregator implements Consumer<BufferPool.IndexedByteBuf> {

        private int index;

        private SendWorkRequest currentWorkRequest;

        private SendWorkRequest previousWorkRequest;

        private ScatterGatherElement currentElement;

        private final SendWorkRequest[] workRequestPool;

        private final ScatterGatherElement[] scatterGatherPool;

        private WorkRequestAggregator(int capacity) {
            var builder = new SendWorkRequest.Builder()
                    .withOpCode(SendWorkRequest.OpCode.SEND)
                    .withSendFlags(SendWorkRequest.SendFlag.SIGNALED);

            workRequestPool = new SendWorkRequest[capacity];
            scatterGatherPool = new ScatterGatherElement[capacity];
            for (int i = 0; i < workRequestPool.length; i++) {
                workRequestPool[i] = builder.build();
                scatterGatherPool[i] = new ScatterGatherElement();
            }
        }

        @Override
        public void accept(BufferPool.IndexedByteBuf data) {
            currentWorkRequest = workRequestPool[index];
            currentElement = scatterGatherPool[index];

            // Remember number of bytes to send
            var messageSize = data.readableBytes();

            // Advance send buffer by number of written bytes
            var memoryAddress = data.memoryAddress() + data.readerIndex();
            data.readerIndex(data.writerIndex());

            currentElement.setAddress(memoryAddress);
            currentElement.setLength(messageSize);
            currentElement.setLocalKey(data.getLocalKey());

            currentWorkRequest.setId(data.getIndex());
            currentWorkRequest.setListHandle(currentElement.getHandle());
            currentWorkRequest.setListLength(1);

            if (previousWorkRequest != null) {
                previousWorkRequest.linkWith(currentWorkRequest);
            }

            previousWorkRequest = currentWorkRequest;
            index++;
        }

        private void reset() {
            currentWorkRequest = null;
            previousWorkRequest = null;
            currentElement = null;
            index = 0;
        }

        private int commit(QueuePair queuePair) {
            if (index == 0) {
                return 0;
            }

            currentWorkRequest.unlink();
            queuePair.postSend(workRequestPool[0]);
            return index;
        }
    }
}
