package de.hhu.bsinfo.neutrino.api.subscribe.impl;

import de.hhu.bsinfo.neutrino.api.subscribe.Subscriber;

import java.util.List;

public class SubscriptionManager {

    private final SubscriptionStore store = new SubscriptionStore();

    public final void register(Subscriber subscriber) {
        SubscriptionUtil.findSubscriptions(subscriber).forEach(store::register);
    }

    public final List<SubscriberMethod> getSubscribers(final Class<?> type) {
        return store.getSubscribers(type);
    }
}
