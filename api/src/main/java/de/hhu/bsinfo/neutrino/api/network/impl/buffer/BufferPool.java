package de.hhu.bsinfo.neutrino.api.network.impl.buffer;

import de.hhu.bsinfo.neutrino.api.util.MemoryUtil;
import de.hhu.bsinfo.neutrino.api.util.UnsafeRegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.agrona.concurrent.QueuedPipe;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.hints.ThreadHints;

import java.io.IOException;
import java.util.function.IntConsumer;

@Slf4j
public final class BufferPool {

    @FunctionalInterface
    public interface BufferRegistrator {
        MemoryRegion wrap(long handle, long capacity, AccessFlag... accessFlags) throws IOException;
    }

    /**
     * The buffer used for creating pooled buffer instances.
     */
    private final AtomicBuffer baseBuffer;

    /**
     * The base buffers memory region.
     */
    private final MemoryRegion baseRegion;

    /**
     * Pooled buffers stored using their identifiers as indices.
     */
    private final PooledBuffer[] indexedBuffers;

    /**
     * Pooled buffers.
     */
    private final QueuedPipe<PooledBuffer> buffers;

    public BufferPool(final BufferRegistrator registrator, final int count, final int size) throws IOException {
        indexedBuffers = new PooledBuffer[count];
        buffers = new ManyToManyConcurrentArrayQueue<>(count);

        // Create base buffer containing enough space for pooled buffers
        // and register it with the InfiniBand hardware
        baseBuffer = MemoryUtil.allocateAligned(count * size, MemoryAlignment.PAGE);
        baseRegion = registrator.wrap(baseBuffer.addressOffset(), baseBuffer.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);

        var baseAddress = baseBuffer.addressOffset();
        IntConsumer releaser = this::release;
        for (int i = 0; i < count; i++) {

            // Slice the next chunk of memory from our base buffer
            var slice = new UnsafeBuffer(baseAddress + ((long) i * size), size);

            // Create a new pooled buffer using the previously sliced chunk of memory
            indexedBuffers[i] = new PooledBuffer(i, slice, baseRegion, releaser);

            // Push the pooled buffer into our queue
            buffers.add(indexedBuffers[i]);
        }
    }

    public PooledBuffer claim() {

        // Create variable for spin-wait loop
        PooledBuffer tmp;

        // Busy-wait until we get an buffer
        while ((tmp = buffers.poll()) == null) {
            ThreadHints.onSpinWait();
        }

        // Return the claimed buffer
        return tmp;
    }

    public void release(int identifier) {

        // Get buffer by identifier
        var buffer = indexedBuffers[identifier];

        // Clear buffer for next usage
        //        buffer.clear();

        // Put buffer back into queue
        while (!buffers.offer(buffer)) {
            ThreadHints.onSpinWait();
        }
    }

    public PooledBuffer get(int identifier) {
        return indexedBuffers[identifier];
    }

    @Override
    public String toString() {
        var first = indexedBuffers[0];
        var last = indexedBuffers[indexedBuffers.length - 1];
        return String.format("BufferPool { region: [ 0x%08X , 0x%08X ] }",
                first.addressOffset(), last.addressOffset()+ last.capacity());
    }

    public static final class PooledBuffer extends UnsafeRegisteredBuffer {

        /**
         * This pooled buffer's identifier.
         */
        private final int identifier;

        /**
         * This pooled buffer's memory region.
         */
        private final MemoryRegion memoryRegion;

        /**
         * Function for returning this pooled buffer to its pool.
         */
        private final IntConsumer releaser;

        /**
         * Preallocated receive work request filled with this pooled buffers information.
         */
        private final ReceiveWorkRequest receiveRequest;

        /**
         * Preallocated scatter-gather element filled with this buffers information.
         */
        private final ScatterGatherElement element;

        public PooledBuffer(int identifier, DirectBuffer buffer, MemoryRegion memoryRegion, IntConsumer releaser) {
            super(buffer, memoryRegion);
            this.identifier = identifier;
            this.releaser = releaser;
            this.memoryRegion = memoryRegion;

            element = new ScatterGatherElement(buffer.addressOffset(), capacity(), memoryRegion.getLocalKey());
            receiveRequest = new ReceiveWorkRequest.Builder()
                    .withId(identifier)
                    .withScatterGatherElement(element)
                    .build();
        }

        public int getIdentifier() {
            return identifier;
        }

        public int getLocalKey() {
            return memoryRegion.getLocalKey();
        }

        public int getRemoteKey() {
            return memoryRegion.getRemoteKey();
        }

        public ReceiveWorkRequest getReceiveRequest() {
            return receiveRequest;
        }

        public ScatterGatherElement getElement() {
            return element;
        }

        public void release() {
            releaser.accept(identifier);
        }
    }
}
