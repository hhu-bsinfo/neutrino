package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.StringJoiner;

class BaseContext implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseContext.class);

    private final Context context;
    private final ProtectionDomain protectionDomain;

    BaseContext(int deviceNumber) throws IOException {
        int numDevices = Context.getDeviceCount();

        if(numDevices <= deviceNumber) {
            throw new InvalidParameterException("Invalid device number '" + deviceNumber + "'. Only " + numDevices + " InfiniBand " + (numDevices == 1 ? "device was" : "devices were") + " found in your system");
        }

        context = Context.openDevice(deviceNumber);
        if(context == null) {
            throw new IOException("Unable to open context");
        }

        LOGGER.info("Opened context for device {}", context.getDeviceName());

        protectionDomain = context.allocateProtectionDomain();
        if(protectionDomain == null) {
            throw new IOException("Unable to allocate protection domain");
        }

        LOGGER.info("Allocated protection domain");
    }

    Context getContext() {
        return context;
    }

    ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public void close() {
        protectionDomain.close();
        context.close();
    }

    public static final class ConnectionInformation {

        private final byte portNumber;
        private final short localId;
        private final int queuePairNumber;

        ConnectionInformation(byte portNumber, short localId, int queuePairNumber) {
            this.portNumber = portNumber;
            this.localId = localId;
            this.queuePairNumber = queuePairNumber;
        }

        ConnectionInformation(ByteBuffer buffer) {
            portNumber = buffer.get();
            localId = buffer.getShort();
            queuePairNumber = buffer.getInt();
        }

        public byte getPortNumber() {
            return portNumber;
        }

        public short getLocalId() {
            return localId;
        }

        public int getQueuePairNumber() {
            return queuePairNumber;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ConnectionInformation.class.getSimpleName() + "[", "]")
                    .add("portNumber=" + portNumber)
                    .add("localId=" + localId)
                    .add("queuePairNumber=" + queuePairNumber)
                    .toString();
        }
    }
}
