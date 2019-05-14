package de.hhu.bsinfo.rdma.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NativeString extends NativeDataType {

    private final int length;

    public NativeString(final ByteBuffer byteBuffer, final int offset, final int length) {
        super(byteBuffer, offset);
        this.length = length;
    }

    public void set(final String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        getByteBuffer().put(bytes, getOffset(), length > bytes.length ? bytes.length : length);

        if (length > bytes.length) {
            getByteBuffer().put(new byte[length - bytes.length], getOffset() + bytes.length, length - bytes.length);
        }
    }

    public String get() {
        byte[] bytes = new byte[length];
        getByteBuffer().get(bytes, getOffset(), length);
        return new String(bytes, 0, getLength(bytes), StandardCharsets.US_ASCII);
    }

    private int getLength(byte[] bytes) {
        for(int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                return i;
            }
        }

        return bytes.length;
    }

    @Override
    public String toString() {
        return super.toString() + " " + get();
    }
}
