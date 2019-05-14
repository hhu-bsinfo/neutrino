package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectionDomain {

    private static final Logger LOGGER = LoggerFactory.getLogger(Context.class);

    private final long handle;

    ProtectionDomain(long handle) {
        this.handle = handle;
    }

    public boolean deallocate() {
        var result = new Result();
        Verbs.deallocateProtectionDomain(handle, result.getHandle());

        return !result.isError();
    }
}
