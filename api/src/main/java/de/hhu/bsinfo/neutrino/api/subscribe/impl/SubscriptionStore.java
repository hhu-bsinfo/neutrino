package de.hhu.bsinfo.neutrino.api.subscribe.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class SubscriptionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionStore.class);

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<Class<?>, ArrayList<SubscriberMethod>> subscribers = new HashMap<>();

    public List<SubscriberMethod> getSubscribers(Class<?> type) {
        return subscribers.get(type);
    }

    public void register(final Subscription subscription) {
        subscribers.compute(subscription.getEventClass(), (eventClass, subscriberMethods) -> {
            var list = Objects.requireNonNullElseGet(subscriberMethods, ArrayList<SubscriberMethod>::new);
            list.add(subscription.getMethod());
            return list;
        });
    }
}
