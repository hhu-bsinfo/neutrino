package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.NativeLibrary;
import de.hhu.bsinfo.neutrino.verbs.Verbs;
import org.openjdk.jmh.annotations.Benchmark;

public class NativeCallBenchmark {

    static {
        NativeLibrary.load("neutrino");
    }

    @Benchmark
    public long nativeCallResultBench() {
        var result = Result.localInstance();

        Verbs.benchmarkDummyMethod1(result.getHandle());

        return result.longValue();
    }

    @Benchmark
    public long nativeCallReturnBench() {
        return Verbs.benchmarkDummyMethod2();
    }
}
