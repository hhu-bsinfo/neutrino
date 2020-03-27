package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true)
public @Value class AgentResources {

    /**
     * The Infiniband device used by the associated agent.
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
     * The thread domain used by the associated agent.
     */
    ThreadDomain threadDomain;

    /**
     * The protection domain used by the associated agent.
     */
    ProtectionDomain protectionDomain;
}
