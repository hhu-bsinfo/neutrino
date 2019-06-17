package de.hhu.bsinfo.neutrino.api.connection.impl.manager;

import de.hhu.bsinfo.neutrino.verbs.QueuePair;

@FunctionalInterface
public interface QueuePairConnector {
    void connect(QueuePair queuePair, RemoteQueuePair remoteQueuePair);
}
