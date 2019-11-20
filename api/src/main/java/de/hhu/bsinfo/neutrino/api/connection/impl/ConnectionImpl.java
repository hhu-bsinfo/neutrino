package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
public @Data class ConnectionImpl implements Connection {
    private final short localId;
    private final byte portNumber;
    private final QueuePair queuePair;
    private final CompletionQueue sendCompletionQueue;
    private final CompletionQueue receiveCompletionQueue;
    private final RegisteredByteBuf sendBuffer;
    private final RegisteredByteBuf receiveBuffer;
}
