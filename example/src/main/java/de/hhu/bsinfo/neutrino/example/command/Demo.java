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

    private static final int DEMO_BUFFER_SIZE = 1024;
    private static final int DEMO_PORT = 2998;

    private static final InetSocketAddress DEMO_BIND_ADDR = new InetSocketAddress(DEMO_PORT);

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    private Neutrino neutrino;

    @Override
    public Void call() throws Exception {
        return null;
    }
}
