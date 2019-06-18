package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.verbs.QueuePair;

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
        return new StringJoiner(", ", Connection.class.getSimpleName() + "[", "]")
                .add("queuePairNumber=" + queuePair.getQueuePairNumber())
                .add("localId=" + localId)
                .add("portNumber=" + portNumber)
                .toString();
    }
}
