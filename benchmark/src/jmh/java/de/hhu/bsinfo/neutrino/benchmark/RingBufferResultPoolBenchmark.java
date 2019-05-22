package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.RingBufferPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class RingBufferResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private static final ThreadLocal<RingBufferPool<Result>> POOL = ThreadLocal.withInitial(
            () -> new RingBufferPool<>(1024, Result::new));

        public RingBufferPool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}
