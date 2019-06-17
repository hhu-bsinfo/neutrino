package de.hhu.bsinfo.neutrino.api.util.service;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class Service<T extends ServiceConfig> {

    private static final String SERVICE_SUFFIX = "Service";

    protected final Logger log = LoggerFactory.getLogger(
            Arrays.stream(getClass().getInterfaces())
                .filter(it -> it.getName().endsWith(SERVICE_SUFFIX))
                .findFirst().orElseThrow());

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
