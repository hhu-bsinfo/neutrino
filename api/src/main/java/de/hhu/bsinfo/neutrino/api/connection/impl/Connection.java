package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;

import java.util.Objects;
import java.util.StringJoiner;

public class Connection {

    private final QueuePair queuePair;
    private final short localId;
    private final byte portNumber;

    public Connection(QueuePair queuePair, short localId, byte portNumber) {
        this.queuePair = queuePair;
        this.localId = localId;
        this.portNumber = portNumber;
    }

    byte getPortNumber() {
        return portNumber;
    }

    QueuePair getQueuePair() {
        return queuePair;
    }

    short getLocalId() {
        return localId;
    }

    @Override
    public String toString() {
        return String.format("0x%04X", localId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Connection connection = (Connection) other;
        return queuePair.getQueuePairNumber() == connection.queuePair.getQueuePairNumber();
    }

    @Override
    public int hashCode() {
        return queuePair.getQueuePairNumber();
    }
}
