package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.verbs.AccessFlag;

public interface BufferAllocator {

    Buffer buffer(int capacity, AccessFlag... accessFlags);
}
