package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Component
@CommandLine.Command(
        name = "rsocket",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class RSocketDemo implements Runnable {

    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final int DEFAULT_MTU = 4096;
    private static final long MICRO_SECOND = 1000000;

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
        if (isServer) {
            startServer();
        } else {
            startClient();
        }
    }

    private void startClient() {
        // Create a socket to exchange queue pair information with the server
        try (var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort())) {

            // Create RSocket infiniband client transport using neutrino
            var transport = new InfinibandClientTransport(networkService, address -> connect(socket, address));

            // Configure and create a RSocket passing it the infiniband transport and connect to the server
            RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> new EmptyMessageHandler())
                    .transport(transport)
                    .start()
                    .flatMapMany(rsocket -> {
                        // Create a payload (once) and send it (multiple times) to the server
                        var payload = DefaultPayload.create("ping");
                        return rsocket.requestChannel(Flux.just(payload).repeat())
                                .doOnNext(Payload::release);
                    })
                    .window(Duration.ofSeconds(1))
                    .flatMap(Flux::count)
                    .subscribe(count -> {
                        if (count != 0) {
                            log.info("Received {} messages ({} us/m)", count, MICRO_SECOND/count);
                        }
                    });

            LockSupport.parkNanos(Duration.ofSeconds(600).toNanos());
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
    }

    private void startServer() {
        log.info("Waiting for incoming connections on port {}", port);

        // Create a server socket to exchange queue pair information with the client
        try (var serverSocket = new ServerSocket(port);
             var socket = serverSocket.accept()) {

            // Create RSocket infiniband server transport using neutrino
            var transport = new InfinibandServerTransport(networkService, address -> connect(socket, address));

            // Configure and create an RSocket passing it the infiniband transport
            var rsocket = RSocketFactory.receive()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor((setup, reactiveSocket) ->  Mono.just(new MessageHandler()))
                    .transport(transport)
                    .start()
                    .subscribe();

            // Block the main thread
            LockSupport.parkNanos(Duration.ofSeconds(600).toNanos());
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
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

    private static class MessageHandler extends AbstractRSocket {

        @Override
        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            // Relay back all received payloads to their sender
            return Flux.from(payloads);
        }
    }

    private static class EmptyMessageHandler extends AbstractRSocket {}
}
