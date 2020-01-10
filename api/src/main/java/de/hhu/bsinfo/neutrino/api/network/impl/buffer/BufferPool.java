package de.hhu.bsinfo.neutrino.api.network.impl.buffer;

import de.hhu.bsinfo.neutrino.util.AtomicIntegerStack;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BufferPool {

    @FunctionalInterface
    public interface BufferRegistrator {
        MemoryRegion wrap(long handle, long capacity, AccessFlag... accessFlags);
    }

    @FunctionalInterface
    public interface BufferReleaser {
        void release(int index);
    }

    private final IndexedByteBuf[] indexedBuffers;
    private final AtomicIntegerStack stack;

    private BufferPool(final IndexedByteBuf[] indexedBuffers, final AtomicIntegerStack stack) {
        this.indexedBuffers = indexedBuffers;
        this.stack = stack;
    }

    public static BufferPool create(int mtu, int count, BufferRegistrator registrator) {
        var buffers = new IndexedByteBuf[count];
        var stack = new AtomicIntegerStack();
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new IndexedByteBuf(i, UnpooledByteBufAllocator.DEFAULT, mtu, mtu, registrator);
            stack.push(i);
        }

        var bufferPool = new BufferPool(buffers, stack);
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].setReleaser(bufferPool::release);
        }

        return bufferPool;
    }

    public IndexedByteBuf leaseNext() {
        int index;
        long then = System.currentTimeMillis();
        while ((index = stack.pop()) == -1) {
            if (System.currentTimeMillis() - then > 2000) {
                log.warn("Waiting over 2 seconds for buffer lease");
                then = System.currentTimeMillis();
            }
        }
        return indexedBuffers[index];
    }

    public void release(int index) {
        indexedBuffers[index].clear();
        stack.push(index);
    }

    public IndexedByteBuf get(int index) {
        return indexedBuffers[index];
    }

    public static final class IndexedByteBuf extends UnpooledUnsafeDirectByteBuf {

        private final int index;
        private final MemoryRegion memoryRegion;
        private BufferReleaser releaser;

        public IndexedByteBuf(int index, ByteBufAllocator alloc, int initialCapacity, int maxCapacity, BufferRegistrator registrator) {
            super(alloc, initialCapacity, maxCapacity);
            this.index = index;
            memoryRegion = registrator.wrap(memoryAddress(), capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
        }

        public int getIndex() {
            return index;
        }

        public int getLocalKey() {
            return memoryRegion.getLocalKey();
        }

        public int getRemoteKey() {
            return memoryRegion.getRemoteKey();
        }

        public void setReleaser(BufferReleaser releaser) {
            this.releaser = releaser;
        }

        @Override
        protected void deallocate() {
            log.debug("Deallocating buffer at index {}", index);
            setRefCnt(1);
            releaser.release(index);
        }
    }
}
