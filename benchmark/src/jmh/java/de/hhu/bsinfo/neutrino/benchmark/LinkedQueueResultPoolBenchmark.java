package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.QueuePool;
import de.hhu.bsinfo.neutrino.benchmark.pool.QueuePool.QueueType;
import de.hhu.bsinfo.neutrino.struct.Result;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class LinkedQueueResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    private static final int THREADS = 1;

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private static final ThreadLocal<QueuePool<Result>> POOL = ThreadLocal.withInitial(
            () -> new QueuePool<>(QueueType.LINKED_BLOCKING, 1024, Result::new));

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
