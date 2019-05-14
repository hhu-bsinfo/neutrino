package de.hhu.bsinfo.rdma.verbs;

public class Verbs {

    static {
        System.loadLibrary("rdma");
    }

    public static native int getNumDevices();
    public static native void openDevice(int index, long resultHandle);
    public static native void queryDevice(long contextHandle, long deviceHandle, long resultHandle);
}
