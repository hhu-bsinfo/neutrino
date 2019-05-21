package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.benchmark.pool.QueueResultPool;
import de.hhu.bsinfo.neutrino.benchmark.pool.QueueResultPool.QueueType;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.concurrent.LinkedBlockingQueue;
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

public class LinkedQueueResultPoolBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    private static final int THREADS = 1;

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private static final ThreadLocal<ObjectPool<Result>> POOL = ThreadLocal.withInitial(
            () -> new QueueResultPool(QueueType.LINKED_BLOCKING, 1024));

        public ObjectPool<Result> getPool() {
            return POOL.get();
        }
    }

    @Benchmark
    @Threads(THREADS)
    @Warmup(iterations = 2)
    @Measurement(iterations =  10)
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void resultBench(BenchmarkState state) {
        var result = state.getPool().getInstance();
        state.getPool().returnInstance(result);
    }
}