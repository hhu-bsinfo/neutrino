package de.hhu.bsinfo.neutrino.api.util;

import io.netty.util.ReferenceCounted;

public interface Transferable extends ReferenceCounted {

    /**
     * The virtual memory address.
     */
    long memoryAddress();

    /**
     * The capacity in bytes.
     */
    int capacity();

    /**
     * The local key used for RDMA operations.
     */
    int localKey();

    /**
     * The remote key used for RDMA operations.
     */
    int remoteKey();
}
