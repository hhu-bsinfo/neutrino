package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.util.ConnectionEvent;
import de.hhu.bsinfo.neutrino.api.network.impl.util.EpollWatchList;
import de.hhu.bsinfo.neutrino.util.Epoll;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.QueuedPipe;
import org.agrona.hints.ThreadHints;

public abstract class EpollAgent implements Agent {


    private static final int EPOLL_TIMEOUT = 500;

    private static final int MAX_CONNECTIONS = 1024;

    /**
     * Incoming connections which should be watched by this agent.
     */
    private final QueuedPipe<InternalConnection> connectionPipe = new ManyToOneConcurrentArrayQueue<>(MAX_CONNECTIONS);

    /**
     * Watches over connections associated with this agent.
     */
    private final EpollWatchList<InternalConnection, ConnectionEvent> watchList;

    /**
     * Event types this agent should watch over.
     */
    private final ConnectionEvent[] events;

    protected EpollAgent(ConnectionEvent... events) {
        watchList = new EpollWatchList<>(MAX_CONNECTIONS);
        this.events = events.clone();
    }

    @Override
    public int doWork() {

        // Add new connections to our watch list
        connectionPipe.drain(this::watch);

        // Process events
        return watchList.forEach(EPOLL_TIMEOUT, this::processConnection);
    }

    private void watch(InternalConnection connection) {
        for (var event : events) {
            switch (event) {
                case SEND_READY:
                    watchList.add(connection.getResources().getSendFileDescriptor(), connection, event, Epoll.EventType.EPOLLIN);
                    break;
                case RECEIVE_READY:
                    watchList.add(connection.getResources().getReceiveFileDescriptor(), connection, event, Epoll.EventType.EPOLLIN);
                    break;
                case QUEUE_READY:
                    watchList.add(connection.getQueueFileDescriptor(), connection, event, Epoll.EventType.EPOLLIN);
                    break;
            }
        }
    }

    /**
     * Adds the connection to this agent's watch list.
     */
    public final void add(InternalConnection connection) {

        // Add connection so it will be picked up and added on the next work cycle
        while (!connectionPipe.offer(connection)) {
            ThreadHints.onSpinWait();
        }

        // Wake up watch list
        watchList.wake();
    }

    /**
     * Called every time a connection becomes ready (readable/writeable).
     */
    protected abstract void processConnection(InternalConnection connection, ConnectionEvent event);

    protected abstract void onConnection(InternalConnection connection);
}
