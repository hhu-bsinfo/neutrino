package de.hhu.bsinfo.neutrino.api.device.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "infiniband.device")
@Getter @Setter
public class InternalInfinibandDeviceConfig implements InfinibandDeviceConfig {

    /**
     * The selected device's device number.
     */
    private int deviceNumber;

    /**
     * The selected device's port number.
     */
    private byte portNumber = 1;
}
