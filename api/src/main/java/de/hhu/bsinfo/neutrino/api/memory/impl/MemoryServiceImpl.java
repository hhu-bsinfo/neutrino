package de.hhu.bsinfo.neutrino.api.memory.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;

import javax.inject.Inject;

public class MemoryServiceImpl extends Service<NullConfig> implements MemoryService {

    @Inject
    private CoreService core;

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }
}
