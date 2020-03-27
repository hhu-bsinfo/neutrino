package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairState;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.ArrayUtil;
import org.agrona.collections.Int2ObjectHashMap;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@Slf4j
@Builder
public @Data class InternalConnection {

    private static final AtomicLongFieldUpdater<InternalConnection> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(InternalConnection.class, "requested");

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<InternalConnection, CoreSubscriber> SUBSCRIBER =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, CoreSubscriber.class, "inboundSubscriber");

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
     * Map used to retrieve Subscribers based on their identifiers.
     */
    private final Int2ObjectHashMap<OperationSubscriber> outboundSubscribers = new Int2ObjectHashMap<>();

    /**
     * The subscriber.
     */
    private volatile CoreSubscriber<ByteBuf> inboundSubscriber;

    /**
     * Flux emitting received buffers.
     */
    private final FluxReceive inbound = new FluxReceive();

    /**
     * The number of requested messages.
     */
    private volatile long requested;

    /**
     * Enabled if the subscriber requested Long.MAX_VALUE elements.
     */
    private volatile boolean isUnbounded;

    private volatile SendAgent sendAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, SendAgent> SEND_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, SendAgent.class, "sendAgent");

    private volatile ReceiveAgent receiveAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, ReceiveAgent> RECEIVE_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, ReceiveAgent.class, "receiveAgent");

    @Builder.Default
    private volatile OperationSubscriber[] subscribers = new OperationSubscriber[0];
    private static final AtomicReferenceFieldUpdater<InternalConnection, OperationSubscriber[]> SUBSCRIBERS =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, OperationSubscriber[].class, "subscribers");

    public void setSendAgent(SendAgent agent) {
        SEND_AGENT.set(this, agent);
    }

    public void setReceiveAgent(ReceiveAgent agent) {
        RECEIVE_AGENT.set(this, agent);
    }

    public void addSubscriber(OperationSubscriber subscriber) {
        OperationSubscriber[] oldArray;
        OperationSubscriber[] newArray;

        do {
            oldArray = subscribers;
            newArray = ArrayUtil.add(oldArray, subscriber);
        } while (!SUBSCRIBERS.compareAndSet(this, oldArray, newArray));

        outboundSubscribers.put(subscriber.getId(), subscriber);
    }

    public void onCompletion(OperationSubscriber subscriber) {
        OperationSubscriber[] oldArray;
        OperationSubscriber[] newArray;

        do {
            oldArray = subscribers;
            newArray = ArrayUtil.remove(oldArray, subscriber);
        } while (!SUBSCRIBERS.compareAndSet(this, oldArray, newArray));
    }

    public void remove(OperationSubscriber subscriber) {
        outboundSubscribers.remove(subscriber.getId(), subscriber);
    }

    public int getFreeSlots() {
        return (int) queueFileDescriptor.read();
    }

    public void incrementFreeSlots(int value) {
        queueFileDescriptor.increment(value);
    }

    public FluxReceive getInbound() {
        return inbound;
    }

    public Subscriber<ByteBuf> getInboundSubscriber() {
        return inboundSubscriber;
    }

    public OperationSubscriber getOutboundSubscriber(long identifier) {
        return outboundSubscribers.get(Identifier.getContext(identifier));
    }

    private class FluxReceive extends Flux<ByteBuf> implements Subscription {

        @Override
        public void request(long n) {
            if (isUnbounded) {
                return;
            }

            if (n == Long.MAX_VALUE) {
                isUnbounded = true;
                requested = Long.MAX_VALUE;
                return;
            }

            Operators.addCap(REQUESTED, InternalConnection.this, n);
        }


        @Override
        public void cancel() {
            if (inboundSubscriber != null) {
                inboundSubscriber.onComplete();
            }

            log.debug("Subscription has been cancelled");
        }

        @Override
        public void subscribe(CoreSubscriber<? super ByteBuf> subscriber) {
            boolean result = SUBSCRIBER.compareAndSet(InternalConnection.this, null, subscriber);
            if (result) {
                subscriber.onSubscribe(this);
            } else {
                Operators.error(subscriber, Exceptions.duplicateOnSubscribeException());
            }
        }
    }
}
