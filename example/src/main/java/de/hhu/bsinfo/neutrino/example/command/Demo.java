package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "demo",
        description = "Starts the neutrino high-level api demo%n",
        showDefaultValues = true,
        separator = " ")
public class Demo implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

    private static final int DEFAULT_SERVER_PORT = 2998;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = "--bind",
            description = "The servers bind address.")
    private InetSocketAddress bindAddress = new InetSocketAddress(DEFAULT_SERVER_PORT);

    @CommandLine.Option(
            names = "--connect",
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    private final Neutrino neutrino = Neutrino.newInstance();

    @Override
    public Void call() {

        var connectionService = neutrino.getService(ConnectionService.class);

        if (isServer) {
            connectionService.listen(bindAddress)
                    .subscribe(connection -> LOGGER.info(connection.toString()));
        } else {
            connectionService.connect(serverAddress)
                    .subscribe(connection -> LOGGER.info(connection.toString()));
        }

        return null;
    }
}
