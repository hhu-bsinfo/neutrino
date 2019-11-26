package de.hhu.bsinfo.neutrino.api.message.impl.buffer;

import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.util.AtomicIntegerStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Value;

import java.util.function.Function;

public class BufferPool {

    private final IndexedByteBuf[] indexedBuffers;
    private final AtomicIntegerStack stack;

    private BufferPool(final IndexedByteBuf[] indexedBuffers, final AtomicIntegerStack stack) {
        this.indexedBuffers = indexedBuffers;
        this.stack = stack;
    }

    public static BufferPool create(int mtu, int count, Function<ByteBuf, RegisteredByteBuf> supplier) {
        var buffers = new IndexedByteBuf[count];
        var stack = new AtomicIntegerStack(count);
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new IndexedByteBuf(i, supplier.apply(Unpooled.directBuffer(mtu, mtu)));
            stack.push(i);
        }

        return new BufferPool(buffers, stack);
    }

    public IndexedByteBuf leaseNext() {
        int index;
        while ((index = stack.pop()) == -1);
        return indexedBuffers[index];
    }

    public void release(int index) {
        indexedBuffers[index].buffer.clear();
        if (!stack.push(index)) {
            throw new IllegalStateException("stack overflow detected");
        }
    }

    public static final @Value class IndexedByteBuf {
        private final int index;
        private final RegisteredByteBuf buffer;

        public ByteBuf getActualBuffer() {
            return buffer.getBuffer();
        }
    }
}
