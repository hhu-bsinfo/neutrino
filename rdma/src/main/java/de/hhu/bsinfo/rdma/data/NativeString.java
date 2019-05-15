package de.hhu.bsinfo.rdma.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NativeString extends NativeDataType {

    private final NativeByte[] bytes;

    public NativeString(final ByteBuffer byteBuffer, final int offset, final int length) {
        super(byteBuffer, offset);
        bytes = new NativeByte[length];

        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = new NativeByte(byteBuffer, offset + i);
        }
    }

    public void set(final String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);

        for(int i = 0; i < size(); i++) {
            if(i < valueBytes.length) {
                bytes[i].set(valueBytes[i]);
            } else {
                bytes[i].set((byte) 0);
            }
        }
    }

    public String get() {
        byte[] ret = new byte[size()];

        for(int i = 0; i < size(); i++) {
            ret[i] = bytes[i].get();
        }

        return new String(ret, 0, length(), StandardCharsets.US_ASCII);
    }

    public int size() {
        return bytes.length;
    }

    private int length() {
        for(int i = 0; i < size(); i++) {
            if (bytes[i].get() == 0) {
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
