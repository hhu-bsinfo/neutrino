package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.util.ConnectionEvent;
import de.hhu.bsinfo.neutrino.api.network.impl.util.EpollWatchList;
import de.hhu.bsinfo.neutrino.util.Epoll;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.QueuedPipe;
import org.agrona.hints.ThreadHints;

import java.util.Arrays;
import java.util.function.BiConsumer;

@Slf4j
public abstract class EpollAgent implements Agent {


    private static final int WAIT_INDEFINITELY = -1;

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

    /**
     * The maximum number of milliseconds epoll waits for new events.
     */
    private final int timeout;

    /**
     * Method reference for connection processor function.
     */
    private final BiConsumer<InternalConnection, ConnectionEvent> consumer = this::processConnection;

    protected EpollAgent(int timeout, ConnectionEvent... events) {
        watchList = new EpollWatchList<>(MAX_CONNECTIONS);
        this.events = events.clone();
        this.timeout = timeout;
    }

    protected EpollAgent(ConnectionEvent... events) {
        this(WAIT_INDEFINITELY, events);
    }

    @Override
    public int doWork() {

        // Add new connections to our watch list
        if (!connectionPipe.isEmpty()) {
            connectionPipe.drain(this::watch);
        }

        // Process events
        return watchList.forEach(timeout, consumer);
    }

    private void watch(InternalConnection connection) {
        log.debug("Registering for {} on connection #{}", Arrays.toString(events), connection.getId());
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
}
