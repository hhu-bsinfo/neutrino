package de.hhu.bsinfo.neutrino.benchmark;

import de.hhu.bsinfo.neutrino.bench.JniCall;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.util.MemoryUtil;
import de.hhu.bsinfo.neutrino.util.NativeLibrary;
import org.agrona.concurrent.AtomicBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class JniCallBenchmark {

    static { NativeLibrary.load("neutrino"); }

    @State(Scope.Thread)
    public static class Objects {

        @Param({"64", "512", "1024", "4096", "16384", "32768"})
        public int bufferSize;

        public byte[] byteArray;
        public ByteBuffer directByteBuffer;
        public AtomicBuffer atomicBuffer;


        @Setup(Level.Trial)
        public void doSetup() {
            byteArray = new byte[bufferSize];
            ThreadLocalRandom.current().nextBytes(byteArray);

            directByteBuffer = ByteBuffer.allocateDirect(bufferSize);
            directByteBuffer.put(byteArray, 0, byteArray.length);

            atomicBuffer = MemoryUtil.allocateAligned(bufferSize, MemoryAlignment.CACHE);
            atomicBuffer.setMemory(0, atomicBuffer.capacity(), (byte) 0);
        }
    }

    @Benchmark
    public byte checkSumDirectByteBuffer(Objects objects) {
        return JniCall.checkSumDirectByteBuffer(objects.directByteBuffer);
    }

    @Benchmark
    public byte checkSumLocalBuffer(Objects objects) {
        return JniCall.checkSumLocalBuffer(objects.atomicBuffer);
    }

    @Benchmark
    public byte checkSumByteArrayGet(Objects objects) {
        return JniCall.checkSumByteArrayGet(objects.byteArray);
    }

    @Benchmark
    public byte checkSumByteArrayGetCritical(Objects objects) {
        return JniCall.checkSumByteArrayGetCritical(objects.byteArray);
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = {"-XX:+CriticalJNINatives", "-Xcomp"})
    public byte checkSumByteArrayJavaCritical(Objects objects) {
        return JniCall.checkSumByteArrayJavaCritical(objects.byteArray);
    }
}
