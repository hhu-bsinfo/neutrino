package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.example.command.panama.PanamaDemo;
import picocli.CommandLine;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = {
        DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
        ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class,
        CommunicationManagerDemo.class, PanamaDemo.class
    }
)
public class Root implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}