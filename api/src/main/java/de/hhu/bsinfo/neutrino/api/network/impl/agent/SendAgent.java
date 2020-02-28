package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.QueuedPipe;
import org.agrona.hints.ThreadHints;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.UnicastProcessor;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

@Slf4j
public class SendAgent extends EpollAgent implements NeutrinoOutbound {

    private static final int MAX_BATCH_SIZE = 100;

    /**
     * Publishers emitting new buffers to send.
     */
    private volatile BufferPublisher[] publishers = new BufferPublisher[0];
    private static final AtomicReferenceFieldUpdater<SendAgent, BufferPublisher[]> PUBLISHER =
            AtomicReferenceFieldUpdater.newUpdater(SendAgent.class, BufferPublisher[].class, "publishers");

    /**
     * The buffer pool used by this agent.
     */
    private final BufferPool bufferPool;

    /**
     * Helper object for work request aggregation.
     */
    private final WorkRequestAggregator aggregator;

    /**
     * Helper object used to poll completion queues.
     */
    private final QueuePoller queuePoller;

    public SendAgent(SharedResources sharedResources) {
        super(EpollEvent.QUEUE_READY, EpollEvent.SEND_READY);
        bufferPool = sharedResources.bufferPool();
        aggregator = new WorkRequestAggregator(MAX_BATCH_SIZE);
        queuePoller = new QueuePoller(MAX_BATCH_SIZE);
    }

    @Override
    public Mono<Void> send(InternalConnection connection, Publisher<ByteBuf> data) {
        return Mono.defer(() -> {
            var subscriber = new BufferPublisher(this, connection);
            data.subscribe(subscriber);
            return subscriber.onDispose();
        });
    }

    @Override
    protected void processConnection(InternalConnection connection, EpollEvent event) {

        var state = connection.getState();

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

            var connection = publisher.connection;
            var state = connection.getState();

            // Calculate the next batch size
            var batchSize = state.getPending();
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

        var processedCompletions = queuePoller.drain(completionQueue, this::handleSendCompletion);
        pendingRequests -= processedCompletions;

        return postedWorkRequests + processedCompletions;
    }

    private void handleSendCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
        }

        bufferPool.release((int) id);
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

    private static final class BufferPublisher extends BaseSubscriber<ByteBuf> {

        private enum Status {
            NONE, SUBSCRIBE, CANCEL, ERROR, COMPLETE
        }

        /**
         * This publisher's current status.
         */
        private volatile Status status = Status.NONE;
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
         * The connection used by this publisher.
         */
        private final InternalConnection connection;

        private BufferPublisher(SendAgent agent, InternalConnection connection) {
            this.agent = agent;
            this.connection = connection;
            hookIn();
        }

        private Mono<Void> onDispose() {
            return onDispose;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            // Event loop will request elements
        }

        @Override
        protected void hookOnNext(ByteBuf buffer) {
            var buffers = connection.getBuffers();
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
}
