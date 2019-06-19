package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;

public interface InternalConnectionService extends ConnectionService {

    CompletionQueue getCompletionQueue();

    SharedReceiveQueue getSharedReceiveQueue();

    QueuePair getQueuePair(Connection connection);

    RegisteredBuffer getSendBuffer(Connection connection);

    RegisteredBuffer getReceiveBuffer(Connection connection);
}
