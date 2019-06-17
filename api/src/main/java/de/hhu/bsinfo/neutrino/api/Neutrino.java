package de.hhu.bsinfo.neutrino.api;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.api.util.InternalAccessException;
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

    public <T> T getService(final Class<T> service) {
        if (!service.isAnnotationPresent(Expose.class)) {
            throw new InternalAccessException("{} is not exposed", service.getName());
        }

        return serviceManager.get(service);
    }
}
