package de.hhu.bsinfo.neutrino.example.command;

import picocli.CommandLine;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class }
)
public class Root implements Runnable {

    @Override
    public void run() {}
}