package de.hhu.bsinfo.neutrino.api.util.service;

public abstract class Service<T extends ServiceConfig> {

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
