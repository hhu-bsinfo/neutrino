package de.hhu.bsinfo.neutrino.api.network.impl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.util.concurrent.atomic.AtomicLong;

@Accessors(fluent = true)
public @Data final class NetworkMetrics {

    private final Timer claimTime;

    private final Timer sendTime;

    public NetworkMetrics(MeterRegistry meterRegistry) {
        claimTime = Timer.builder("network.send.buffer.claim")
                .description("The time it takes to claim a buffer")
                .register(meterRegistry);

        sendTime = Timer.builder("network.request.append")
                .description("The time it takes to append a request")
                .register(meterRegistry);
    }
}
