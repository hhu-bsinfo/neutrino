package de.hhu.bsinfo.rdma;

import de.hhu.bsinfo.rdma.verbs.Context;
import de.hhu.bsinfo.rdma.verbs.Device;
import de.hhu.bsinfo.rdma.verbs.Port;
import de.hhu.bsinfo.rdma.verbs.Verbs;

public class DeviceTest {

    public static void main(String... args) {
        int numDevices = Verbs.getNumDevices();

        if(numDevices <= 0) {
            System.out.println("No RDMA devices were found in your system!");
            return;
        }

        Context context = Context.openDevice(0);
        Device device = context.queryDevice();

        System.out.println(device);

        Port port = context.queryPort(1);

        System.out.println(port);
    }

}
