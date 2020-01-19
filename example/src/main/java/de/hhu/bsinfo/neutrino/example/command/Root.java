package de.hhu.bsinfo.neutrino.example.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
            ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class, RSocketDemo.class, GrpcDemo.class }
)
public class Root implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}