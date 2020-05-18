package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true)
public @Value class SharedResources {

    /**
     * The Infiniband device used by neutrino.
     */
    InfinibandDevice device;

    /**
     * The Infiniband device's configuration parameters.
     */
    InfinibandDeviceConfig deviceConfig;

    /**
     * The network service's configuration parameters.
     */
    NetworkConfiguration networkConfig;

    /**
     * Network metrics.
     */
    NetworkMetrics networkMetrics;

    /**
     * Micrometer meter registry.
     */
    MeterRegistry meterRegistry;
}
