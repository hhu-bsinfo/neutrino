package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

public class ConnectionServiceImpl extends Service<ConnectionServiceConfig> implements InternalConnectionService {

    @Inject
    private InternalCoreService coreService;

    private SharedReceiveQueue sharedReceiveQueue;

    private static final PooledByteBufAllocator ALLOCATOR = PooledByteBufAllocator.DEFAULT;

    @Override
    protected void onInit(ConnectionServiceConfig config) {
        var initialAttribtues = new SharedReceiveQueue.InitialAttributes.Builder(
                getConfig().getReceiveQueueSize(), getConfig().getMaxScatterGatherElements()).build();

        sharedReceiveQueue = coreService.createSharedReceiveQueue(initialAttribtues);

        if (sharedReceiveQueue == null) {
            throw new InitializationException("Creating shared receive queue failed");
        }
    }

    @Override
    protected void onShutdown() {
        sharedReceiveQueue.close();
    }

    @Override
    public Flux<Connection> listen() {
        return null;
    }

    @Override
    public Mono<Connection> newConnection() {
        return Mono.fromCallable(() -> {
            var sendQueue = coreService.createCompletionQueue(getConfig().getCompletionQueueSize());
            var receiveQueue = coreService.createCompletionQueue(getConfig().getCompletionQueueSize());

            var initialAttributes = new QueuePair.InitialAttributes.Builder(
                    QueuePair.Type.RC,
                    sendQueue,
                    receiveQueue,
                    getConfig().getCompletionQueueSize(),
                    getConfig().getCompletionQueueSize(),
                    getConfig().getMaxScatterGatherElements(),
                    getConfig().getMaxScatterGatherElements()).build();

            var queuePair = coreService.createQueuePair(initialAttributes);

            queuePair.modify(new QueuePair.Attributes.Builder()
                    .withState(QueuePair.State.INIT)
                    .withPartitionKeyIndex((short) 0)
                    .withPortNumber(getConfig().getPortNumber())
                    .withAccessFlags(AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_WRITE, AccessFlag.REMOTE_READ));

            var sendBuffer = coreService.registerBuffer(ALLOCATOR.directBuffer(getConfig().getConnectionBufferSize()));
            var receiveBuffer = coreService.registerBuffer(ALLOCATOR.directBuffer(getConfig().getConnectionBufferSize()));

            return ConnectionImpl.builder()
                    .localId(coreService.getLocalId())
                    .portNumber(getConfig().getPortNumber())
                    .sendBuffer(sendBuffer)
                    .receiveBuffer(receiveBuffer)
                    .queuePair(queuePair)
                    .sendCompletionQueue(sendQueue)
                    .receiveCompletionQueue(receiveQueue)
                    .build();
        });
    }

    @Override
    public Mono<Connection> connect(final Connection connection, final QueuePairAddress remote, final Mtu mtu) {
        return Mono.fromCallable(() -> {
            var queuePair = connection.getQueuePair();
            queuePair.modify(QueuePair.Attributes.Builder
                    .buildReadyToReceiveAttributesRC(remote.getQueuePairNumber(), remote.getLocalId(), remote.getPortNumber())
                    .withPathMtu(mtu)
                    .withReceivePacketNumber(0)
                    .withMaxDestinationAtomicReads((byte) 1)
                    .withMinRnrTimer(getConfig().getRnrTimer())
                    .withServiceLevel(getConfig().getServiceLevel())
                    .withSourcePathBits((byte) 0)
                    .withIsGlobal(false));

            queuePair.modify(QueuePair.Attributes.Builder.buildReadyToSendAttributesRC()
                    .withTimeout(getConfig().getTimeout())
                    .withRetryCount(getConfig().getRetryCount())
                    .withRnrRetryCount(getConfig().getRnrRetryCount()));

            return connection;
        });
    }

    @Override
    public SharedReceiveQueue getSharedReceiveQueue() {
        return sharedReceiveQueue;
    }
}
