package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.util.EpollEvent;
import de.hhu.bsinfo.neutrino.api.network.impl.util.EpollWatchList;
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
    private final EpollWatchList watchList;

    /**
     * Event types this agent should watch over.
     */
    private final EpollEvent[] events;

    protected EpollAgent(EpollEvent... events) {
        watchList = new EpollWatchList(MAX_CONNECTIONS);
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
        watchList.add(connection, events);
    }

    /**
     * Adds the connection to this agent's watch list.
     */
    public void add(InternalConnection connection) {

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
    protected abstract void processConnection(InternalConnection connection, EpollEvent event);
}
