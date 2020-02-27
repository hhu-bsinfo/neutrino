package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.example.util.Result;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Component
@CommandLine.Command(
        name = "rsocket",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class RSocketDemo implements Runnable {

    private static final int DEFAULT_MESSAGE_SIZE = 64;
    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final int DEFAULT_MTU = 4096;
    private static final long MICRO_SECOND = 1000000;

    private static final long DEFAULT_MESSAGE_COUNT = 50_000_000;

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

    @CommandLine.Option(
            names = {"-b", "--bytes"},
            description = "The number of bytes per message.")
    private int messageSize = DEFAULT_MESSAGE_SIZE;

    @CommandLine.Option(
            names = {"-m", "--messages"},
            description = "The number of messages.")
    private long messageCount = DEFAULT_MESSAGE_COUNT;

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

            log.info("Connected with {}", socket.getInetAddress());

            // Create RSocket infiniband client transport using neutrino
            var transport = new InfinibandClientTransport(networkService, address -> connect(socket, address));

            // Create payload
            final var data = new byte[messageSize];
            ThreadLocalRandom.current().nextBytes(data);
            final var payload = DefaultPayload.create(data);

            var startTime = new AtomicLong();

            var disposable = RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> new EmptyMessageHandler())
                    .transport(transport)
                    .start()
                    .map(rsocket -> InfinibandSocket.create(rsocket, transport.getConnection(), networkService))
                    .flatMapMany(rsocket -> Flux.just(payload).repeat(messageCount - 1).flatMap(rsocket::fireAndForget))
                    .doFirst(() -> startTime.set(System.nanoTime()))
                    .blockLast();

            var result = new Result(messageCount, messageSize, System.nanoTime() - startTime.get());

            System.out.println();
            System.out.println();
            System.out.println(result);
            System.out.println();

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
                    .acceptor((setup, reactiveSocket) ->  Mono.just(new MessageHandler()))
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

    private static class MessageHandler extends AbstractRSocket {

        @Override
        public Mono<Void> fireAndForget(Payload payload) {
            log.info("Received payload with size {}", payload.data().readableBytes());
            payload.release();
            return Mono.empty();
        }

        @Override
        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            // Relay back all received payloads to their sender
            return Flux.from(payloads);
        }
    }

    private static class EmptyMessageHandler extends AbstractRSocket {}
}
