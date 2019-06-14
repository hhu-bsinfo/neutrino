package de.hhu.bsinfo.neutrino.api;

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
        initializeServices();
    }

    private void initializeServices() {
        serviceManager.initialize();
    }

    public <T extends Service<?>> T getService(final Class<T> service) {
        return serviceManager.get(service);
    }
}
