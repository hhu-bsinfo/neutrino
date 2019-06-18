package de.hhu.bsinfo.neutrino.api.connection.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ConnectionHandler {

    private static final int PAYLOAD_SIZE = Short.BYTES + Integer.BYTES + Byte.BYTES;

    private final ByteBuffer receiveBuffer;
    private final ByteBuffer sendBuffer;

    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private final Connection connection;

    ConnectionHandler(SocketChannel socketChannel, SelectionKey selectionKey, Connection connection) {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;
        this.connection = connection;

        receiveBuffer = ByteBuffer.allocateDirect(PAYLOAD_SIZE);
        sendBuffer = ByteBuffer.allocateDirect(PAYLOAD_SIZE);

        sendBuffer.putShort(connection.getLocalId());
        sendBuffer.putInt(connection.getQueuePair().getQueuePairNumber());
        sendBuffer.put(connection.getPortNumber());
        sendBuffer.flip();
    }

    RemoteQueuePair read() throws IOException {
        if (receiveBuffer.hasRemaining()) {
            socketChannel.read(receiveBuffer);
        }

        if (!receiveBuffer.hasRemaining()) {
            selectionKey.interestOpsAnd(~SelectionKey.OP_READ);
            return new RemoteQueuePair(receiveBuffer.flip());
        }

        return null;
    }

    void write() throws IOException {
        if (sendBuffer.hasRemaining()) {
            socketChannel.write(sendBuffer);
        }

        if (!sendBuffer.hasRemaining()) {
            selectionKey.interestOpsAnd(~SelectionKey.OP_WRITE);
        }
    }

    public RemoteQueuePair getRemoteQueuePair() {
        return new RemoteQueuePair(receiveBuffer.flip());
    }

    boolean isFinished() {
        return !receiveBuffer.hasRemaining() && !sendBuffer.hasRemaining();
    }

    void cancel() {
        selectionKey.attach(null);
        selectionKey.cancel();
    }

    Connection getConnection() {
        return connection;
    }

}
