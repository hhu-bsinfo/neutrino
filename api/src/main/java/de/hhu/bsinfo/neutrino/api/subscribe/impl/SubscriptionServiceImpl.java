package de.hhu.bsinfo.neutrino.api.subscribe.impl;

import de.hhu.bsinfo.neutrino.api.subscribe.InternalSubscriptionService;
import de.hhu.bsinfo.neutrino.api.subscribe.Subscriber;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;

public class SubscriptionServiceImpl extends Service<NullConfig> implements InternalSubscriptionService {

    private final SubscriptionStore store = new SubscriptionStore();

    @Override
    protected void onInit(NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public void register(Subscriber subscriber) {
        SubscriptionUtil.findSubscriptions(subscriber).forEach(store::register);
    }

    @Override
    public void publish(Object event) {
        store.getSubscribers(event.getClass()).forEach(method -> method.handle(event));
    }
}
