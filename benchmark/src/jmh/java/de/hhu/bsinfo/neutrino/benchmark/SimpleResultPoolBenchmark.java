package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.SimplePool;
import de.hhu.bsinfo.neutrino.struct.Result;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class SimpleResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @State(Scope.Thread)
    public static class BenchmarkState {

        private static final ThreadLocal<SimplePool<Result>> POOL = ThreadLocal.withInitial(
            () -> new SimplePool<>(Result::new));

        public SimplePool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}
