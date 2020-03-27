package de.hhu.bsinfo.neutrino.api.network.impl.buffer;

import de.hhu.bsinfo.neutrino.verbs.*;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.QueuedPipe;
import org.agrona.hints.ThreadHints;

import java.util.function.IntConsumer;

@Slf4j
public final class BufferPool {

    @FunctionalInterface
    public interface BufferRegistrator {
        MemoryRegion wrap(long handle, long capacity, AccessFlag... accessFlags);
    }

    /**
     * Pooled buffers stored using their identifiers as indices.
     */
    private final PooledBuffer[] indexedBuffers;

    /**
     * Pooled buffers.
     */
    private final QueuedPipe<PooledBuffer> buffers;

    public BufferPool(final BufferRegistrator registrator, final int count, final int size) {
        indexedBuffers = new PooledBuffer[count];
        buffers = new ManyToOneConcurrentArrayQueue<>(count);

        IntConsumer releaser = this::release;
        for (int i = 0; i < count; i++) {
            indexedBuffers[i] = new PooledBuffer(i, size, releaser, registrator);
            buffers.add(indexedBuffers[i]);
        }

        log.debug("Created buffer poll with {} buffers of size {}B", count, size);
    }

    public PooledBuffer claim() {

        // Create variable for spin-wait loop
        PooledBuffer tmp = null;

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
        buffer.clear();

        // Put buffer back into queue
        while (!buffers.offer(buffer)) {
            ThreadHints.onSpinWait();
        }
    }

    public PooledBuffer get(int identifier) {
        return indexedBuffers[identifier];
    }

    public static final class PooledBuffer extends UnpooledUnsafeDirectByteBuf {

        private static final ByteBufAllocator ALLOCATOR = UnpooledByteBufAllocator.DEFAULT;

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

        public PooledBuffer(int identifier, int capacity, IntConsumer releaser, BufferRegistrator registrator) {
            super(ALLOCATOR, capacity, capacity);
            this.identifier = identifier;
            this.releaser = releaser;
            memoryRegion = registrator.wrap(memoryAddress(), capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);

            element = new ScatterGatherElement(memoryAddress(), capacity, memoryRegion.getLocalKey());
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

        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        protected void deallocate() {

            // Reset the reference count
            setRefCnt(1);

            // Put this buffer back into its pool
            releaser.accept(identifier);
        }
    }
}
