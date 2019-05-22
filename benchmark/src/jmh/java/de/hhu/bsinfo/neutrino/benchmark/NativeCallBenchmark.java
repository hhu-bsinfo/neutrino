package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.verbs.Verbs;
import org.openjdk.jmh.annotations.Benchmark;

public class NativeCallBenchmark {

    static {
        System.loadLibrary("neutrino");
    }

    @Benchmark
    public void nativeCallBench() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.benchmarkDummyMethod(result.getHandle());

        result.releaseInstance();
    }
}
