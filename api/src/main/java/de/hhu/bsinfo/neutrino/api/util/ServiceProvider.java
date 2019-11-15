package de.hhu.bsinfo.neutrino.api.util;

@FunctionalInterface
public interface ServiceProvider {
    <T> T getService(final Class<T> service);
}
