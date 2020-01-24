package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;

import java.nio.ByteBuffer;

public final class Buffer extends UnpooledUnsafeDirectByteBuf {

    private static final boolean PREFER_DIRECT_BUFFER = true;
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(PREFER_DIRECT_BUFFER);

    private final MemoryRegion memoryRegion;

    private Buffer(int capacity, BufferRegistrator registrator) {
        super(ALLOCATOR, capacity, capacity);
        memoryRegion = registrator.wrap(memoryAddress(), capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
    }

    private Buffer(ByteBuffer initialBuffer, BufferRegistrator registrator) {
        super(ALLOCATOR, initialBuffer, initialBuffer.remaining());
        memoryRegion = registrator.wrap(memoryAddress(), capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
    }

    public static Buffer allocate(int capacity, BufferRegistrator registrator){
        return new Buffer(capacity, registrator);
    }

    public static Buffer allocate(ByteBuffer initialBuffer, BufferRegistrator registrator){
        return new Buffer(initialBuffer, registrator);
    }

    public int getLocalKey() {
        return memoryRegion.getLocalKey();
    }

    public int getRemoteKey() {
        return memoryRegion.getRemoteKey();
    }
}
