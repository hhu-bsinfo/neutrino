package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.accessor.ScatterGatherAccessor;
import de.hhu.bsinfo.neutrino.api.network.impl.accessor.SendRequestAccessor;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.RequestBuffer;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.BitMask;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.hints.ThreadHints;

import static org.agrona.concurrent.ringbuffer.RingBuffer.INSUFFICIENT_CAPACITY;

@Slf4j
public class Requests {

    private static final SendWorkRequest REQUEST_ACCESSOR = new SendWorkRequest(0);
    private static final ScatterGatherElement ELEMENT_ACCESSOR = new ScatterGatherElement(0);

    private static final int SCATTER_GATHER_OFFSET = SendRequestAccessor.ELEMENT_SIZE;
    private static final int SEND_FLAGS = BitMask.intOf(SendWorkRequest.SendFlag.SIGNALED);
    private static final int SEND_OPCODE = SendWorkRequest.OpCode.SEND.getValue();
    private static final int READ_OPCODE = SendWorkRequest.OpCode.RDMA_READ.getValue();
    private static final int WRITE_OPCODE = SendWorkRequest.OpCode.RDMA_WRITE.getValue();

    private static final int NO_SCATTER_GATHER = 0;
    private static final int SINGLE_SCATTER_GATHER = 1;

    public static void appendSend(int id, BufferPool.PooledBuffer target, RequestBuffer buffer, DirectBuffer data, int offset, int length) {

        // Copy bytes into send buffer
        target.putBytes(0, data, offset, length);

        int index;
        while ((index = buffer.tryClaim(SINGLE_SCATTER_GATHER)) == INSUFFICIENT_CAPACITY) {
            ThreadHints.onSpinWait();
        }

        var requestHandle = buffer.memoryAddress() + index;
        var elementHandle = requestHandle + SCATTER_GATHER_OFFSET;

        // Set work request parameters
        SendRequestAccessor.setId(requestHandle, Identifier.create(id, (short) 0, (short) target.getIdentifier()));
        SendRequestAccessor.setListHandle(requestHandle, elementHandle);
        SendRequestAccessor.setListLength(requestHandle, 1);
        SendRequestAccessor.setOpCode(requestHandle, SEND_OPCODE);
        SendRequestAccessor.setFlags(requestHandle, SEND_FLAGS);

        // Set scatter gather element parameters
        ScatterGatherAccessor.setAddress(elementHandle, target.addressOffset());
        ScatterGatherAccessor.setLength(elementHandle, length);
        ScatterGatherAccessor.setLocalKey(elementHandle, target.getLocalKey());

        // Commit the written request
        buffer.commitWrite(index);
    }

    public static void appendRead(int id, RequestBuffer buffer, RemoteHandle source, RegisteredBuffer target, int offset, int length) {

        int index;
        while ((index = buffer.tryClaim(SINGLE_SCATTER_GATHER)) == INSUFFICIENT_CAPACITY) {
            ThreadHints.onSpinWait();
        }

        var requestHandle = buffer.memoryAddress() + index;
        var elementHandle = requestHandle + SCATTER_GATHER_OFFSET;

        // Set work request parameters
        SendRequestAccessor.setId(requestHandle, Identifier.create(id, (short) 0));
        SendRequestAccessor.setListHandle(requestHandle, elementHandle);
        SendRequestAccessor.setListLength(requestHandle, 1);
        SendRequestAccessor.setOpCode(requestHandle, READ_OPCODE);
        SendRequestAccessor.setFlags(requestHandle, SEND_FLAGS);
        SendRequestAccessor.setRdmaRemoteAddress(requestHandle, source.getAddress());
        SendRequestAccessor.setRdmaRemoteKey(requestHandle, source.getKey());

        // Set scatter gather element parameters
        ScatterGatherAccessor.setAddress(elementHandle, target.addressOffset() + offset);
        ScatterGatherAccessor.setLength(elementHandle, length);
        ScatterGatherAccessor.setLocalKey(elementHandle, target.localKey());

        // Commit the written request
        buffer.commitWrite(index);
    }

    public static void appendWrite(int id, RequestBuffer buffer, RegisteredBuffer source, int offset, int length, RemoteHandle target) {

        int index;
        while ((index = buffer.tryClaim(SINGLE_SCATTER_GATHER)) == INSUFFICIENT_CAPACITY) {
            ThreadHints.onSpinWait();
        }

        var requestHandle = buffer.memoryAddress() + index;
        var elementHandle = requestHandle + SCATTER_GATHER_OFFSET;

        // Set work request parameters
        SendRequestAccessor.setId(requestHandle, Identifier.create(id, (short) 0));
        SendRequestAccessor.setListHandle(requestHandle, elementHandle);
        SendRequestAccessor.setListLength(requestHandle, 1);
        SendRequestAccessor.setOpCode(requestHandle, WRITE_OPCODE);
        SendRequestAccessor.setFlags(requestHandle, SEND_FLAGS);
        SendRequestAccessor.setRdmaRemoteAddress(requestHandle, target.getAddress());
        SendRequestAccessor.setRdmaRemoteKey(requestHandle, target.getKey());

        // Set scatter gather element parameters
        ScatterGatherAccessor.setAddress(elementHandle, source.addressOffset() + offset);
        ScatterGatherAccessor.setLength(elementHandle, length);
        ScatterGatherAccessor.setLocalKey(elementHandle, source.localKey());

        // Commit the written request
        buffer.commitWrite(index);
    }
}
