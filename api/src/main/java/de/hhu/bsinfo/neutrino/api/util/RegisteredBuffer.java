package de.hhu.bsinfo.neutrino.api.util;

import org.agrona.concurrent.AtomicBuffer;

public interface RegisteredBuffer extends AtomicBuffer {

    int localKey();

    int remoteKey();

    void release() throws Exception;
}
