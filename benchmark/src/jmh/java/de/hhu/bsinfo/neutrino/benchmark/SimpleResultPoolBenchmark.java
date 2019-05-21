package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.SimpleResultPool;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

public class SimpleResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @State(Scope.Thread)
    public static class BenchmarkState {

        private static final ThreadLocal<ObjectPool<Result>> POOL = ThreadLocal.withInitial(
            SimpleResultPool::new);

        public ObjectPool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    @Threads(8)
    @Warmup(iterations = 2)
    @Measurement(iterations =  10)
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}
