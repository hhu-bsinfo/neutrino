package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.QueueResultPool;
import de.hhu.bsinfo.neutrino.benchmark.pool.QueueResultPool.QueueType;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class ArrayQueueResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private static final ThreadLocal<ObjectPool<Result>> POOL = ThreadLocal.withInitial(
            () -> new QueueResultPool(QueueType.ARRAY_BLOCKING, 1024));

        public ObjectPool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}
