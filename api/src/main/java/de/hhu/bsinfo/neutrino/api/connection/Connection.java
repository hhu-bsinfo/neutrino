package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBuf;

public interface Connection {

    /**
     * The connection's local port number.
     */
    byte getPortNumber();

    /**
     * The connection's local id.
     */
    short getLocalId();

    /**
     * The {@link QueuePair} used by this connection.
     */
    QueuePair getQueuePair();

    /**
     * The {@link CompletionQueue} used by this connection.
     */
    CompletionQueue getSendCompletionQueue();

    /**
     * The {@link CompletionQueue} used by this connection.
     */
    CompletionQueue getReceiveCompletionQueue();

    /**
     * The send buffer used by this connection.
     */
    RegisteredByteBuf getSendBuffer();

    /**
     * The receive buffer used by this connection.
     */
    RegisteredByteBuf getReceiveBuffer();
}
