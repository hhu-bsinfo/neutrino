package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.Device;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static final int BUFFER_SIZE = 1024;

    private static final int COMPLETIONQUEUE_SIZE = 100;

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
        var memoryRegion = protectionDomain.registerMemoryRegion(buffer, AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_READ, AccessFlag.REMOTE_WRITE);

        LOGGER.info("Registered memory region!");

        var completionQueue = context.createCompletionQueue(COMPLETIONQUEUE_SIZE);

        LOGGER.info("Created completion queue");

        if (completionQueue.destroy()) {
            LOGGER.info("Destroyed completion queue");
        }

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
