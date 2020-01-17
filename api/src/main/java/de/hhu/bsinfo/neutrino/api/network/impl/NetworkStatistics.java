package de.hhu.bsinfo.neutrino.api.network.impl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class NetworkStatistics {

    private final AtomicLong sendRequests = new AtomicLong();
    private final Gauge sendRequestsGauge;

    private final AtomicLong receiveRequests = new AtomicLong();
    private final Gauge receiveRequestsGauge;

    public NetworkStatistics(MeterRegistry meterRegistry) {
        sendRequestsGauge = Gauge.builder("network.sendRequests", sendRequests::get)
                .description("The number of send work requests on the queue pair")
                .register(meterRegistry);

        receiveRequestsGauge = Gauge.builder("network.receiveRequests", sendRequests::get)
                .description("The number of receive work requests on the shared receive queue")
                .register(meterRegistry);
    }

    public AtomicLong getSendRequests() {
        return sendRequests;
    }

    public AtomicLong getReceiveRequests() {
        return receiveRequests;
    }
}
