package de.hhu.bsinfo.rdma.data;

public class Result extends Struct {

    private static final int SIZE = Integer.BYTES + Long.BYTES;

    public final NativeInteger status = new NativeInteger(getByteBuffer(), 0);
    public final NativeLong resultHandle = new NativeLong(getByteBuffer(), 4);

    public Result() {
        super(SIZE);
    }

    public Result(long handle) {
        super(handle, SIZE);
    }

    public boolean isError() {
        return status.get() != 0;
    }
}
