package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.DeviceAttributes;
import de.hhu.bsinfo.neutrino.verbs.PortAttributes;

@Expose
public interface CoreService {

    PortAttributes getPortAttributes();

    DeviceAttributes getDeviceAttributes();

    RegisteredBuffer registerMemory(long capacity);
}
