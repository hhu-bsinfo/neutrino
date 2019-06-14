package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreServiceImpl extends CoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreService.class);

    private Context context;
    private Port port;

    @Override
    protected void onInit() {
        var options = getOptions();
        context = Context.openDevice(options.getDeviceNumber());
        if (context == null) {
            throw new InitializationException("Opening device context failed");
        }

        port = context.queryPort(options.getPortNumber());
        if (port == null) {
            throw new InitializationException("Querying device port failed");
        }
    }

    @Override
    protected void onShutdown() {
        context.close();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public Port getPort() {
        return port;
    }
}
