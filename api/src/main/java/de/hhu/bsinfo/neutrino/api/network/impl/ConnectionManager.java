package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.*;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.AgentResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairState;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ConnectionManager {

    private static final int MAX_CONNECTIONS = 1024;

    private final InfinibandDevice device;

    private final InfinibandDeviceConfig deviceConfig;

    private final NetworkConfiguration networkConfig;

    private final AtomicInteger connectionCounter = new AtomicInteger();

    private final SharedResources sharedResources;

    // TODO(krakowski):
    //  let connections array grow dynamically
    private final InternalConnection[] connections = new InternalConnection[MAX_CONNECTIONS];

    public ConnectionManager(SharedResources sharedResources) {
        device = sharedResources.device();
        deviceConfig = sharedResources.deviceConfig();
        networkConfig = sharedResources.networkConfig();
        this.sharedResources = sharedResources;
    }

    public InternalConnection connect(Negotiator negotiator, Mtu mtu, NetworkHandler networkHandler, SharedReceiveQueue receiveQueue, AgentResources agentResources, NetworkService networkService) throws IOException {

        var sendProtectionDomain = agentResources.protectionDomain();

        // Create resources for new queue pair
        var queuePairResources = QueuePairResources.create(device, networkConfig);

        // Create initial attributes
        var initialAttributes = createInitialAttributes(queuePairResources, receiveQueue);

        // Create queue pair
        var queuePair = createQueuePair(initialAttributes, sendProtectionDomain);

        // Create new connection
        var connection = createConnection(queuePair, queuePairResources, networkHandler, networkService);

        // Exchange queue pair information with remote peer
        var remote = exchangeInfo(negotiator, connection);

        // Connect to the remote's queue pair
        connect(queuePair, remote, mtu);

        log.debug("Established connection with {}:{}", remote.getLocalId(), remote.getQueuePairNumber());

        connections[connection.getId()] = connection;

        return connection;
    }

    public InternalConnection get(InfinibandChannel infinibandChannel) {
        return get(infinibandChannel.getId());
    }

    private InternalConnection get(int id) {
        return Objects.requireNonNull(connections[id], "Connection does not exit");
    }

    private QueuePair.InitialAttributes createInitialAttributes(QueuePairResources queuePairResources, SharedReceiveQueue receiveQueue) {
        return new QueuePair.InitialAttributes.Builder(
                QueuePair.Type.RC,
                queuePairResources.getSendCompletionQueue(),
                queuePairResources.getReceiveCompletionQueue(),
                networkConfig.getQueuePairSize(),
                networkConfig.getQueuePairSize(),
                networkConfig.getMaxScatterGatherElements(),
                networkConfig.getMaxScatterGatherElements()
        ).withSharedReceiveQueue(receiveQueue).build();
    }

    private QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes, ProtectionDomain protectionDomain) throws IOException {
        var queuePair = protectionDomain.createQueuePair(initialAttributes);
        queuePair.modify(new QueuePair.Attributes.Builder()
                .withState(QueuePair.State.INIT)
                .withPartitionKeyIndex((short) 0)
                .withPortNumber(deviceConfig.getPortNumber())
                .withAccessFlags(AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_WRITE, AccessFlag.REMOTE_READ));

        return queuePair;
    }

    private InternalConnection createConnection(QueuePair queuePair, QueuePairResources queuePairResources, NetworkHandler networkHandler, NetworkService networkService) {

        // Query queue pair attributes to set initial queue pair state
        var attributes = queuePair.queryAttributes(QueuePair.AttributeFlag.CAP);
        var state = new QueuePairState(attributes.capabilities.getMaxSendWorkRequests(), 0);

        // Create event file descriptor for tracking free space on the queue pair
        var queueDescriptor = EventFileDescriptor.create(1, EventFileDescriptor.OpenMode.NONBLOCK);

        // Get the next connection id
        var id = connectionCounter.getAndIncrement();

        var channel = new InfinibandChannel(id, networkService);

        // Create a new connection
        return InternalConnection.builder()
                .id(id)
                .localId(device.getPortAttributes().getLocalId())
                .portNumber(deviceConfig.getPortNumber())
                .queuePair(queuePair)
                .resources(queuePairResources)
                .state(state)
                .queueFileDescriptor(queueDescriptor)
                .networkHandler(networkHandler)
                .channel(channel)
                .build();
    }

    private static QueuePairAddress exchangeInfo(Negotiator negotiator, InternalConnection connection) {
        return negotiator.exchange(QueuePairAddress.builder()
                .localId(connection.getLocalId())
                .portNumber(connection.getPortNumber())
                .queuePairNumber(connection.getQueuePair().getQueuePairNumber()).build());
    }

    private void connect(QueuePair queuePair, QueuePairAddress remote, Mtu mtu) throws IOException {
        queuePair.modify(QueuePair.Attributes.Builder
                .buildReadyToReceiveAttributesRC(remote.getQueuePairNumber(), remote.getLocalId(), remote.getPortNumber())
                .withPathMtu(device.getPortAttributes().getMaxMtu())
                .withReceivePacketNumber(0)
                .withMaxDestinationAtomicReads((byte) 1)
                .withMinRnrTimer(networkConfig.getRnrTimer())
                .withServiceLevel(networkConfig.getServiceLevel())
                .withSourcePathBits((byte) 0)
                .withIsGlobal(false));

        queuePair.modify(QueuePair.Attributes.Builder.buildReadyToSendAttributesRC()
                .withTimeout(networkConfig.getTimeout())
                .withRetryCount(networkConfig.getRetryCount())
                .withRnrRetryCount(networkConfig.getRnrRetryCount()));
    }

}
