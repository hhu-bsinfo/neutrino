package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairState;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

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
    //  let connections array grow
    private final InternalConnection[] connections = new InternalConnection[MAX_CONNECTIONS];

    public ConnectionManager(SharedResources sharedResources) {
        device = sharedResources.device();
        deviceConfig = sharedResources.deviceConfig();
        networkConfig = sharedResources.networkConfig();
        this.sharedResources = sharedResources;
    }

    public InternalConnection connect(Negotiator negotiator, Mtu mtu) {

        // Create resources for new queue pair
        var queuePairResources = QueuePairResources.create(device, networkConfig);

        // Create initial attributes
        var initialAttributes = createInitialAttributes(queuePairResources);

        // Create queue pair
        var queuePair = createQueuePair(initialAttributes);

        // Create new connection
        var connection = createConnection(queuePair, queuePairResources);

        // Exchange queue pair information with remote peer
        var remote = exchangeInfo(negotiator, connection);

        // Connect to the remote's queue pair
        connect(queuePair, remote, mtu);

        log.debug("Established connection with {}:{}", remote.getLocalId(), remote.getQueuePairNumber());

        connections[connection.getId()] = connection;

        return connection;
    }

    public InternalConnection get(Connection connection) {
        return get(connection.getId());
    }

    public InternalConnection get(int id) {
        return Objects.requireNonNull(connections[id], "Connection does not exit");
    }

    private QueuePair.InitialAttributes createInitialAttributes(QueuePairResources queuePairResources) {
        return new QueuePair.InitialAttributes.Builder(
                QueuePair.Type.RC,
                queuePairResources.getSendCompletionQueue(),
                queuePairResources.getReceiveCompletionQueue(),
                networkConfig.getQueuePairSize(),
                networkConfig.getQueuePairSize(),
                networkConfig.getMaxScatterGatherElements(),
                networkConfig.getMaxScatterGatherElements()
        ).withSharedReceiveQueue(sharedResources.sharedReceiveQueue()).build();
    }

    private QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes) {
        var queuePair = device.createQueuePair(initialAttributes);
        queuePair.modify(new QueuePair.Attributes.Builder()
                .withState(QueuePair.State.INIT)
                .withPartitionKeyIndex((short) 0)
                .withPortNumber(deviceConfig.getPortNumber())
                .withAccessFlags(AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_WRITE, AccessFlag.REMOTE_READ));

        return queuePair;
    }

    private BufferPool createBufferPool(QueuePair queuePair) {
        var attributes = queuePair.queryAttributes(QueuePair.AttributeFlag.CAP);
        var maxMtu = device.getPortAttributes().getMaxMtu().getMtuValue();
        return BufferPool.create("send", maxMtu, attributes.capabilities.getMaxSendWorkRequests(), device::wrapRegion);
    }

    private InternalConnection createConnection(QueuePair queuePair, QueuePairResources queuePairResources) {
        var attributes = queuePair.queryAttributes(QueuePair.AttributeFlag.CAP);
        var state = new QueuePairState(attributes.capabilities.getMaxSendWorkRequests(), 0);
        var buffers = new ManyToOneConcurrentArrayQueue<BufferPool.IndexedByteBuf>(attributes.capabilities.getMaxSendWorkRequests());
        var queueDescriptor = EventFileDescriptor.create(attributes.capabilities.getMaxSendWorkRequests(), EventFileDescriptor.OpenMode.NONBLOCK);
        return InternalConnection.builder()
                .id(connectionCounter.getAndIncrement())
                .localId(device.getPortAttributes().getLocalId())
                .portNumber(deviceConfig.getPortNumber())
                .queuePair(queuePair)
                .resources(queuePairResources)
                .state(state)
                .queueFileDescriptor(queueDescriptor)
                .build();
    }

    private static QueuePairAddress exchangeInfo(Negotiator negotiator, InternalConnection connection) {
        return negotiator.exchange(QueuePairAddress.builder()
                .localId(connection.getLocalId())
                .portNumber(connection.getPortNumber())
                .queuePairNumber(connection.getQueuePair().getQueuePairNumber()).build());
    }

    private void connect(QueuePair queuePair, QueuePairAddress remote, Mtu mtu) {
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
