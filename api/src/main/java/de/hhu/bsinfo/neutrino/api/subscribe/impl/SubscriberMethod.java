package de.hhu.bsinfo.neutrino.api.subscribe.impl;

@FunctionalInterface
public interface SubscriberMethod {
    <T> void handle(final T event);
}
