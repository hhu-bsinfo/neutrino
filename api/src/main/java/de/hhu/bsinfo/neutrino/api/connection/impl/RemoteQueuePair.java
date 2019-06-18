package de.hhu.bsinfo.neutrino.api.connection.impl;

import java.nio.ByteBuffer;
import java.util.StringJoiner;

public class RemoteQueuePair {

    private short localId;
    private int queuePairNumber;
    private byte portNumber;

    public RemoteQueuePair() {}

    public RemoteQueuePair(short localId, int queuePairNumber, byte portNumber) {
        this.localId = localId;
        this.queuePairNumber = queuePairNumber;
        this.portNumber = portNumber;
    }

    public RemoteQueuePair(ByteBuffer buffer) {
        localId = buffer.getShort();
        queuePairNumber = buffer.getInt();
        portNumber = buffer.get();
    }

    public short getLocalId() {
        return localId;
    }

    public int getQueuePairNumber() {
        return queuePairNumber;
    }

    public byte getPortNumber() {
        return portNumber;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RemoteQueuePair.class.getSimpleName() + "[", "]")
                .add("localId=" + localId)
                .add("queuePairNumber=" + queuePairNumber)
                .add("portNumber=" + portNumber)
                .toString();
    }
}
