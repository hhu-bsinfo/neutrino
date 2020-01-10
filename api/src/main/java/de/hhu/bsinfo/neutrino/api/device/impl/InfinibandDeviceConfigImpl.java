package de.hhu.bsinfo.neutrino.api.device.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "infiniband.device")
@Getter @Setter
public class InfinibandDeviceConfigImpl implements InfinibandDeviceConfig {
    private int deviceNumber;
    private byte portNumber = 1;
}
