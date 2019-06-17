package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.manager.Connection;
import de.hhu.bsinfo.neutrino.api.connection.impl.manager.ConnectionManager;
import de.hhu.bsinfo.neutrino.api.connection.impl.manager.RemoteQueuePair;
import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
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
import java.util.Queue;

public class ConnectionServiceImpl extends Service<ConnectionServiceConfig> implements ConnectionService {

    @Inject
    private CoreService core;

    private ConnectionManager connectionManager;

    private CompletionQueue completionQueue;
    private SharedReceiveQueue sharedReceiveQueue;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onInit(ConnectionServiceConfig config) {
        completionQueue = core.createCompletionQueue(config.getCompletionQueueSize());
        if (completionQueue == null) {
            throw new InitializationException("Creating completion queue failed");
        }

        sharedReceiveQueue = core.createSharedReceiveQueue(configurator -> {
            configurator.attributes.setMaxWorkRequest(getConfig().getReceiveQueueSize());
            configurator.attributes.setMaxScatterGatherElements(getConfig().getMaxScatterGatherElements());
        });

        if (sharedReceiveQueue == null) {
            throw new InitializationException("Creating shared receive queue failed");
        }

        connectionManager = new ConnectionManager(this::newConnection, this::connect);
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
        var queuePair = core.createQueuePair(configurator -> {
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
        },  QueuePair.AttributeMask.STATE,
            QueuePair.AttributeMask.PKEY_INDEX,
            QueuePair.AttributeMask.PORT,
            QueuePair.AttributeMask.ACCESS_FLAGS);

        return new Connection(queuePair, core.getLocalId(), getConfig().getPortNumber());
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
        },  QueuePair.AttributeMask.STATE,
            QueuePair.AttributeMask.PATH_MTU,
            QueuePair.AttributeMask.RQ_PSN,
            QueuePair.AttributeMask.DEST_QPN,
            QueuePair.AttributeMask.AV,
            QueuePair.AttributeMask.MAX_DEST_RD_ATOMIC,
            QueuePair.AttributeMask.MIN_RNR_TIMER);

        queuePair.modify(configurator -> {
            configurator.setState(QueuePair.State.RTS);
            configurator.setSendPacketNumber(0);
            configurator.setTimeout(getConfig().getTimeout());
            configurator.setRetryCount(getConfig().getRetryCount());
            configurator.setRnrRetryCount(getConfig().getRnrRetryCount());
            configurator.setMaxInitiatorAtomicReads((byte) 1);
        },  QueuePair.AttributeMask.STATE,
            QueuePair.AttributeMask.TIMEOUT,
            QueuePair.AttributeMask.RETRY_CNT,
            QueuePair.AttributeMask.RNR_RETRY,
            QueuePair.AttributeMask.SQ_PSN,
            QueuePair.AttributeMask.MAX_QP_RD_ATOMIC);
    }
}
