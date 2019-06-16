package de.hhu.bsinfo.neutrino.api.util.service;

@FunctionalInterface
public interface ServiceProvider {
    <T> T get(final Class<T> service);
}
