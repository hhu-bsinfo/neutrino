package de.hhu.bsinfo.rdma.util;

import de.hhu.bsinfo.rdma.data.Result;
import de.hhu.bsinfo.rdma.data.StructInformation;

public class StructUtil {

    static {
        System.loadLibrary("rdma");
    }

    public static native void getDeviceAttributes(long resultHandle);

    public static StructInformation getDeviceAttribtues() {
        Result result = new Result();
        getDeviceAttributes(result.getHandle());
        return new StructInformation(result.resultHandle.get());
    }

}
