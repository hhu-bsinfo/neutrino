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

    private ConnectionManager connectionManager;

    private CompletionQueue completionQueue;
    private SharedReceiveQueue sharedReceiveQueue;

    private BufferPool sendBuffers;
    private BufferPool receiveBuffers;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onInit(ConnectionServiceConfig config) {
        completionQueue = coreService.createCompletionQueue(config.getCompletionQueueSize());
        if (completionQueue == null) {
            throw new InitializationException("Creating completion queue failed");
        }

        var initialAttribtues = new SharedReceiveQueue.InitialAttributes.Builder(
                getConfig().getReceiveQueueSize(), getConfig().getMaxScatterGatherElements()).build();

        sharedReceiveQueue = coreService.createSharedReceiveQueue(initialAttribtues);

        if (sharedReceiveQueue == null) {
            throw new InitializationException("Creating shared receive queue failed");
        }

        connectionManager = new ConnectionManager(this::newConnection, this::connect);
        sendBuffers = new BufferPool(() -> coreService.registerMemory(getConfig().getConnectionBufferSize()));
        receiveBuffers = new BufferPool(() -> coreService.registerMemory(getConfig().getConnectionBufferSize()));
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
        var initialAttributes = new QueuePair.InitialAttributes.Builder(
                QueuePair.Type.RC,
                completionQueue,
                completionQueue,
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

        return new Connection(queuePair, coreService.getLocalId(), getConfig().getPortNumber());
    }

    private void connect(final QueuePair queuePair, final RemoteQueuePair remote) {
        queuePair.modify(QueuePair.Attributes.Builder
                .buildReadyToReceiveAttributesRC(remote.getQueuePairNumber(), remote.getLocalId(), remote.getPortNumber())
                .withPathMtu(Mtu.MTU_4096)
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
    public RegisteredBuffer getSendBuffer(Connection connection) {
        return sendBuffers.get(connection);
    }

    @Override
    public RegisteredBuffer getReceiveBuffer(Connection connection) {
        return receiveBuffers.get(connection);
    }
}
