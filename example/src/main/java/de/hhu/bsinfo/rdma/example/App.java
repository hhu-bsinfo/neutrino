package de.hhu.bsinfo.rdma.example;

import de.hhu.bsinfo.rdma.verbs.Context;
import de.hhu.bsinfo.rdma.verbs.Device;
import de.hhu.bsinfo.rdma.verbs.MemoryRegion.AccessFlag;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static final int BUFFER_SIZE = 1024;

    public static void main(String... args) {

        int numDevices = Device.getDeviceCount();

        if(numDevices <= 0) {
            LOGGER.error("No RDMA devices were found in your system!");
            return;
        }

        var context = Context.openDevice(0);

        LOGGER.info("Opened context for device {}!", context.getDeviceName());

        var device = context.queryDevice();

        LOGGER.info(device.toString());

        var port = context.queryPort(1);

        LOGGER.info(port.toString());

        var protectionDomain = context.allocateProtectionDomain();

        LOGGER.info("Allocated protection domain!");

        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        var memoryRegion = protectionDomain.registerMemoryRegion(buffer, AccessFlag.IBV_ACCESS_LOCAL_WRITE, AccessFlag.IBV_ACCESS_REMOTE_READ, AccessFlag.IBV_ACCESS_REMOTE_WRITE);

        LOGGER.info("Registered memory region!");

        if(memoryRegion.deregister()) {
            LOGGER.info("Deregistered memory region!");
        }

        if(protectionDomain.deallocate()) {
            LOGGER.info("Deallocated protection domain!");
        }

        if(context.close()) {
            LOGGER.info("Closed context!");
        }
    }

}
