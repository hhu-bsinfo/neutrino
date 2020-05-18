package de.hhu.bsinfo.neutrino.api.network.impl.metrics;

import io.micrometer.core.instrument.Timer;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.TimeUnit;

@NotThreadSafe
public final class StartStopTimer  {

    private final Timer delegate;

    private long startTime;

    public StartStopTimer(Timer timer) {
        delegate = timer;
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        delegate.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }
}
