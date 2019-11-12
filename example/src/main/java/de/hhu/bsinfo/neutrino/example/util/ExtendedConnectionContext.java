package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ExtendedConnectionContext extends BaseContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionContext.class);

    private final RegisteredBuffer localBuffer;
    private final ExtendedCompletionQueue completionQueue;
    private final CompletionChannel completionChannel;

    private final QueuePair queuePair;

    private final PortAttributes port;

    public ExtendedConnectionContext(int deviceNumber, int queueSize, long messageSize, ExtendedCompletionQueue.WorkCompletionCapability... workCompletionCapabilities) throws IOException {
        super(deviceNumber);

        port = getContext().queryPort(1);
        if(port == null) {
            throw new IOException("Unable to query port");
        }

        localBuffer = getProtectionDomain().allocateMemory(messageSize, AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_READ, AccessFlag.REMOTE_WRITE, AccessFlag.MW_BIND);
        if(localBuffer == null) {
            throw new IOException("Unable to allocate message buffer");
        }

        LOGGER.info("Allocated registered memory buffer");

        completionChannel = getContext().createCompletionChannel();
        if(completionChannel == null) {
            throw new IOException("Unable to create completion channel");
        }

        LOGGER.info("Created completion channel");

        completionQueue = getContext().createExtendedCompletionQueue(new ExtendedCompletionQueue.InitialAttributes.Builder(queueSize)
                .withCompletionChannel(completionChannel).withWorkCompletionCapabilities(workCompletionCapabilities).build());
        if(completionQueue == null) {
            throw new IOException("Unable to create completion queue");
        }

        LOGGER.info("Created completion queue");

        queuePair = getContext().createExtendedQueuePair(new ExtendedQueuePair.InitialAttributes.Builder(QueuePair.Type.RC, getProtectionDomain())
                .withSendCompletionQueue(completionQueue.toCompletionQueue())
                .withReceiveCompletionQueue(completionQueue.toCompletionQueue())
                .withMaxSendWorkRequests(queueSize)
                .withMaxReceiveWorkRequests(queueSize)
                .withMaxSendScatterGatherElements(1)
                .withMaxReceiveScatterGatherElements(1)
                .withSendOperationFlags(ExtendedQueuePair.SendOperationFlag.WITH_SEND,
                        ExtendedQueuePair.SendOperationFlag.WITH_SEND_WITH_IMM,
                        ExtendedQueuePair.SendOperationFlag.WITH_RDMA_READ,
                        ExtendedQueuePair.SendOperationFlag.WITH_RDMA_WRITE,
                        ExtendedQueuePair.SendOperationFlag.WITH_BIND_MW)
                .build());
        if(queuePair == null) {
            throw new IOException("Unable to create queue pair");
        }

        LOGGER.info("Created queue pair");

        if(!queuePair.modify(QueuePair.Attributes.Builder.buildInitAttributesRC((short) 0, (byte) 1, AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_READ, AccessFlag.REMOTE_WRITE))) {
            throw new IOException("Unable to move queue pair into INIT state");
        }

        LOGGER.info("Moved queue pair into INIT state");
    }

    public void connect(Socket socket) throws IOException {
        var localInfo = new ConnectionInformation((byte) 1, port.getLocalId(), queuePair.getQueuePairNumber());

        LOGGER.info("Local connection information: {}", localInfo);

        socket.getOutputStream().write(ByteBuffer.allocate(Byte.BYTES + Short.BYTES + Integer.BYTES)
                .put(localInfo.getPortNumber())
                .putShort(localInfo.getLocalId())
                .putInt(localInfo.getQueuePairNumber())
                .array());

        LOGGER.info("Waiting for remote connection information");

        var byteBuffer = ByteBuffer.wrap(socket.getInputStream().readNBytes(Byte.BYTES + Short.BYTES + Integer.BYTES));
        var remoteInfo = new ConnectionInformation(byteBuffer);

        LOGGER.info("Received connection information: {}", remoteInfo);

        if(!queuePair.modify(QueuePair.Attributes.Builder.buildReadyToReceiveAttributesRC(
                remoteInfo.getQueuePairNumber(), remoteInfo.getLocalId(), remoteInfo.getPortNumber()))) {
            throw new IOException("Unable to move queue pair into RTR state");
        }

        LOGGER.info("Moved queue pair into RTR state");

        if(!queuePair.modify(QueuePair.Attributes.Builder.buildReadyToSendAttributesRC())) {
            throw new IOException("Unable to move queue pair into RTS state");
        }

        LOGGER.info("Moved queue pair into RTS state");
    }

    public RegisteredBuffer getLocalBuffer() {
        return localBuffer;
    }

    public ExtendedCompletionQueue getCompletionQueue() {
        return completionQueue;
    }

    public CompletionChannel getCompletionChannel() {
        return completionChannel;
    }

    public ExtendedQueuePair getQueuePair() {
        return queuePair.toExtendedQueuePair();
    }

    @Override
    public void close() {
        queuePair.close();
        completionQueue.close();
        completionChannel.close();
        localBuffer.close();
        super.close();

        LOGGER.info("Closed context");
    }
}
