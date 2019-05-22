package de.hhu.bsinfo.neutrino.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import picocli.CommandLine;

public class BenchmarkApp {

    @CommandLine.Option(
        names = { "-t", "--threads" },
        description = "Amount of threads to run the benchmarks with",
        paramLabel = "<THREADS>")
    private int threadCount = 1;

    @CommandLine.Option(
        names = { "-w", "--warmup" },
        description = "Amount of warmup iterations to perform before a benchmark",
        paramLabel = "<WARMUP_ITERATIONS>")
    private int warmupIterations = 2;

    @CommandLine.Option(
        names = { "-i", "--iterations" },
        description = "Amount of iterations to perform per benchmark",
        paramLabel = "<BENCHMARK_ITERATIONS>")
    private int benchmarkIterations = 10;

    @CommandLine.Option(
        names = { "-b", "--benchmarks" },
        description = "List of benchmarks to perform",
        paramLabel = "<BENCHMARK_LIST>")
    private String[] benchmarks = {};


    public static void main(String... args) throws RunnerException {
        BenchmarkApp app = new BenchmarkApp();

        new CommandLine(app).parse(args);

        ChainedOptionsBuilder options = new OptionsBuilder()
            .mode(Mode.SampleTime)
            .timeUnit(TimeUnit.MICROSECONDS)
            .threads(app.threadCount)
            .warmupIterations(app.warmupIterations)
            .measurementIterations(app.benchmarkIterations);

        for(String benchmark : app.benchmarks) {
            options.include(benchmark);
        }

        new Runner(options.build()).run();
    }
}
