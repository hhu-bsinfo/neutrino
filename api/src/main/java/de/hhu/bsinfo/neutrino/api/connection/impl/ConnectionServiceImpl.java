package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

import javax.inject.Inject;
import java.net.InetSocketAddress;

public class ConnectionServiceImpl extends Service<ConnectionServiceConfig> implements InternalConnectionService {

    @Inject
    private InternalCoreService coreService;

    @Inject
    private MemoryService memoryService;

    private ConnectionManager connectionManager;

    private CompletionQueue completionQueue;
    private SharedReceiveQueue sharedReceiveQueue;

    private BufferPool bufferPool;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onInit(ConnectionServiceConfig config) {
        completionQueue = coreService.createCompletionQueue(config.getCompletionQueueSize());
        if (completionQueue == null) {
            throw new InitializationException("Creating completion queue failed");
        }

        sharedReceiveQueue = coreService.createSharedReceiveQueue(configurator -> {
            configurator.attributes.setMaxWorkRequest(getConfig().getReceiveQueueSize());
            configurator.attributes.setMaxScatterGatherElements(getConfig().getMaxScatterGatherElements());
        });

        if (sharedReceiveQueue == null) {
            throw new InitializationException("Creating shared receive queue failed");
        }

        connectionManager = new ConnectionManager(this::newConnection, this::connect);
        bufferPool = new BufferPool(() -> memoryService.register(getConfig().getConnectionBufferSize()));
    }

    @Override
    protected void onShutdown() {
        disposables.dispose();
        sharedReceiveQueue.close();
        completionQueue.close();
    }

    @Override
    public Single<Connection> connect(InetSocketAddress remote) {
        return connectionManager.connect(remote);
    }

    @Override
    public Observable<Connection> listen(InetSocketAddress bindAddress) {
        return connectionManager.listen(bindAddress);
    }

    private Connection newConnection() {
        var queuePair = coreService.createQueuePair(configurator -> {
            configurator.setReceiveCompletionQueue(completionQueue);
            configurator.setSendCompletionQueue(completionQueue);
            configurator.setSharedReceiveQueue(sharedReceiveQueue);
            configurator.setType(QueuePair.Type.RC);
            configurator.capabilities.setMaxSendWorkRequests(getConfig().getCompletionQueueSize());
            configurator.capabilities.setMaxReceiveWorkRequests(getConfig().getCompletionQueueSize());
            configurator.capabilities.setMaxReceiveScatterGatherElements(getConfig().getMaxScatterGatherElements());
            configurator.capabilities.setMaxSendScatterGatherElements(getConfig().getMaxScatterGatherElements());
        });

        queuePair.modify(configurator -> {
            configurator.setState(QueuePair.State.INIT);
            configurator.setPartitionKeyIndex((short) 0);
            configurator.setPortNumber(getConfig().getPortNumber());
            configurator.setAccessFlags(AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_WRITE, AccessFlag.REMOTE_READ);
        },  QueuePair.AttributeFlag.STATE,
            QueuePair.AttributeFlag.PKEY_INDEX,
            QueuePair.AttributeFlag.PORT,
            QueuePair.AttributeFlag.ACCESS_FLAGS);

        return new Connection(queuePair, coreService.getLocalId(), getConfig().getPortNumber());
    }

    private void connect(final QueuePair queuePair, final RemoteQueuePair remote) {
        queuePair.modify(configurator -> {
            configurator.setState(QueuePair.State.RTR);
            configurator.setPathMtu(Mtu.MTU_4096);
            configurator.setDestination(remote.getQueuePairNumber());
            configurator.setReceivePacketNumber(0);
            configurator.setMaxDestinationAtomicReads((byte) 1);
            configurator.setMinRnrTimer(getConfig().getRnrTimer());
            configurator.addressHandle.setDestination(remote.getLocalId());
            configurator.addressHandle.setServiceLevel(getConfig().getServiceLevel());
            configurator.addressHandle.setPortNumber(remote.getPortNumber());
            configurator.addressHandle.setSourcePathBits((byte) 0);
            configurator.addressHandle.setIsGlobal(false);
        },  QueuePair.AttributeFlag.STATE,
            QueuePair.AttributeFlag.PATH_MTU,
            QueuePair.AttributeFlag.RQ_PSN,
            QueuePair.AttributeFlag.DEST_QPN,
            QueuePair.AttributeFlag.AV,
            QueuePair.AttributeFlag.MAX_DEST_RD_ATOMIC,
            QueuePair.AttributeFlag.MIN_RNR_TIMER);

        queuePair.modify(configurator -> {
            configurator.setState(QueuePair.State.RTS);
            configurator.setSendPacketNumber(0);
            configurator.setTimeout(getConfig().getTimeout());
            configurator.setRetryCount(getConfig().getRetryCount());
            configurator.setRnrRetryCount(getConfig().getRnrRetryCount());
            configurator.setMaxInitiatorAtomicReads((byte) 1);
        },  QueuePair.AttributeFlag.STATE,
            QueuePair.AttributeFlag.TIMEOUT,
            QueuePair.AttributeFlag.RETRY_CNT,
            QueuePair.AttributeFlag.RNR_RETRY,
            QueuePair.AttributeFlag.SQ_PSN,
            QueuePair.AttributeFlag.MAX_QP_RD_ATOMIC);
    }

    @Override
    public CompletionQueue getCompletionQueue() {
        return completionQueue;
    }

    @Override
    public SharedReceiveQueue getSharedReceiveQueue() {
        return sharedReceiveQueue;
    }

    @Override
    public QueuePair getQueuePair(Connection connection) {
        return connection.getQueuePair();
    }

    @Override
    public RegisteredBuffer getBuffer(Connection connection) {
        return bufferPool.get(connection);
    }
}
