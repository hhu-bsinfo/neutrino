package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.util.ResultFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

public class FactoryBenchmark {

    @State(Scope.Benchmark)
    private static class BenchmarkState {
        ResultFactory factory = new ResultFactory();
    }

    @Benchmark
    @Warmup(iterations = 1000)
    @Measurement(iterations = 1000000)
    @BenchmarkMode(Mode.Throughput)
    public void resultBench(BenchmarkState state) {
        state.factory.newInstance();
    }
}
