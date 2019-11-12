package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.verbs.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "devices",
        description = "Scans the system for InfiniBand devices.%n",
        showDefaultValues = true,
        separator = " ")
public class DeviceInfo implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceInfo.class);

    @Override
    public Void call() throws Exception {
        int numDevices = Context.getDeviceCount();

        if(numDevices < 0) {
            LOGGER.error("No InfiniBand devices were found in your system");
            return null;
        }

        LOGGER.info("Found {} InfiniBand {} in your system", numDevices, numDevices == 1 ? "device" : "devices");

        for(int i = 0; i < numDevices; i++) {
            var context = Context.openDevice(0);

            if(context == null) {
                continue;
            }

            var device = context.queryDevice();

            if(device == null) {
                continue;
            }

            LOGGER.info("Device {}: {}\n{}", i, context.getDeviceName(), device);
            LOGGER.info("Mlx5 direct verbs support: {}", context.mlx5IsSupported());

            for(int j = 0; j < device.getPhysicalPortCount(); j++) {
                var port = context.queryPort(j + 1);

                if(port == null) {
                    continue;
                }

                LOGGER.info("Port {}:\n{}", j, port);
            }
        }

        return null;
    }
}
