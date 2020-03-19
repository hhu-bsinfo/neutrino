package de.hhu.bsinfo.neutrino.example.command.rsocket;

import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import io.rsocket.AbstractRSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public abstract class RSocketDemo implements Runnable {

    private static final int DEFAULT_MTU = 4096;

    private static final int DEFAULT_SERVER_PORT = 2998;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    @CommandLine.Option(
            names = {"-p", "--port"},
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

        // Create a socket to exchange queue pair information with the server
        try (var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort())) {

            log.info("Connected with {}", socket.getInetAddress());

            // Create RSocket infiniband client transport using neutrino
            var transport = new InfinibandClientTransport(networkService, address -> connect(socket, address));

            var infinibandSocket = RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> getClientHandler())
                    .transport(transport)
                    .start()
                    .map(rsocket -> InfinibandSocket.create(rsocket, transport.getConnection(), networkService))
                    .block();

            onClientReady(infinibandSocket);
        } catch (Throwable e) {
            log.error("An unexpected error occured", e);
        }

        log.info("Client exit");
    }

    private void startServer() {
        log.info("Waiting for incoming connections on port {}", port);

        // Create a server socket to exchange queue pair information with the client
        try (var serverSocket = new ServerSocket(port);
             var socket = serverSocket.accept()) {

            log.info("Accepted new connection from {}", socket.getInetAddress());

            // Create RSocket infiniband server transport using neutrino
            var transport = new InfinibandServerTransport(networkService, address -> connect(socket, address));

            // Configure and create an RSocket passing it the infiniband transport
            var server = RSocketFactory.receive()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor((setup, reactiveSocket) ->  Mono.just(getServerHandler()))
                    .transport(transport)
                    .start()
                    .block();

            server.onClose().block();
        } catch (Throwable e) {
            log.error("An unexpected error occured", e);
        }

        log.info("Server exit");
    }

    private static QueuePairAddress connect(Socket socket, QueuePairAddress local) {
        // Create input and output streams to exchange queue pair information
        try (var out = new ObjectOutputStream(socket.getOutputStream());
             var in = new ObjectInputStream(socket.getInputStream())) {

            // Send local queue pair information
            out.writeObject(local);

            // Receive remote queue pair information
            return (QueuePairAddress) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    protected abstract AbstractRSocket getClientHandler();

    protected abstract AbstractRSocket getServerHandler();

    protected abstract void onClientReady(InfinibandSocket socket);

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
