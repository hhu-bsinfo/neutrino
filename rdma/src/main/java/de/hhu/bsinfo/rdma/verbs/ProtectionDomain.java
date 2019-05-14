package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectionDomain {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectionDomain.class);

    private final long handle;

    ProtectionDomain(long handle) {
        this.handle = handle;
    }

    public boolean deallocate() {
        var result = new Result();
        Verbs.deallocateProtectionDomain(handle, result.getHandle());

        if(result.isError()) {
            LOGGER.error("Could not deallocate protection domain");
            return false;
        }

        return true;
    }
}
