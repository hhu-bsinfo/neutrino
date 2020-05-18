package de.hhu.bsinfo.neutrino.api.network.impl.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

@Accessors(fluent = true)
public @Data class ReceiveMetrics {

    private final StartStopTimer refillTime;

    private final Counter processedRequests;

    public ReceiveMetrics(MeterRegistry meterRegistry, int workerIndex) {
        refillTime = new StartStopTimer(Timer.builder("network.receive.refillTime")
                .description("The time it takes to refill the shared receive queue")
                .minimumExpectedValue(Duration.ofNanos(0))
                .maximumExpectedValue(Duration.ofMillis(1))
                .tags("worker", String.valueOf(workerIndex))
                .register(meterRegistry));

        processedRequests = Counter.builder("network.receive.requests.processed")
                .description("The number of receive work requests processed")
                .baseUnit("requests")
                .tag("worker", String.valueOf(workerIndex))
                .register(meterRegistry);
    }
}
