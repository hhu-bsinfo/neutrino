package de.hhu.bsinfo.neutrino.example.command.bench;

import de.hhu.bsinfo.neutrino.api.network.InfinibandChannel;
import de.hhu.bsinfo.neutrino.api.network.NetworkHandler;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.DefaultNegotiator;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class BenchmarkDemo implements Runnable, NetworkHandler {

    private static final int DEFAULT_MTU = 4096;

    private static final int DEFAULT_SERVER_PORT = 2998;

    private static final int DEFAULT_CONNECTION_COUNT = 1;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = "--connections",
            description = "The number of connections to establish.")
    private int connections = DEFAULT_CONNECTION_COUNT;

    @CommandLine.Option(
            names = "--connect",
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    @CommandLine.Option(
            names = "--port",
            description = "The port the server will listen on.")
    private int port = DEFAULT_SERVER_PORT;

    @Autowired
    private NetworkService networkService;

    @Override
    public void run() {
        onStart();
        if (isServer) {
            startServer();
        } else {
            startClient();
        }
    }

    private void startClient() {

        // Create the requested number of connections
        var clientList = Stream.generate(this::createClient)
                .limit(connections)
                .collect(Collectors.toList());

        // Pass connections to demo implementation
        onClientReady(clientList);
    }

    private InfinibandChannel createClient() {
        // Create a socket to exchange queue pair information with the server
        try (var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort())) {
            log.info("Connected with {}", socket.getInetAddress());
            return networkService.connect(DefaultNegotiator.fromSocket(socket), this, Mtu.MTU_4096);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void startServer() {
        log.info("Waiting for incoming connections on port {}", port);

        // Create a server socket to exchange queue pair information with the client
        try (var serverSocket = new ServerSocket(port);) {
            receive(serverSocket);
        } catch (Throwable e) {
            log.error("An unexpected error occured", e);
        }

        log.info("Server exit");
    }

    private void receive(ServerSocket serverSocket) throws IOException {
        var connections = new ArrayList<InfinibandChannel>();

        while (true) {
            // Accept new client connection
            var socket = serverSocket.accept();

            // Connect with the client's queue pair
            var connection = networkService.connect(DefaultNegotiator.fromSocket(socket), this, Mtu.MTU_4096);
            connections.add(connection);
        }
    }

    protected abstract void onClientReady(List<InfinibandChannel> sockets);

    protected void onStart() {
        // no-op
    }

    protected final boolean isServer() {
        return isServer;
    }

    protected final boolean isClient() {
        return !isServer;
    }
}
