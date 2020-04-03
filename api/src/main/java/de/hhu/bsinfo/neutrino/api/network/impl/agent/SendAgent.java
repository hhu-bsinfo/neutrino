package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.NetworkMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.util.BitMask;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.BiConsumer;

@Slf4j
public class SendAgent extends EpollAgent {

    private static final int MAX_BATCH_SIZE = 1024;

    /**
     * This agent's resources;
     */
    private final AgentResources resources;

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

    /**
     * Method reference to completion handler function.
     */
    private final BiConsumer<InternalConnection, WorkCompletion> completionHandler = this::handleWorkCompletion;

    /**
     * The network metrics.
     */
    private final NetworkMetrics metrics;

    public SendAgent(SharedResources sharedResources) {
        super(ConnectionEvent.QUEUE_READY, ConnectionEvent.SEND_READY);

        var device = sharedResources.device();
        var deviceConfig = sharedResources.deviceConfig();
        var networkConfig = sharedResources.networkConfig();

        var attributes = new ThreadDomain.InitialAttributes();
        var threadDomain = device.createThreadDomain(attributes);

        ProtectionDomain protectionDomain;
        if (threadDomain == null) {
            log.info("Thread domains are not supported. Falling back to global protection domain");
            protectionDomain = device.getProtectionDomain();
        } else {
            protectionDomain = device.getProtectionDomain().allocateParentDomain(threadDomain);
        }

        metrics = sharedResources.networkMetrics();
        queuePoller = new QueuePoller(MAX_BATCH_SIZE);
        bufferPool = new BufferPool(device::wrapRegion, networkConfig.getQueuePairSize(), networkConfig.getMtu());
        aggregator = OperationAggregator.create(MAX_BATCH_SIZE);
        resources = AgentResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .threadDomain(threadDomain)
                .protectionDomain(protectionDomain)
                .build();
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

        // Get connection resources
        var resources = connection.getResources();

        // Get the completion queue on which the event occured
        var channel = resources.getSendCompletionChannel();
        var queue = resources.getSendCompletionQueue();

        // We can ignore the completion event, since we know
        // the completion queue the event was generated for
        channel.discardCompletionEvent();

        // Acknowledge the event and request further notifications
        queue.acknowledgeEvent();
        queue.requestNotification();

        // Drain the completion queue
        var processed = queuePoller.drain(queue, connection, completionHandler);
        metrics.decrementSendRequests(processed);

        // Increment free slots
        connection.onProcessed(processed);
    }

    private void onQueueReady(InternalConnection connection) {

        // Return early if no subscriber is registered
        var currentSubscribers = connection.getSubscribers();
        if (currentSubscribers.length == 0) {
            return;
        }

        // Get the number of free slots. This operation
        // will reset the counter's value.
        var freeSlots = connection.getFreeSlots();

        // Reset the aggregator
        aggregator.reset();

        // Iterate over all publishers and post their operations in form of send work requests
        // onto the queue pair's send work request queue.
        for (OperationSubscriber subscriber : currentSubscribers) {

            // Break out of loop if there is no space left
            if (freeSlots == 0 || aggregator.remaining() == 0) {
                break;
            }

            // Remove the current publisher if it has completed
            if (subscriber.hasCompleted()) {
                connection.onCompletion(subscriber);
                continue;
            }

            // Drain the current subscriber's operation queue
            aggregator.setCurrentIdentifier(subscriber.getId());
            var operationCount = subscriber.drain(aggregator, Math.min(freeSlots, aggregator.remaining()));
            freeSlots -= operationCount;

            subscriber.incrementPending(operationCount);

            // Request more operations
            if (operationCount > 0) {
                subscriber.request(operationCount);
            }
        }

        // Post the aggregated work requests to the queue pair
        var queuePair = connection.getQueuePair();
        var postedRequests = aggregator.commit(queuePair);
        metrics.incrementSendRequests(postedRequests);

        // Write back free slots
        if (freeSlots > 0) {
            connection.onProcessed(freeSlots);
        }
    }

    private void handleWorkCompletion(InternalConnection connection, WorkCompletion workCompletion) {

        // Get the associated subscriber
        var identifier = workCompletion.getId();
        var subscriber = connection.getOutboundSubscriber(identifier);

        // Check work completion status
        var status = workCompletion.getStatus();
        if (status != WorkCompletion.Status.SUCCESS) {
            subscriber.signalError(new IOException(workCompletion.getStatusMessage()));
        }

        // Handle work completion
        var opCode = workCompletion.getOpCode();
        switch (opCode) {
            case SEND:
                onSendCompletion(identifier);
                break;
            case RDMA_READ:
                onReadCompletion(identifier);
                break;
            case RDMA_WRITE:
                onWriteCompletion(identifier);
                break;
            case FETCH_ADD:
                onFetchCompletion(identifier);
                break;
            case COMP_SWAP:
                onSwapCompletion(identifier);
                break;
        }

        // Decrement the subscriber's counter
        subscriber.decrementPending(1);

        // Check if all requests were processed
        if (subscriber.hasCompleted() && !subscriber.hasPending()) {
            subscriber.signalCompletion();
            connection.remove(subscriber);
        }
    }

    private void onSendCompletion(long identifier) {

        // Extract flags from identifier
        var flags = Identifier.getFlags(identifier);

        // Direct send operations do not need special handling
        if (BitMask.isSet(flags, RequestFlag.DIRECT)) {
            return;
        }

        // Release the used buffer using the buffer index encoded
        // within the identifier's attachement
        var attachement = Identifier.getAttachement(identifier);
        bufferPool.release(attachement);
    }

    private void onReadCompletion(long identifier) {
        // Nothing to do
    }

    private void onWriteCompletion(long identifier) {
        // Nothing to do
    }

    private void onFetchCompletion(long identifier) {
        // Nothing to do
    }

    private void onSwapCompletion(long identifier) {
        // Nothing to do
    }

    public final BufferPool.PooledBuffer copyOf(ByteBuf buffer) {
        var target = bufferPool.claim();

        // Copy bytes into send buffer
        target.writeBytes(buffer);
        buffer.release();

        return target;
    }

    public AgentResources getResources() {
        return resources;
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
