package de.hhu.bsinfo.neutrino.api.util.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Service<T extends ServiceConfig> {

    private static final String SERVICE_SUFFIX = "Service";

    protected final Logger log = LoggerFactory.getLogger(ServiceManager.findServiceInterface(getClass()));

    private T config;

    protected abstract void onInit(final T config);

    protected abstract void onShutdown();

    @SuppressWarnings("unchecked")
    void setConfig(ServiceConfig config) {
        this.config = (T) config;
    }

    protected T getConfig() {
        return config;
    }
}
