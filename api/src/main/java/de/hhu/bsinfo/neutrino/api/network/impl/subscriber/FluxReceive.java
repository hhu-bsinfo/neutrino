package de.hhu.bsinfo.neutrino.api.network.impl.subscriber;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@Slf4j
@SuppressWarnings("rawtypes")
public class FluxReceive extends Flux<ByteBuf> implements Subscription {

    /**
     * The subscriber.
     */
    private volatile CoreSubscriber<ByteBuf> inboundSubscriber;
    private static final AtomicReferenceFieldUpdater<FluxReceive, CoreSubscriber> SUBSCRIBER =
            AtomicReferenceFieldUpdater.newUpdater(FluxReceive.class, CoreSubscriber.class, "inboundSubscriber");

    /**
     * The number of requested messages.
     */
    private volatile long requested;
    private static final AtomicLongFieldUpdater<FluxReceive> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(FluxReceive.class, "requested");

    /**
     * Enabled if the subscriber requested Long.MAX_VALUE elements.
     */
    private volatile boolean isUnbounded;

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

        Operators.addCap(REQUESTED, this, n);
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
        boolean result = SUBSCRIBER.compareAndSet(this, null, subscriber);
        if (result) {
            subscriber.onSubscribe(this);
        } else {
            Operators.error(subscriber, Exceptions.duplicateOnSubscribeException());
        }
    }

    public CoreSubscriber<ByteBuf> getSubscriber() {
        return inboundSubscriber;
    }
}
