package de.hhu.bsinfo.neutrino.api.util.service;

@FunctionalInterface
public interface ServiceProvider {
    <T extends Service<?>> T get(final Class<T> module);
}
