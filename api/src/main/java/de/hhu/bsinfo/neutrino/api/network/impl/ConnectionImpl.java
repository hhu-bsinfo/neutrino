package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import lombok.Builder;
import lombok.Data;

@Builder
public @Data class ConnectionImpl implements Connection {
    private final short localId;
    private final byte portNumber;
    private final QueuePair queuePair;
    private final CompletionQueue sendCompletionQueue;
    private final CompletionQueue receiveCompletionQueue;
}
