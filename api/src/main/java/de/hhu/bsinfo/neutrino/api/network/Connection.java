package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBuf;

public interface Connection {

    /**
     * The connection's id.
     */
    int getId();

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
     * The {@link BufferPool} used for outgoing buffers.
     */
    BufferPool getBufferPool();
}
