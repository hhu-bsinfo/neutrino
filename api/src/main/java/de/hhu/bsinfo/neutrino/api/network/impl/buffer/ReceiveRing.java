package de.hhu.bsinfo.neutrino.api.network.impl.buffer;

import de.hhu.bsinfo.neutrino.api.network.impl.accessor.ReceiveRequestAccessor;
import de.hhu.bsinfo.neutrino.api.network.impl.accessor.ScatterGatherAccessor;
import de.hhu.bsinfo.neutrino.api.util.BufferRegistrator;
import de.hhu.bsinfo.neutrino.api.util.MemoryUtil;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.api.util.UnsafeRegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.verbs.*;
import lombok.extern.slf4j.Slf4j;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

@Slf4j
@NotThreadSafe
public final class ReceiveRing {

    private static final int RECEIVE_BUFFER_SIZE = MemoryAlignment.PAGE.value();

    private static final int ENTRY_SIZE = BitUtil.findNextPositivePowerOfTwo(
            ReceiveRequestAccessor.ELEMENT_SIZE + ScatterGatherAccessor.ELEMENT_SIZE);

    private static final int LIST_LENGTH = 1;

    private static final long NULL = 0;

    /**
     * The circular linked list of receive work requests referencing chunks within the data buffer.
     */
    private final MutableDirectBuffer requests;

    /**
     * The underlying data buffer in which received data is written.
     */
    private final RegisteredBuffer data;

    /**
     * Chunks of memory sliced from the underlying data buffer.
     */
    private final DirectBuffer[] indexedBuffers;

    /**
     * Helper object used for posting to the shared receive queue.
     */
    private final ReceiveWorkRequest workRequest = new ReceiveWorkRequest(0);

    private final ScatterGatherElement element = new ScatterGatherElement(0);

    /**
     * Virtual memory address at which the circular linked list starts.
     */
    private final long requestAddress;

    /**
     * Mask used to keep offset within memory bounds
     */
    private final int offsetMask;

    /**
     * The offset of the next available work request.
     */
    private int currentOffset;

    public ReceiveRing(int entries, BufferRegistrator registrator) throws IOException {

        // Use a power of two so that we can use a mask for wrapping around
        var capacity = BitUtil.findNextPositivePowerOfTwo(entries);

        // Create request and data buffers
        var buffer = MemoryUtil.allocateAligned(capacity * RECEIVE_BUFFER_SIZE, MemoryAlignment.PAGE);
        var region = registrator.wrap(buffer.addressOffset(), buffer.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
        data = new UnsafeRegisteredBuffer(buffer, region);
        requests = MemoryUtil.allocateAligned(capacity * ENTRY_SIZE, MemoryAlignment.PAGE);
        indexedBuffers = new DirectBuffer[capacity];
        offsetMask = requests.capacity() - 1;

        // Fill n-1 requests and link them
        var dataOffset = data.addressOffset();
        var requestOffset = requests.addressOffset();
        requestAddress = requestOffset;

        for (int i = 0; i < capacity - 1; i++) {

            // Calculate work request and scatter gather element offsets
            var requestHandle = requestOffset + (long) i * ENTRY_SIZE;
            var elementHandle = requestHandle + ReceiveRequestAccessor.ELEMENT_SIZE;

            // Fill in work request data
            ReceiveRequestAccessor.setId(requestHandle, i);
            ReceiveRequestAccessor.setNext(requestHandle, requestHandle + ENTRY_SIZE);
            ReceiveRequestAccessor.setListHandle(requestHandle, elementHandle);
            ReceiveRequestAccessor.setListLength(requestHandle, LIST_LENGTH);

            // Fill in scatter-gather element data
            ScatterGatherAccessor.setAddress(elementHandle, dataOffset + (long) i * RECEIVE_BUFFER_SIZE);
            ScatterGatherAccessor.setLength(elementHandle, RECEIVE_BUFFER_SIZE);
            ScatterGatherAccessor.setLocalKey(elementHandle, data.localKey());

            // Create sliced buffer for consumer
            indexedBuffers[i] = new UnsafeBuffer(dataOffset + (long) i * RECEIVE_BUFFER_SIZE, RECEIVE_BUFFER_SIZE);
        }

        // Fill last request and create link it with the first one
        var i = (capacity - 1);
        var requestHandle = requestOffset + (long) i * ENTRY_SIZE;
        var elementHandle = requestHandle + ReceiveRequestAccessor.ELEMENT_SIZE;

        // Fill in work request data
        ReceiveRequestAccessor.setId(requestHandle, i);
        ReceiveRequestAccessor.setNext(requestHandle, requestOffset);
        ReceiveRequestAccessor.setListHandle(requestHandle, elementHandle);
        ReceiveRequestAccessor.setListLength(requestHandle, LIST_LENGTH);

        // Fill in scatter-gather element data
        ScatterGatherAccessor.setAddress(elementHandle, dataOffset + (long) i * RECEIVE_BUFFER_SIZE);
        ScatterGatherAccessor.setLength(elementHandle, RECEIVE_BUFFER_SIZE);
        ScatterGatherAccessor.setLocalKey(elementHandle, data.localKey());

        // Create sliced buffer for consumer
        indexedBuffers[i] = new UnsafeBuffer(dataOffset + (long) i * RECEIVE_BUFFER_SIZE, RECEIVE_BUFFER_SIZE);
        currentOffset = 0;
    }

    public void post(SharedReceiveQueue sharedReceiveQueue, int count) throws IOException {
        if (count == 0) {
            return;
        }

        // Calculate the offsets of the first and last request
        var startOffset = currentOffset;
        var endOffset = (startOffset + ((count - 1) * ENTRY_SIZE)) & offsetMask;

        // Calculate the virtual memory address for each request
        var startAddress = address(startOffset);
        var endAddress = address(endOffset);

        // Remember the next element before unlinking it
        var next = ReceiveRequestAccessor.getNext(endAddress);

        // Unlink the last element
        ReceiveRequestAccessor.setNext(endAddress, NULL);

//        workRequest.wrap(startAddress).toString().lines().forEach(log::info);
//        element.wrap(startAddress + ReceiveRequestAccessor.ELEMENT_SIZE).toString().lines().forEach(log::info);
//        workRequest.wrap(endAddress).toString().lines().forEach(log::info);
//        element.wrap(endAddress + ReceiveRequestAccessor.ELEMENT_SIZE).toString().lines().forEach(log::info);

        // Post the specified number of receive work requests
        sharedReceiveQueue.postReceive(workRequest.wrap(startAddress));

        // Restore the last element's link
        ReceiveRequestAccessor.setNext(endAddress, next);

        // Advance the current offset
        currentOffset = (endOffset + ENTRY_SIZE) & offsetMask;
    }

    public DirectBuffer get(int index) {
        return indexedBuffers[index];
    }

    @Override
    public String toString() {
        return String.format("ReceiveRing { region: [ 0x%08X , 0x%08X ] }",
                data.addressOffset(), data.addressOffset() + data.capacity());
    }

    private long address(int offset) {
        return requestAddress + (long) offset;
    }
}
