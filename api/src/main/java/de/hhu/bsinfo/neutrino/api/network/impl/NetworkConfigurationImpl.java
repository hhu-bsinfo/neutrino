package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.verbs.DeviceAttributes;
import de.hhu.bsinfo.neutrino.verbs.PortAttributes;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "infiniband.network")
@Getter @Setter
public class NetworkConfigurationImpl implements NetworkConfiguration {
    private int mtu = 4096;
    private int sharedReceiveQueueSize = 16383;
    private int maxScatterGatherElements = 10;
    private int completionQueueSize = 4194303;
    private int queuePairSize = 16351;

    private byte rnrTimer;
    private byte rnrRetryCount = 7;

    private byte serviceLevel;
    private byte timeout = 14;
    private byte retryCount = 7;

    private final DeviceAttributes deviceAttributes;
    private final PortAttributes portAttributes;

    public NetworkConfigurationImpl(InfinibandDevice device) {
        deviceAttributes = device.getDeviceAttributes();
        portAttributes = device.getPortAttributes();
    }

    @PostConstruct
    private void postConstruct() {
        if (sharedReceiveQueueSize > deviceAttributes.getMaxSharedReceiveQueueSize()) {
            sharedReceiveQueueSize = deviceAttributes.getMaxSharedReceiveQueueSize();
            log.warn("Set shared receive queue size to maximum value of {}", deviceAttributes.getMaxSharedReceiveQueueSize());
        }

        if (maxScatterGatherElements > deviceAttributes.getMaxScatterGatherCount()) {
            maxScatterGatherElements = deviceAttributes.getMaxScatterGatherCount();
            log.warn("Set scatter gather element count to maximum value of {}", deviceAttributes.getMaxScatterGatherCount());
        }

        if (completionQueueSize > deviceAttributes.getMaxCompletionQueueSize()) {
            completionQueueSize = deviceAttributes.getMaxCompletionQueueSize();
            log.warn("Set completion queue size to maximum value of {}", deviceAttributes.getMaxCompletionQueueSize());
        }

        if (queuePairSize > deviceAttributes.getMaxQueuePairSize()) {
            queuePairSize = deviceAttributes.getMaxQueuePairSize();
            log.warn("Set queue pair size to maximum value of {}", deviceAttributes.getMaxQueuePairSize());
        }
    }
}
