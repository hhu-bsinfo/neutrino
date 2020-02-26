package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ConnectionManager {

    private final InfinibandDevice device;

    private final InfinibandDeviceConfig deviceConfig;

    private final NetworkConfiguration networkConfig;

    private final AtomicInteger connectionCounter = new AtomicInteger();

    private final NetworkResources resources;

    public ConnectionManager(NetworkResources resources) {
        device = resources.device();
        deviceConfig = resources.deviceConfig();
        networkConfig = resources.networkConfig();
        this.resources = resources;
    }

    public Connection connect(Negotiator negotiator, Mtu mtu) {
        var initialAttributes = createInitialAttributes();
        var queuePair = createQueuePair(initialAttributes);
        var bufferPool = createBufferPool(queuePair);
        var connection = createConnection(queuePair, bufferPool);
        var remote = exchangeInfo(negotiator, connection);

        connect(queuePair, remote, mtu);

        log.debug("Established connection with {}:{}", remote.getLocalId(), remote.getQueuePairNumber());

        return connection;
    }

    private QueuePair.InitialAttributes createInitialAttributes() {
        return new QueuePair.InitialAttributes.Builder(
                QueuePair.Type.RC,
                resources.sendCompletionQueue(),
                resources.receiveCompletionQueue(),
                networkConfig.getQueuePairSize(),
                networkConfig.getQueuePairSize(),
                networkConfig.getMaxScatterGatherElements(),
                networkConfig.getMaxScatterGatherElements()
        ).withSharedReceiveQueue(resources.sharedReceiveQueue())
         .build();
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

    private Connection createConnection(QueuePair queuePair, BufferPool bufferPool) {
        return ConnectionImpl.builder()
                .id(connectionCounter.getAndIncrement())
                .localId(device.getPortAttributes().getLocalId())
                .portNumber(deviceConfig.getPortNumber())
                .queuePair(queuePair)
                .sendCompletionQueue(resources.sendCompletionQueue())
                .receiveCompletionQueue(resources.receiveCompletionQueue())
                .bufferPool(bufferPool)
                .build();
    }

    private static QueuePairAddress exchangeInfo(Negotiator negotiator, Connection connection) {
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
