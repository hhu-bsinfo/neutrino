package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.Operation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.ReadOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.SendOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.WriteOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.DrainableSubscriber;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import de.hhu.bsinfo.neutrino.api.network.impl.util.ConnectionEvent;
import de.hhu.bsinfo.neutrino.api.network.impl.util.NeutrinoOutbound;
import de.hhu.bsinfo.neutrino.api.network.impl.util.OperationAggregator;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePoller;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Slf4j
public class SendAgent extends EpollAgent implements NeutrinoOutbound {

    private static final int MAX_BATCH_SIZE = 100;

    /**
     * The buffer pool used by this agent.
     */
    private final BufferPool bufferPool;

    /**
     * Helper object for aggregating operations.
     */
    private final OperationAggregator aggregator;

    /**
     * Helper object used to poll completion queues.
     */
    private final QueuePoller queuePoller;

    public SendAgent(SharedResources sharedResources) {
        super(ConnectionEvent.QUEUE_READY, ConnectionEvent.SEND_READY);
        bufferPool = sharedResources.bufferPool();
        aggregator = OperationAggregator.create(MAX_BATCH_SIZE);
        queuePoller = new QueuePoller(MAX_BATCH_SIZE);
    }

    @Override
    public Mono<Void> send(InternalConnection connection, Publisher<SendOperation> operations) {
        return subscribeTo(connection, operations);
    }

    @Override
    public Mono<Void> write(InternalConnection connection, Publisher<WriteOperation> operations) {
        return subscribeTo(connection, operations);
    }

    @Override
    public Mono<Void> read(InternalConnection connection, Publisher<ReadOperation> operation) {
        return subscribeTo(connection, operation);
    }

    private static Mono<Void> subscribeTo(InternalConnection connection, Publisher<? extends Operation> operations) {
        return Mono.defer(() -> {
            var subscriber = new OperationSubscriber();
            connection.addSubscriber(subscriber);
            operations.subscribe(subscriber);
            return subscriber.onDispose();
        });
    }

    @Override
    protected void processConnection(InternalConnection connection, ConnectionEvent event) {
        switch (event) {

            // The queue has free slots
            case QUEUE_READY:
                onQueueReady(connection);
                break;

            // A work request was processed
            case SEND_READY:
                onSendReady(connection);
                break;

            // We did not subscribe for other events
            default:
                throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    private void onSendReady(InternalConnection connection) {

        // Get connection resources and state
        var resources = connection.getResources();
        var state = connection.getState();

        // Get the completion queue on which the event occured
        var channel = resources.getSendCompletionChannel();
        var queue = channel.getCompletionEvent();

        // Acknowledge the event and request further notifications
        queue.acknowledgeEvent();
        queue.requestNotification();

        // Drain the completion queue
        var processed = queuePoller.drain(queue, this::handleSendCompletion);

        // Increment free slots
        connection.incrementFreeSlots(processed);
    }

    private void onQueueReady(InternalConnection connection) {

        var state = connection.getState();

        // Return early if no publisher is registered
        var currentPublishers = connection.getSubscribers();
        if (currentPublishers.length == 0 && state.getPending() == 0) {
            return;
        }

        // Get the number of free slots. This operation
        // will reset the counter's value.
        var freeSlots = connection.getFreeSlots();

        // Reset the aggregator
        aggregator.reset();

        // Iterate over all publishers and post their buffers in form of send work requests
        // onto the queue pair's send work request queue.
        for (DrainableSubscriber<Operation, Operation> publisher : currentPublishers) {

            // Calculate the next batch size
            if (freeSlots == 0) {
                break;
            }

            // Remove the current publisher if it has completed
            if (publisher.hasCompleted()) {
                connection.removeSubscriber(publisher);
                publisher.onRemove();
                continue;
            }

            // Drain the current publishers buffer queue
            var bufferCount = publisher.drain(aggregator, Math.min(freeSlots, MAX_BATCH_SIZE));
            freeSlots -= bufferCount;

            // Request more buffers
            if (bufferCount > 0) {
                publisher.request(bufferCount);
            }
        }

        // Post the aggregated work requests to the queue pair
        var queuePair = connection.getQueuePair();
        var postedWorkRequests = aggregator.commit(queuePair);

        // Write back free slots
        if (freeSlots > 0) {
            connection.incrementFreeSlots(freeSlots);
        }
    }

    private void handleSendCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
        var opCode = workCompletion.getOpCode();
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work completion [{}, {}] failed with status {}: {}", workCompletion.getOpCode(), (int) id, status, workCompletion.getStatusMessage());
        }

        switch (opCode) {
            case SEND:
                bufferPool.release((int) id);
                break;
            case RDMA_READ:
                break;
            case RDMA_WRITE:
                break;
            case FETCH_ADD:
                break;
            case COMP_SWAP:
                break;
        }
    }

    public final BufferPool.PooledByteBuf copyOf(ByteBuf buffer) {
        var target = bufferPool.leaseNext();

        // Remember number of bytes to send
        var messageSize = buffer.readableBytes();

        // Copy bytes into send buffer
        target.writeBytes(buffer);
        buffer.release();

        return target;
    }

    public final BufferPool.PooledByteBuf leaseBuffer() {
        return bufferPool.leaseNext();
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
}
