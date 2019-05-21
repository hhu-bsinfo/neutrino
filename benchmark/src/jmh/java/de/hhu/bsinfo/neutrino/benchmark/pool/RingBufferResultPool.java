package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.Objects;

public class RingBufferResultPool implements ObjectPool<Result> {

    private final RingBuffer<Result> buffer;

    public RingBufferResultPool(int size) {
        buffer = new RingBuffer<>(size, Result.class);
    }

    @Override
    public Result getInstance() {
        return Objects.requireNonNullElseGet(buffer.pop(), Result::new);
    }

    @Override
    public void returnInstance(Result result) {
        buffer.push(result);
    }
}
