package de.hhu.bsinfo.neutrino.api.network.impl.subscriber;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.Operation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.SendOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Drainable;
import io.netty.buffer.ByteBuf;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

public class BufferSubscriber extends DrainableSubscriber<ByteBuf, Operation> {

    private enum Status {
        NONE, SUBSCRIBE, CANCEL, ERROR, COMPLETE
    }

    /**
     * This publisher's current status.
     */
    private volatile Status status = Status.NONE;
    private static final AtomicReferenceFieldUpdater<BufferSubscriber, Status> STATUS =
            AtomicReferenceFieldUpdater.newUpdater(BufferSubscriber.class, Status.class, "status");

    /**
     * Emits a sinal on disposal.
     */
    private final MonoProcessor<Void> onDispose = MonoProcessor.create();

    /**
     * The buffer pool used by this publisher.
     */
    private final BufferPool bufferPool;

    /**
     * Buffered operations.
     */
    private final ManyToOneConcurrentArrayQueue<Operation> operations = new ManyToOneConcurrentArrayQueue<>(100);

    public BufferSubscriber(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public Mono<Void> onDispose() {
        return onDispose;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        subscription.request(operations.capacity());
    }

    @Override
    protected void hookOnNext(ByteBuf buffer) {
        var target = bufferPool.leaseNext();

        // Remember number of bytes to send
        var messageSize = buffer.readableBytes();

        // Copy bytes into send buffer
        target.writeBytes(buffer);
        buffer.release();

        var operation = new SendOperation(target);

        if (!operations.offer(operation)) {
            throw Exceptions.failWithOverflow();
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        onDispose.onError(throwable);
    }

    @Override
    protected void hookOnComplete() {
        STATUS.set(this, Status.COMPLETE);
    }

    public void onRemove() {
        onDispose.onComplete();
    }

    @Override
    public boolean hasCompleted() {
        return status == Status.COMPLETE && operations.isEmpty();
    }

    @Override
    public int drain(Consumer<Operation> consumer) {

        return operations.drain(consumer);
    }

    @Override
    public int drain(Consumer<Operation> consumer, int limit) {
        return operations.drain(consumer, limit);
    }
}
