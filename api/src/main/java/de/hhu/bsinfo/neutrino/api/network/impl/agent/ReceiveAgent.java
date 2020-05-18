package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.NetworkMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.ReceiveRing;
import de.hhu.bsinfo.neutrino.api.network.impl.metrics.ReceiveMetrics;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
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
     * Helper object used to poll completion queues.
     */
    private final QueuePoller queuePoller;

    /**
     * Method reference to completion handler function.
     */
    private final BiConsumer<InternalConnection, WorkCompletion> completionHandler = this::handleWorkCompletion;

    /**
     * A ring buffer which holds receive work requests and their corresponding data buffers.
     */
    private final ReceiveRing receiveRing;

    /**
     * The network metrics.
     */
    private final NetworkMetrics metrics;

    /**
     * Metrics regarding the receive agent.
     */
    private final ReceiveMetrics receiveMetrics;

    public ReceiveAgent(int index, SharedResources sharedResources) throws IOException {
        super(sharedResources.networkConfig().getEpollTimeout(), INTERESTS);

        var device = sharedResources.device();
        var deviceConfig = sharedResources.deviceConfig();
        var networkConfig = sharedResources.networkConfig();

        var builder = new SharedReceiveQueue.InitialAttributes.Builder(
                networkConfig.getSharedReceiveQueueSize(),
                networkConfig.getMaxScatterGatherElements()
        );

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
        receiveQueue = protectionDomain.createSharedReceiveQueue(builder.build());
        var receiveQueueSize = receiveQueue.queryAttributes().getMaxWorkRequests();

        receiveRing = new ReceiveRing(receiveQueueSize * 2, protectionDomain::registerMemoryRegion);
        queuePoller = new QueuePoller(receiveQueueSize);
        resources = AgentResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .threadDomain(threadDomain)
                .protectionDomain(protectionDomain)
                .build();

        receiveMetrics = new ReceiveMetrics(sharedResources.meterRegistry(), index);

//        receiveMetrics.refillTime().start();
        receiveRing.post(receiveQueue, receiveQueueSize);
//        receiveMetrics.refillTime().stop();
    }

    @Override
    protected void processConnection(InternalConnection connection, ConnectionEvent event) throws IOException {

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

        // Poll completions. We use a completion array as big as the shared receive queue.
        // This way, we know that no completions can be left on the completion queue.
        var completions = queuePoller.poll(queue);
        var length = completions.getLength();
//        receiveMetrics.processedRequests().increment(length);

        // Refill the receive queue
//        receiveMetrics.refillTime().start();
        receiveRing.post(receiveQueue, length);
//        receiveMetrics.refillTime().stop();

        // Handle work completions
        for (int i = 0; i < length; i++) {
            handleWorkCompletion(connection, completions.get(i));
        }
    }

    private void handleWorkCompletion(InternalConnection connection, WorkCompletion workCompletion) {

        // Get work completion id and status
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();

        // Check if work completion was successful
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Receive work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
            return;
        }

        // Get the network handler associated with this connection
        var handler = connection.getNetworkHandler();

        // Advance receive buffer by number of bytes received
        var source = receiveRing.get((int) workCompletion.getId());

        // Pass message to network handler
        handler.onMessage(connection.getChannel(), source, 0, workCompletion.getByteCount());
    }

    public AgentResources getResources() {
        return resources;
    }

    public SharedReceiveQueue getReceiveQueue() {
        return receiveQueue;
    }

    @Override
    public void onStart() {
        super.onStart();
        log.debug("Using {}", receiveRing);
    }

    @Override
    public String roleName() {
        return "receive";
    }
}
