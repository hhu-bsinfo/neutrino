package de.hhu.bsinfo.neutrino.example.command;

import com.google.protobuf.ByteString;
import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.example.service.*;
import de.hhu.bsinfo.neutrino.example.util.Result;
import io.netty.buffer.ByteBuf;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.rpc.rsocket.RequestHandlingRSocket;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(
        name = "grpc",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class GrpcDemo implements Runnable {

    private static final int DEFAULT_SERVER_PORT = 2998;

    private static final int DEFAULT_MTU = 4096;

    private static final long DEFAULT_MESSAGE_COUNT = 50_000_000;

    private static final int DEFAULT_MESSAGE_SIZE = 64;

    private static final int DEFAULT_CONNECTION_COUNT = 1;

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
            names = {"-m", "--messages"},
            description = "The number of messages.")
    private long messageCount = DEFAULT_MESSAGE_COUNT;

    @CommandLine.Option(
            names = {"-b", "--bytes"},
            description = "The number of bytes per message.")
    private int messageSize = DEFAULT_MESSAGE_SIZE;

    @CommandLine.Option(
            names = {"--connections"},
            description = "The number of connections.")
    private long connectionCount = DEFAULT_CONNECTION_COUNT;

    @Autowired
    private Neutrino neutrino;

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

            // Create gRPC service server
            var echoServer = new EchoServiceServer(new EchoServiceImpl(), Optional.empty(), Optional.empty());

            // Create message
            final var data = new byte[messageSize];
            ThreadLocalRandom.current().nextBytes(data);
            final var message = BinaryMessage.newBuilder().setData(ByteString.copyFrom(data)).build();
            final var actualSize = message.getSerializedSize();
            final var messageFlux = Flux.just(message).repeat(messageCount);

            var startTime = new AtomicLong();

            // Configure and create a RSocket passing it the infiniband transport and connect to the server
            RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> new RequestHandlingRSocket(echoServer))
                    .transport(transport)
                    .start()
                    .doOnNext(ignored -> log.info("Client connected"))
                    .map(EchoServiceClient::new)
                    .flatMapMany(client -> messageFlux.flatMap(client::binaryFireAndForget))
                    .doFirst(() -> startTime.set(System.nanoTime()))
                    .blockLast();

            var result = new Result(messageCount, actualSize, System.nanoTime() - startTime.get());

            System.out.println();
            System.out.println();
            System.out.println(result);
            System.out.println();

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

            // Create gRPC service server
            var echoServer = new EchoServiceServer(new EchoServiceImpl(), Optional.empty(), Optional.empty());

            // Configure and create an RSocket passing it the infiniband transport
            var server = RSocketFactory.receive()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor((setup, reactiveSocket) -> {
                        log.info("Client connection accepted");
                        return Mono.just(new RequestHandlingRSocket(echoServer));
                    })
                    .transport(transport)
                    .start()
                    .block();

            server.onClose().block();
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
    }

    private static final class EchoServiceImpl implements EchoService {

        private static final Mono<Empty> EMPTY_MONO = Mono.just(Empty.getDefaultInstance()).cache();

        @Override
        public Mono<Empty> fireAndForget(SimpleMessage message, ByteBuf metadata) {
            return EMPTY_MONO;
        }

        @Override
        public Mono<SimpleMessage> requestReply(SimpleMessage message, ByteBuf metadata) {
            return Mono.just(message);
        }

        @Override
        public Flux<SimpleMessage> requestStream(SimpleMessage message, ByteBuf metadata) {
            return Flux.just(message).repeat();
        }

        @Override
        public Mono<SimpleMessage> streamingRequestSingleResponse(Publisher<SimpleMessage> messages, ByteBuf metadata) {
            return Flux.from(messages)
                    .map(SimpleMessage::getContent)
                    .collect(Collectors.joining(", ", "[", "]"))
                    .map(it -> SimpleMessage.newBuilder().setContent(it).build());
        }

        @Override
        public Flux<SimpleMessage> streamingRequestAndResponse(Publisher<SimpleMessage> messages, ByteBuf metadata) {
            return Flux.from(messages);
        }

        @Override
        public Mono<BinaryMessage> binaryRequestReply(BinaryMessage message, ByteBuf metadata) {
            return Mono.empty();
        }

        @Override
        public Mono<Empty> binaryFireAndForget(BinaryMessage message, ByteBuf metadata) {
            log.info("Received message");
            return EMPTY_MONO;
        }

        @Override
        public Flux<BinaryMessage> binaryRequestStream(BinaryMessage message, ByteBuf metadata) {
            return Flux.empty();
        }

        @Override
        public Mono<BinaryMessage> binaryStreamingRequestSingleResponse(Publisher<BinaryMessage> messages, ByteBuf metadata) {
            return Mono.empty();
        }

        @Override
        public Flux<BinaryMessage> binaryStreamingRequestAndResponse(Publisher<BinaryMessage> messages, ByteBuf metadata) {
            return Flux.empty();
        }
    }

    private static final class MemoryServiceImpl implements MemoryService {

        private final Neutrino neutrino;

        private MemoryServiceImpl(Neutrino neutrino) {
            this.neutrino = neutrino;
        }

        @Override
        public Mono<RemoteHandle> allocateBuffer(BufferRequest message, ByteBuf metadata) {
            return neutrino.allocate(message.getCapacity())
                    .map(buffer -> RemoteHandle.newBuilder()
                                        .setAddress(buffer.memoryAddress())
                                        .setCapacity(buffer.capacity())
                                        .setKey(buffer.getRemoteKey())
                                        .build());
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
}
