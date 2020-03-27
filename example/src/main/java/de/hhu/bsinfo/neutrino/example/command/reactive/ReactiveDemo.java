package de.hhu.bsinfo.neutrino.example.command.reactive;

import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.DefaultNegotiator;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBuf;
import io.rsocket.AbstractRSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class ReactiveDemo implements Runnable {

    public enum MessageMode {
        RSOCKET, DIRECT
    }

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

    private InfinibandSocket createClient() {

        // Create a socket to exchange queue pair information with the server
        try (var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort())) {

            log.info("Connected with {}", socket.getInetAddress());

            // Create RSocket infiniband client transport using neutrino
            var transport = new InfinibandClientTransport(networkService, DefaultNegotiator.fromSocket(socket));

            return RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> getClientHandler())
                    .transport(transport)
                    .start()
                    .map(rsocket -> InfinibandSocket.create(rsocket, transport.getConnection(), networkService))
                    .block();

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void startServer() {
        log.info("Waiting for incoming connections on port {}", port);

        // Create a server socket to exchange queue pair information with the client
        try (var serverSocket = new ServerSocket(port);) {

            log.info("Receiving messages using mode {}", getMessageMode());

            switch (getMessageMode()) {
                case RSOCKET:
                    rsocketReceive(serverSocket);
                    break;
                case DIRECT:
                    directReceive(serverSocket);
                    break;
            }
        } catch (Throwable e) {
            log.error("An unexpected error occured", e);
        }

        log.info("Server exit");
    }

    private void directReceive(ServerSocket serverSocket) throws IOException {

        var disposables = new ArrayList<Disposable>();

        while (true) {

            // Accept new client connection
            var socket = serverSocket.accept();

            // Establish connection using neutrino and receive buffers
            var disposable = networkService.connect(DefaultNegotiator.fromSocket(socket), Mtu.MTU_4096)
                    .flatMapMany(connection -> networkService.receive(connection))
                    .doOnNext(ByteBuf::release)
                    .subscribe();

            disposables.add(disposable);
        }
    }

    private void rsocketReceive(ServerSocket socket) {

        // Create RSocket infiniband server transport using neutrino
        var transport = new InfinibandServerTransport(networkService, socket);

        // Configure and create an RSocket passing it the infiniband transport
        var server = RSocketFactory.receive()
                .fragment(DEFAULT_MTU)
                .frameDecoder(PayloadDecoder.ZERO_COPY)
                .acceptor((setup, reactiveSocket) ->  Mono.just(getServerHandler()))
                .transport(transport)
                .start()
                .block();

        // Wait until server is closed
        Objects.requireNonNull(server).onClose().block();
    }

    protected abstract AbstractRSocket getClientHandler();

    protected abstract AbstractRSocket getServerHandler();

    protected abstract void onClientReady(List<InfinibandSocket> sockets);

    protected abstract MessageMode getMessageMode();

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
