package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import lombok.extern.slf4j.Slf4j;
import org.agrona.BitUtil;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;

@Slf4j
public class UnsafeRegisteredBuffer extends UnsafeBuffer implements RegisteredBuffer {

    private final MemoryRegion region;

    public UnsafeRegisteredBuffer(DirectBuffer buffer, MemoryRegion region) {
        super(buffer);

        if (buffer.byteArray() != null) {
            throw new IllegalArgumentException("wrapped byte[] is not supported");
        }

        if (region.getLength() >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("length must be less than Integer.MAX_VALUE");
        }

        if (!MemoryUtil.contains(region, buffer)) {
            throw new IllegalArgumentException("memory region does not contain buffer");
        }

        this.region = region;
    }

    @Override
    public int localKey() {
        return region.getLocalKey();
    }

    @Override
    public int remoteKey() {
        return region.getRemoteKey();
    }

    @Override
    public void release() throws IOException {
        region.close();
    }

    @Override
    public String toString() {
        return String.format("UnsafeRegisteredBuffer{ region: [ 0x%08X , 0x%08X ], capacity: %d, lkey: 0x%04X }",
                addressOffset(), addressOffset() + capacity(), capacity(), localKey());
    }
}
