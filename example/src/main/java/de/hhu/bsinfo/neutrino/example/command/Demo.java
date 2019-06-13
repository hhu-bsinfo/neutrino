package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "demo",
        description = "Starts the neutrino high-level api demo%n",
        showDefaultValues = true,
        separator = " ")
public class Demo implements Callable<Void> {

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    private final Neutrino neutrino = Neutrino.newInstance();

    @Override
    public Void call() throws Exception {
        return null;
    }
}
