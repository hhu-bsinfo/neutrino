package de.hhu.bsinfo.neutrino.example.command;


import picocli.CommandLine;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { Start.class, Demo.class }
)
public class Root implements Runnable{

    @Override
    public void run() {}
}