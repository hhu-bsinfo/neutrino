package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedQueueResultPool implements ObjectPool<Result> {

    private final Queue<Result> queue;

    public LinkedQueueResultPool(int size) {
        queue = new LinkedBlockingQueue<>(size);

        for(int i = 0; i < size; i++) {
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
