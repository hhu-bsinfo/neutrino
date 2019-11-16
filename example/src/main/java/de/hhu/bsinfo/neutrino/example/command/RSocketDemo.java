package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.example.service.*;
import io.netty.buffer.ByteBuf;
import io.rsocket.*;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.rpc.rsocket.RequestHandlingRSocket;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(
        name = "rsocket",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class RSocketDemo implements Runnable {

    private static final byte PORT_NUMBER = 1;
    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final Duration SEND_INTERVAL = Duration.ofMillis(200);

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

    private static final QueuePairAddress DUMMY_ADDRESS = QueuePairAddress.builder().build();

    private final Neutrino neutrino = Neutrino.newInstance();
    private final CoreService coreService = neutrino.getService(CoreService.class);
    private final ConnectionService connectionService = neutrino.getService(ConnectionService.class);

    @Override
    public void run() {
        if (isServer) {
            startServer();
        } else {
            startClient();
        }
    }

    private void startClient() {
        // TODO(krakowski): Implement connection via URI string
        //  var clientTransport = UriTransportRegistry.clientForUri(connectionString);

        log.info("Connecting to {}", serverAddress);
        try (var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort())) {
            var connection = connectionService.newConnection().block();
            var remoteAddress = connect(socket, connection);
            var transport = InfinibandClientTransport.create(connection, remoteAddress, neutrino);
            var serviceServer = new EchoServiceServer(new EchoServiceImpl(), Optional.empty(), Optional.empty());
            var rsocket = RSocketFactory.connect()
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> new RequestHandlingRSocket(serviceServer))
                    .transport(transport)
                    .start()
                    .block();

            var client = new EchoServiceClient(rsocket);
            Flux.interval(SEND_INTERVAL)
                .map(second -> SimpleMessage.newBuilder().setContent("Hello Infiniworld! (" + second + ")").build())
                .compose(client::streamingRequestAndResponse)
                .doOnNext(response -> log.info("Received echo message \"{}\"", response.getContent()))
                .blockLast();
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
    }

    private void startServer() {
        log.info("Waiting for incoming connections on port {}", port);
        try (var serverSocket = new ServerSocket(port);
             var socket = serverSocket.accept()) {

            var connection = connectionService.newConnection().block();
            var remoteAddress = connect(socket, connection);
            var transport = InfinibandServerTransport.create(connection, remoteAddress, neutrino);
            var serviceServer = new EchoServiceServer(new EchoServiceImpl(), Optional.empty(), Optional.empty());
            var rsocket = RSocketFactory.receive()
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor((setup, reactiveSocket) -> Mono.just(new RequestHandlingRSocket(serviceServer)))
                    .transport(transport)
                    .start()
                    .block();

            rsocket.onClose().block();
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
    }

    private static final class EchoServiceImpl implements EchoService {

        @Override
        public Mono<Empty> fireAndForget(SimpleMessage message, ByteBuf metadata) {
            log.info("[fireAndForget] received message \"{}\"", message.getContent());
            return Mono.just(Empty.getDefaultInstance());
        }

        @Override
        public Mono<SimpleMessage> requestReply(SimpleMessage message, ByteBuf metadata) {
            log.info("[requestReply] received message \"{}\"", message.getContent());
            return Mono.just(message);
        }

        @Override
        public Flux<SimpleMessage> requestStream(SimpleMessage message, ByteBuf metadata) {
            log.info("[requestStream] received message \"{}\"", message.getContent());
            return Flux.interval(SEND_INTERVAL)
                    .map(it -> message);
        }

        @Override
        public Mono<SimpleMessage> streamingRequestSingleResponse(Publisher<SimpleMessage> messages, ByteBuf metadata) {
            return Flux.from(messages)
                    .doOnNext(it ->  log.info("[streamingRequestSingleResponse] received message \"{}\"", it.getContent()))
                    .map(SimpleMessage::getContent)
                    .collect(Collectors.joining(", ", "[", "]"))
                    .map(it -> SimpleMessage.newBuilder().setContent(it).build());
        }

        @Override
        public Flux<SimpleMessage> streamingRequestAndResponse(Publisher<SimpleMessage> messages, ByteBuf metadata) {
            return Flux.from(messages)
                    .doOnNext(it ->  log.info("[streamingRequestAndResponse] received message \"{}\"", it.getContent()));
        }
    }

    private QueuePairAddress connect(Socket socket, Connection connection) {
        var queuePair = connection.getQueuePair();
        var localInfo = QueuePairAddress.builder()
                .localId(coreService.getLocalId())
                .queuePairNumber(queuePair.getQueuePairNumber())
                .portNumber(PORT_NUMBER)
                .build();

        try (var out = new ObjectOutputStream(socket.getOutputStream());
             var in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(localInfo);
            return (QueuePairAddress) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
