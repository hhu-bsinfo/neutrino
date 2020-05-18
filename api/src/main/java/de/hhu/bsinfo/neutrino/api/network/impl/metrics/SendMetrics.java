package de.hhu.bsinfo.neutrino.api.network.impl.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public @Data class SendMetrics {

    private final StartStopTimer ackTime;

    private final StartStopTimer postTime;

    private final Counter processedRequests;

    private final Counter postedRequests;

    public SendMetrics(MeterRegistry meterRegistry, int workerIndex) {
        ackTime = new StartStopTimer(Timer.builder("network.send.completion.ack")
                .description("The time it takes to acknowledge a completion event")
                .tags("worker", String.valueOf(workerIndex))
                .register(meterRegistry));

        postTime = new StartStopTimer(Timer.builder("network.send.request.post")
                .description("The time it takes to post work requests")
                .tags("worker", String.valueOf(workerIndex))
                .register(meterRegistry));

        processedRequests = Counter.builder("network.send.request.processed")
                .description("The number of processed send work requests")
                .baseUnit("requests")
                .tag("worker", String.valueOf(workerIndex))
                .register(meterRegistry);

        postedRequests = Counter.builder("network.send.request.posted")
                .description("The number of posted send work requests")
                .baseUnit("requests")
                .tag("worker", String.valueOf(workerIndex))
                .register(meterRegistry);
    }
}
