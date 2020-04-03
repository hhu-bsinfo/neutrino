package de.hhu.bsinfo.neutrino.api.network.impl.subscriber;

import de.hhu.bsinfo.neutrino.api.network.operation.Operation;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Drainable;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

@SuppressWarnings("MethodDoesntCallSuperMethod")
public final class OperationSubscriber extends BaseSubscriber<Operation> implements Drainable<Operation> {

    private enum Status {
        NONE, SUBSCRIBE, CANCEL, ERROR, COMPLETE
    }

    private static final AtomicInteger SUBSCRIBER_ID = new AtomicInteger();

    /**
     * This subscriber's current status.
     */
    private volatile Status status = Status.NONE;
    private static final AtomicReferenceFieldUpdater<OperationSubscriber, Status> STATUS =
            AtomicReferenceFieldUpdater.newUpdater(OperationSubscriber.class, Status.class, "status");

    /**
     * Emits a signal to the actual subscriber.
     */
    private final MonoProcessor<Void> signal = MonoProcessor.create();

    /**
     * Buffered operations.
     */
    private final ManyToOneConcurrentArrayQueue<Operation> operations = new ManyToOneConcurrentArrayQueue<>(128);

    /**
     * The number of pending requests this subscriber produced.
     */
    private int pending;

    /**
     * This subscriber's unique identifier
     */
    private final int id = SUBSCRIBER_ID.getAndIncrement();

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        subscription.request(operations.capacity());
    }

    @Override
    protected void hookOnNext(Operation operation) {
        if (!operations.offer(operation)) {
            throw Exceptions.failWithOverflow();
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        signal.onError(throwable);
    }

    @Override
    protected void hookOnComplete() {
        STATUS.set(this, Status.COMPLETE);
    }

    @Override
    public int drain(Consumer<Operation> consumer) {
        return operations.drain(consumer);
    }

    @Override
    public int drain(Consumer<Operation> consumer, int limit) {
        return operations.drain(consumer, limit);
    }

    public int getId() {
        return id;
    }

    public void incrementPending(int value) {
        pending += value;
    }

    public void decrementPending(int value) {
        pending -= value;
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    public boolean hasPending() {
        return pending > 0;
    }

    public boolean hasCompleted() {
        return status == Status.COMPLETE && isEmpty();
    }

    public void signalCompletion() {
        signal.onComplete();
    }

    public void signalError(Throwable throwable) {
        signal.onError(throwable);
    }

    public Mono<Void> then() {
        return signal;
    }
}
