package de.hhu.bsinfo.neutrino.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ExtendedRdmaContext extends ExtendedConnectionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedRdmaContext.class);

    private RdmaContext.BufferInformation remoteBufferInfo;

    public ExtendedRdmaContext(int deviceNumber, int queueSize, long bufferSize) throws IOException {
        super(deviceNumber, queueSize, bufferSize);
    }

    @Override
    public void connect(Socket socket) throws IOException {
        super.connect(socket);

        var localBufferInfo = new RdmaContext.BufferInformation(getLocalBuffer().getHandle(), getLocalBuffer().getRemoteKey());

        LOGGER.info("Local buffer information: {}", localBufferInfo);

        socket.getOutputStream().write(ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(localBufferInfo.getAddress())
                .putInt(localBufferInfo.getRemoteKey())
                .array());

        LOGGER.info("Waiting for remote buffer information");

        var byteBuffer = ByteBuffer.wrap(socket.getInputStream().readNBytes(Long.BYTES + Integer.BYTES));
        remoteBufferInfo = new RdmaContext.BufferInformation(byteBuffer);

        LOGGER.info("Received buffer information: {}", remoteBufferInfo);
    }

    public RdmaContext.BufferInformation getRemoteBufferInfo() {
        return remoteBufferInfo;
    }
}
