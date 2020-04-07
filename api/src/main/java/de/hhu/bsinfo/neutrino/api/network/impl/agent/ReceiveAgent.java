package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.NetworkMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
public class ReceiveAgent extends EpollAgent {

    private static final ConnectionEvent[] INTERESTS = { ConnectionEvent.RECEIVE_READY };

    /**
     * This agent's resources;
     */
    private final AgentResources resources;

    /**
     * The shared receive queue.
     */
    private final SharedReceiveQueue receiveQueue;

    /**
     * The buffer pool used for receiving data.
     */
    private final BufferPool bufferPool;

    /**
     * Helper object to fill up the receive queue.
     */
    private final QueueFiller queueFiller;

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

    public ReceiveAgent(SharedResources sharedResources) {
        super(sharedResources.networkConfig().getEpollTimeout(), INTERESTS);

        var device = sharedResources.device();
        var deviceConfig = sharedResources.deviceConfig();
        var networkConfig = sharedResources.networkConfig();

        var builder = new SharedReceiveQueue.InitialAttributes.Builder(
                networkConfig.getSharedReceiveQueueSize(),
                networkConfig.getMaxScatterGatherElements()
        );

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
        receiveQueue = protectionDomain.createSharedReceiveQueue(builder.build());
        bufferPool = new BufferPool(protectionDomain::registerMemoryRegion, receiveQueue.queryAttributes().getMaxWorkRequests() * 2, networkConfig.getMtu());
        queuePoller = new QueuePoller(receiveQueue.queryAttributes().getMaxWorkRequests());
        queueFiller = new QueueFiller(bufferPool, receiveQueue.queryAttributes().getMaxWorkRequests());
        resources = AgentResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .threadDomain(threadDomain)
                .protectionDomain(protectionDomain)
                .build();

        queueFiller.fillUp(receiveQueue);
        metrics.incrementReceiveRequests(receiveQueue.queryAttributes().getMaxWorkRequests());
    }

    @Override
    protected void processConnection(InternalConnection connection, ConnectionEvent event) {

        // Check if we got the right kind of event
        if (event != ConnectionEvent.RECEIVE_READY) {
            throw new IllegalStateException("Unknown event");
        }

        // Get the completion channel
        var resources = connection.getResources();
        var channel = resources.getReceiveCompletionChannel();
        var queue = resources.getReceiveCompletionQueue();

        // We can ignore the completion event, since we know
        // the completion queue the event was generated for
        channel.discardCompletionEvent();

        // Acknowledge the received event and request subsequent notifications
        queue.acknowledgeEvent();
        queue.requestNotification();

        // Poll completions
        var completions = queuePoller.poll(queue);
        var length = completions.getLength();
        metrics.decrementReceiveRequests(length);

        // Fill up receive queue
        queueFiller.fillUp(receiveQueue, length);
        metrics.incrementReceiveRequests(length);

        // Handle work completions
        for (int i = 0; i < length; i++) {
            handleWorkCompletion(connection, completions.get(i));
        }
    }

    private void handleWorkCompletion(InternalConnection connection, WorkCompletion workCompletion) {

        // Get the subscriber
        var subscriber = connection.getInboundSubscriber();

        // Get work completion id and status
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();

        // Check if work completion was successful
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Receive work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
            bufferPool.release((int) id);
            subscriber.onError(new RuntimeException(workCompletion.getStatusMessage()));
        }

        // Remember the number of bytes received
        var bytesReceived = workCompletion.getByteCount();

        // Advance receive buffer by number of bytes received
        var source = bufferPool.get((int) workCompletion.getId());
        source.writerIndex(source.writerIndex() + bytesReceived);

        // Publish received data
        subscriber.onNext(source);
    }

    public AgentResources getResources() {
        return resources;
    }

    public SharedReceiveQueue getReceiveQueue() {
        return receiveQueue;
    }

    @Override
    public String roleName() {
        return "receive";
    }
}
