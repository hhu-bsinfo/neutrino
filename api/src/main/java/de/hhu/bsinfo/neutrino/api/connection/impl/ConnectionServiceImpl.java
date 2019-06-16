package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;

import javax.inject.Inject;

public class ConnectionServiceImpl extends Service<NullConfig> implements ConnectionService {

    @Inject
    private CoreService core;

    @Override
    protected void onInit(NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }
}
