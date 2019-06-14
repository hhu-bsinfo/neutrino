package de.hhu.bsinfo.neutrino.api;

import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Neutrino {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neutrino.class);

    private final ServiceManager serviceManager = new ServiceManager();

    private Neutrino() {}

    public static Neutrino newInstance() {
        var neutrino = new Neutrino();
        neutrino.initialize();
        return neutrino;
    }

    private void initialize() {
        registerModules();
        initializeModules();
    }

    private void registerModules() {
        serviceManager.register(ConnectionService.class);
        serviceManager.register(MessageService.class);
    }

    private void initializeModules() {
        serviceManager.initialize();
    }

    public <T extends Service<?>> T getModule(final Class<T> module) {
        return serviceManager.get(module);
    }
}
