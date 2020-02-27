package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true)
public @Value class SharedResources {
    private final InfinibandDevice device;
    private final InfinibandDeviceConfig deviceConfig;
    private final NetworkConfiguration networkConfig;
    private final SharedReceiveQueue sharedReceiveQueue;
    private final BufferPool bufferPool;
}
