package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;

@FunctionalInterface
public interface Negotiator {

    /**
     * Exchanges the local {@link QueuePairAddress} with the remote.
     */
    QueuePairAddress exchange(QueuePairAddress localInfo);
}
