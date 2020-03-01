package de.hhu.bsinfo.neutrino.data;

import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import java.nio.ByteBuffer;

public class NativeByte extends NativeDataType {

    public NativeByte(final LocalBuffer byteBuffer, final long offset) {
        super(byteBuffer, offset);
    }

    @Override
    public long getSize() {
        return Byte.BYTES;
    }

    public void set(final byte value) {
        getByteBuffer().put(getOffset(), value);
    }

    public byte get() {
        return getByteBuffer().get(getOffset());
    }

    @Override
    public String toString() {
        return super.toString() + " " + get();
    }
}
