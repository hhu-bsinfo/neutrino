package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import picocli.CommandLine;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
            ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class, RSocketDemo.class}
)
public class Root implements Runnable {

    @Override
    public void run() {
        Neutrino.printBanner();
    }
}