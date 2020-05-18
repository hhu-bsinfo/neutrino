package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.NetworkMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.metrics.SendMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.util.BitMask;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

@Slf4j
public class SendAgent extends EpollAgent {

    private static final ConnectionEvent[] INTERESTS = { ConnectionEvent.QUEUE_READY, ConnectionEvent.SEND_READY };

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
     * Helper object used to process the connection's request ring buffer.
     */
    private final RequestProcessor requestProcessor = new RequestProcessor();

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

    /**
     * Metrics regarding this send agent.
     */
    private final SendMetrics sendMetrics;

    public SendAgent(int index, SharedResources sharedResources) throws IOException {
        super(sharedResources.networkConfig().getEpollTimeout(), INTERESTS);

        var device = sharedResources.device();
        var deviceConfig = sharedResources.deviceConfig();
        var networkConfig = sharedResources.networkConfig();

        ThreadDomain threadDomain = null;
        var attributes = new ThreadDomain.InitialAttributes();

        try {
            threadDomain = device.createThreadDomain(attributes);
        } catch (IOException ignored) {
        } finally {
            if (threadDomain != null) {
                threadDomain.close();
            }
        }

        ProtectionDomain protectionDomain;
        if (threadDomain == null) {
            protectionDomain = device.getProtectionDomain();
        } else {
            protectionDomain = device.getProtectionDomain().allocateParentDomain(threadDomain);
        }

        metrics = sharedResources.networkMetrics();
        queuePoller = new QueuePoller(MAX_BATCH_SIZE);
        bufferPool = new BufferPool(device::wrapRegion, networkConfig.getQueuePairSize() << 8, networkConfig.getMtu());
        resources = AgentResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .threadDomain(threadDomain)
                .protectionDomain(protectionDomain)
                .build();

        sendMetrics = new SendMetrics(sharedResources.meterRegistry(), index);
    }

    @Override
    protected void processConnection(InternalConnection connection, ConnectionEvent event) throws IOException {
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

    private void onSendReady(InternalConnection connection) throws IOException {

        // Get connection resources
        final var resources = connection.getResources();
        final var state = connection.getState();

        // Get the completion queue on which the event occured
        final var channel = resources.getSendCompletionChannel();
        final var queue = resources.getSendCompletionQueue();

//        sendMetrics.ackTime().start();

        // We can ignore the completion event, since we know
        // the completion queue the event was generated for
        channel.discardCompletionEvent();

        // Acknowledge the event and request further notifications
        queue.acknowledgeEvent();
        queue.requestNotification();

//        sendMetrics.ackTime().stop();

        // Drain the completion queue
        var processed = queuePoller.drain(queue, connection, completionHandler);
//        sendMetrics.processedRequests().increment(processed);

        // Decrease pending requests
        state.decrementPending(processed);
    }

    private void onQueueReady(InternalConnection connection) throws IOException {

        // Return early if the queue pair has no free slots left
        final var state = connection.getState();
        final var remaining = state.remaining();
        if (remaining == 0) {
            return;
        }

        // Query the number of requests queued within this connection
        final var requests = connection.getRequestBuffer();

//        sendMetrics.postTime().start();

        // Process outstanding requests
        requestProcessor.reset();
        var bytes = requests.read(requestProcessor, remaining);

        // Commit the processed requests onto the connection's queue pair
        var commited = requestProcessor.commit(connection.getQueuePair());

//        sendMetrics.postTime().stop();
//        sendMetrics.postedRequests().increment(commited);

        // Increment the number of pending work requests
        state.incrementPending(commited);

        // Release the bytes commited to the connection's queue pair
        requests.commitRead(bytes);
    }

    private void handleWorkCompletion(InternalConnection connection, WorkCompletion workCompletion) {

        // Get the associated subscriber
        var identifier = workCompletion.getId();
        var userContext = Identifier.getContext(identifier);
        var handler = connection.getNetworkHandler();

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

        // Check work completion status
        var status = workCompletion.getStatus();
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work request #{} ({}) failed with status {}", userContext, workCompletion.getOpCode(), status);
            handler.onRequestFailed(connection.getChannel(), userContext);
            return;
        }

        handler.onRequestCompleted(connection.getChannel(), userContext);
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

    public BufferPool.PooledBuffer claim() {
//        var start = System.nanoTime();
        var buffer = bufferPool.claim();
//        metrics.claimTime().record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return buffer;
    }

    @Override
    public void onStart() {
        super.onStart();
        log.debug("Using {}", bufferPool);
    }

    public AgentResources getResources() {
        return resources;
    }

    @Override
    public String roleName() {
        return "send";
    }
}
