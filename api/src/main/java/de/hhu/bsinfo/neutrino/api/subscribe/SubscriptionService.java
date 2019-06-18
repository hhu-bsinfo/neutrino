package de.hhu.bsinfo.neutrino.api.subscribe;

import de.hhu.bsinfo.neutrino.api.util.Expose;

@Expose
public interface SubscriptionService {

    void register(Subscriber subscriber);

}
