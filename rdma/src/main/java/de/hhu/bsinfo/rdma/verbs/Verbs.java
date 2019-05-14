package de.hhu.bsinfo.rdma.verbs;

public class Verbs {

    static {
        System.loadLibrary("rdma");
    }

    static native int getNumDevices();
    static native void openDevice(int index, long resultHandle);
    static native void queryDevice(long contextHandle, long deviceHandle, long resultHandle);
    static native void queryPort(long contextHandle, long portHandle, int portNumber, long resultHandle);
    static native void allocateProtectionDomain(long contextHandle, long resultHandle);
    static native void deallocateProtectionDomain(long protectionDomainHandle, long resultHandle);
}
