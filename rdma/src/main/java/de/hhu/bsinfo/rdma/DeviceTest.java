package de.hhu.bsinfo.rdma;

import de.hhu.bsinfo.rdma.verbs.Context;
import de.hhu.bsinfo.rdma.verbs.Device;
import de.hhu.bsinfo.rdma.verbs.MemoryRegion.AccessFlag;
import de.hhu.bsinfo.rdma.verbs.Port;
import de.hhu.bsinfo.rdma.verbs.Port.PortState;
import de.hhu.bsinfo.rdma.verbs.Verbs;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class DeviceTest {

    public static void main(String... args) {

        int numDevices = Device.getDeviceCount();

        if(numDevices <= 0) {
            System.out.println("No RDMA devices were found in your system!");
            return;
        }

        var context = Context.openDevice(0);

        System.out.println("Opened context for device '" + context.getDeviceName() + "'!");

        var device = context.queryDevice();

        System.out.println(device);

        var port = context.queryPort(1);

        System.out.println(port);

        var protectionDomain = context.allocateProtectionDomain();

        System.out.println("Allocated protection domain!");

        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        var memoryRegion = protectionDomain.registerMemoryRegion(buffer, AccessFlag.IBV_ACCESS_LOCAL_WRITE, AccessFlag.IBV_ACCESS_REMOTE_READ, AccessFlag.IBV_ACCESS_REMOTE_WRITE);

        System.out.println("Registered memory region!");

        if(memoryRegion.deregister()) {
            System.out.println("Deregistered memory region!");
        }

        if(protectionDomain.deallocate()) {
            System.out.println("Deallocated protection domain!");
        }

        if(context.close()) {
            System.out.println("Closed context!");
        }
    }

}
