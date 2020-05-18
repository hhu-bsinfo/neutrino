package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;

import java.io.IOException;

@FunctionalInterface
public interface BufferRegistrator {
    MemoryRegion wrap(long handle, long capacity, AccessFlag... accessFlags) throws IOException;
}
