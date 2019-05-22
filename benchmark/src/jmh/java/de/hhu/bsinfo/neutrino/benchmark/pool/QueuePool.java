package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.util.Pool;
import de.hhu.bsinfo.neutrino.util.Poolable;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class QueuePool<T extends Poolable> extends Pool<T> {

    public enum QueueType {
        LINKED_BLOCKING,
        ARRAY_BLOCKING
    }

    private final Queue<T> queue;

    public QueuePool(final QueueType type, final int size, final Supplier<T> supplier) {
        super(supplier);

        switch (type) {
            case ARRAY_BLOCKING:
                queue = new ArrayBlockingQueue<>(size);
                break;
            case LINKED_BLOCKING:
                queue = new LinkedBlockingQueue<>(size);
                break;
            default:
                queue = null;
        }
    }

    @Override
    public T getInstance() {
        return Objects.requireNonNullElseGet(queue.poll(), getSupplier());
    }

    @Override
    public void returnInstance(T instance) {
        queue.offer(instance);
    }
}
