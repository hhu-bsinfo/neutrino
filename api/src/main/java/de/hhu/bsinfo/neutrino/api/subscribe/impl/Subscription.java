package de.hhu.bsinfo.neutrino.api.subscribe.impl;

public class Subscription {

    private final Class<?> eventClass;
    private final SubscriberMethod method;


    public Subscription(Class<?> eventClass, SubscriberMethod method) {
        this.eventClass = eventClass;
        this.method = method;
    }

    public SubscriberMethod getMethod() {
        return method;
    }

    public Class<?> getEventClass() {
        return eventClass;
    }
}
