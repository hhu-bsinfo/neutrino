package de.hhu.bsinfo.neutrino.api.network.impl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public final class NetworkMetrics {

    private final AtomicLong sendRequests = new AtomicLong();
    private final Gauge sendRequestsGauge;

    private final AtomicLong receiveRequests = new AtomicLong();
    private final Gauge receiveRequestsGauge;

    public NetworkMetrics(MeterRegistry meterRegistry) {
        sendRequestsGauge = Gauge.builder("network.sendRequests", sendRequests::get)
                .description("The number of send work requests on the queue pair")
                .register(meterRegistry);

        receiveRequestsGauge = Gauge.builder("network.receiveRequests", sendRequests::get)
                .description("The number of receive work requests on the shared receive queue")
                .register(meterRegistry);
    }

    public long getSendRequests() {
        return sendRequests.get();
    }

    public void incrementSendRequests() {
        sendRequests.incrementAndGet();
    }

    public void incrementSendRequests(long delta) {
        sendRequests.addAndGet(delta);
    }

    public void decrementSendRequests() {
        sendRequests.decrementAndGet();
    }

    public void decrementSendRequests(long delta) {
        sendRequests.addAndGet(-delta);
    }

    public long getReceiveRequests() {
        return receiveRequests.get();
    }

    public void incrementReceiveRequests() {
        receiveRequests.incrementAndGet();
    }

    public void incrementReceiveRequests(long delta) {
        receiveRequests.addAndGet(delta);
    }

    public void decrementReceiveRequests() {
        receiveRequests.decrementAndGet();
    }

    public void decrementReceiveRequests(long delta) {
        receiveRequests.addAndGet(-delta);
    }
}
