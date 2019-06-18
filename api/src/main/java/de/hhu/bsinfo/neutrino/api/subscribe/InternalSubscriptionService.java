package de.hhu.bsinfo.neutrino.api.subscribe;

public interface InternalSubscriptionService extends SubscriptionService {

    void publish(Object event);
}
