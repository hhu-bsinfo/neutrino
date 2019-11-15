package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.connection.impl.ConnectionImpl;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;

public interface InternalConnectionService extends ConnectionService {

    /**
     * The {@link SharedReceiveQueue} used for all connections.
     */
    SharedReceiveQueue getSharedReceiveQueue();
}
