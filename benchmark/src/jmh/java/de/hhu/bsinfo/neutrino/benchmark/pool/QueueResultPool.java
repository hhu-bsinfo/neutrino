package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueResultPool implements ObjectPool<Result> {

    public enum QueueType {
        LINKED_BLOCKING,
        ARRAY_BLOCKING
    }

    private final Queue<Result> queue;

    public QueueResultPool(final QueueType type, final int size) {
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

        for(int i = 0; i < queue.size(); i++) {
            queue.offer(new Result());
        }
    }

    @Override
    public Result getInstance() {
        return Objects.requireNonNullElseGet(queue.poll(), Result::new);
    }

    @Override
    public void returnInstance(Result result) {
        queue.offer(result);
    }
}
