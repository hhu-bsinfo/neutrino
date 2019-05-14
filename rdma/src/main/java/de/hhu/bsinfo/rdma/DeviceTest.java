package de.hhu.bsinfo.rdma;

import de.hhu.bsinfo.rdma.data.Result;
import de.hhu.bsinfo.rdma.data.StructInformation;
import de.hhu.bsinfo.rdma.util.StructUtil;
import de.hhu.bsinfo.rdma.verbs.Context;
import de.hhu.bsinfo.rdma.verbs.Device;
import de.hhu.bsinfo.rdma.verbs.Verbs;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DeviceTest {

    public static void main(String... args) {

        var attributes = StructUtil.getDeviceAttribtues();

        int x = attributes.getOffset("fw_ver");



        Context context = Context.openDevice(0);
        Device device = context.queryDevice();

        System.out.println(device);
    }

}
