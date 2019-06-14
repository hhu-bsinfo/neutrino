package de.hhu.bsinfo.neutrino.api.memory.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;

import javax.inject.Inject;

public class MemoryServiceImpl extends MemoryService {

    @Inject
    private CoreService core;

    @Override
    protected void onInit() {

    }

    @Override
    protected void onShutdown() {

    }
}
