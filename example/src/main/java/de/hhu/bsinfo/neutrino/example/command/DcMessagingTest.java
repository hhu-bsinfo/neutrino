package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.verbs.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "dc-msg-test",
        description = "Starts a simple InfiniBand messaging test with dynamic connections, using the neutrino core.%n",
        showDefaultValues = true,
        separator = " ")
public class DcMessagingTest implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcMessagingTest.class);

    @Override
    public Void call() throws Exception {
        int numDevices = Context.getDeviceCount();

        if(numDevices < 0) {
            LOGGER.error("No InfiniBand devices were found in your system");
            return null;
        }

        var context = Context.openDevice(0);

        if(!context.mlx5IsSupported()) {
            LOGGER.error("Your device does not support mlx5 direct verbs!");
            return null;
        }

        return null;
    }
}
