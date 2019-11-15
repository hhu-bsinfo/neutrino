package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.DeviceAttributes;
import de.hhu.bsinfo.neutrino.verbs.PortAttributes;
import io.netty.buffer.ByteBuf;

@Expose
public interface CoreService {

    /**
     * The {@link PortAttributes} belonging to the current infiniband device.
     */
    PortAttributes getPortAttributes();

    /**
     * The {@link DeviceAttributes} belonging to the current infiniband device.
     */
    DeviceAttributes getDeviceAttributes();

    /**
     * Creates a new {@link RegisteredBuffer} used for RDMA operations.
     */
    RegisteredBuffer registerMemory(long capacity);

    /**
     * Registers the provided {@link ByteBuf} for usage within the verbs API.
     */
    RegisteredByteBuf registerBuffer(ByteBuf buffer);

    /**
     * The current infiniband device's local id.
     */
    short getLocalId();
}
