package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.NativeObjectStore;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueStore<T extends NativeObject> extends NativeObjectStore<T> {

    public enum QueueType {
        LINKED_BLOCKING,
        ARRAY_BLOCKING
    }

    private final Queue<T> queue;

    public QueueStore(final QueueType type, final int size) {
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
    public final void storeInstance(T instance) {
        queue.offer(instance);
    }

    @Override
    public T getInstance() {
        return queue.poll();
    }
}
