package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.buffer.RemoteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.StringJoiner;

public class RdmaContext extends DefaultContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdmaContext.class);

    private BufferInformation remoteBufferInfo;

    public RdmaContext(int deviceNumber, int queueSize, long bufferSize) throws IOException {
        super(deviceNumber, queueSize, bufferSize);
    }

    @Override
    public void connect(Socket socket) throws IOException {
        super.connect(socket);

        var localBufferInfo = new BufferInformation(getLocalBuffer().getHandle(), getLocalBuffer().getRemoteKey());

        LOGGER.info("Local buffer information: {}", localBufferInfo);

        socket.getOutputStream().write(ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(localBufferInfo.getAddress())
                .putInt(localBufferInfo.getRemoteKey())
                .array());

        LOGGER.info("Waiting for remote buffer information");

        var byteBuffer = ByteBuffer.wrap(socket.getInputStream().readNBytes(Long.BYTES + Integer.BYTES));
        remoteBufferInfo = new BufferInformation(byteBuffer);

        LOGGER.info("Received buffer information: {}", remoteBufferInfo);
    }

    public BufferInformation getRemoteBufferInfo() {
        return remoteBufferInfo;
    }

    public static final class BufferInformation {

        private final long address;
        private final int remoteKey;

        BufferInformation(long address, int remoteKey) {
            this.address = address;
            this.remoteKey = remoteKey;
        }

        BufferInformation(ByteBuffer buffer) {
            address = buffer.getLong();
            remoteKey = buffer.getInt();
        }

        public long getAddress() {
            return address;
        }

        public int getRemoteKey() {
            return remoteKey;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BufferInformation.class.getSimpleName() + "[", "]")
                    .add("address=" + address)
                    .add("remoteKey=" + remoteKey)
                    .toString();
        }
    }
}
