package de.hhu.bsinfo.neutrino.api.memory.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;

import javax.inject.Inject;

public class MemoryServiceImpl extends Service<NullConfig> implements MemoryService {

    @Inject
    private InternalCoreService core;

    private static final AccessFlag[] DEFAULT_ACCESS_FLAGS = {
            AccessFlag.LOCAL_WRITE,
            AccessFlag.REMOTE_READ,
            AccessFlag.REMOTE_WRITE,
            AccessFlag.MW_BIND
    };

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public RegisteredBuffer register(long capacity) {
        return core.allocateMemory(capacity, DEFAULT_ACCESS_FLAGS);
    }
}
