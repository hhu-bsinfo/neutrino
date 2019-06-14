package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.core.CoreService;

import javax.inject.Inject;

public class ConnectionServiceImpl extends ConnectionService {

    @Inject
    private CoreService core;

    @Override
    protected void onInit() {

    }

    @Override
    protected void onShutdown() {

    }
}
