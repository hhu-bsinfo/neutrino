package de.hhu.bsinfo.neutrino.api.memory;

import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;

public interface MemoryService {

    RegisteredBuffer register(long capacity);
}
