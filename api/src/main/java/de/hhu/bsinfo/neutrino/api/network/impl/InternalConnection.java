package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.FluxReceive;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.api.network.impl.util.OperationStore;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairState;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@Slf4j
@Builder
public @Data class InternalConnection {

    /**
     * This connection's unique identifier.
     */
    private final int id;

    /**
     * The queue pair's local id.
     */
    private final short localId;

    /**
     * The queue pair's port number.
     */
    private final byte portNumber;

    /**
     * The queue pair used by this connection.
     */
    private final QueuePair queuePair;

    /**
     * The associated queue pair's resources.
     */
    private final QueuePairResources resources;

    /**
     * The associated queue pair's state.
     */
    private final QueuePairState state;

    /**
     * Used for signaling free slots within the send queue.
     */
    private final EventFileDescriptor queueFileDescriptor;

    /**
     * Helper object containing all active operation subscribers assigned to this connection.
     */
    private final OperationStore operationStore = new OperationStore();

    /**
     * Flux emitting received buffers.
     */
    private final FluxReceive inbound = new FluxReceive();

    /**
     * The send agent assigned to this connection.
     */
    private volatile SendAgent sendAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, SendAgent> SEND_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, SendAgent.class, "sendAgent");

    /**
     * The receive agent assigned to this connection.
     */
    private volatile ReceiveAgent receiveAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, ReceiveAgent> RECEIVE_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, ReceiveAgent.class, "receiveAgent");


    /**
     * Assigns a send agent to this connection.
     */
    public void setSendAgent(SendAgent agent) {
        if (!SEND_AGENT.compareAndSet(this, null, agent)) {
            throw new IllegalStateException("Send agent already set");
        }
    }

    /**
     * Assigns a receive agent to this connection.
     */
    public void setReceiveAgent(ReceiveAgent agent) {
        if (!RECEIVE_AGENT.compareAndSet(this, null, agent)) {
            throw new IllegalStateException("Receive agent already set");
        }
    }

    /**
     * Adds a new operation subscriber to this connection.
     */
    public void addSubscriber(OperationSubscriber subscriber) {
        operationStore.addSubscriber(subscriber);
    }

    /**
     * Called after all operations have been commited to this connection's queue pair.
     */
    public void onCompletion(OperationSubscriber subscriber) {
        operationStore.onCompletion(subscriber);
    }

    /**
     * Removes the specified operation subscriber from this connection.
     */
    public void remove(OperationSubscriber subscriber) {
        operationStore.remove(subscriber);
    }

    public OperationSubscriber[] getSubscribers() {
        return operationStore.getSubscribers();
    }

    /**
     * Looks up an operation subscriber by its identifier.
     */
    public OperationSubscriber getOutboundSubscriber(long identifier) {
        return operationStore.get(Identifier.getContext(identifier));
    }

    /**
     * Returns the number of free slots within this connection's queue pair.
     */
    public int getFreeSlots() {
        return (int) queueFileDescriptor.read();
    }

    /**
     * Indicate that the specified number of requests have been processed.
     */
    public void onProcessed(int count) {
        state.decrementPending(count);
        queueFileDescriptor.increment(count);
    }

    /**
     * Returns a Flux emitting inbound buffers.
     */
    public FluxReceive getInbound() {
        return inbound;
    }

    /**
     * Returns the subscriber listening for incoming data on this connection.
     */
    public Subscriber<ByteBuf> getInboundSubscriber() {
        return inbound.getSubscriber();
    }
}
