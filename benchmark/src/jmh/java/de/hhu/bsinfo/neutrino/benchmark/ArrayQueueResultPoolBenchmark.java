package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.QueuePool;
import de.hhu.bsinfo.neutrino.benchmark.pool.QueuePool.QueueType;
import de.hhu.bsinfo.neutrino.struct.Result;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class ArrayQueueResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        static final ThreadLocal<QueuePool<Result>> POOL = ThreadLocal.withInitial(
            () -> new QueuePool<>(QueueType.ARRAY_BLOCKING, 1024, Result::new));

        public QueuePool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}
