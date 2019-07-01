package de.hhu.bsinfo.neutrino.example.command;

import picocli.CommandLine;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { MessagingTest.class, RdmaTest.class }
)
public class Root implements Runnable {

    @Override
    public void run() {}
}