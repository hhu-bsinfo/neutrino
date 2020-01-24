package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.example.service.*;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Optional;
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
    private static final long MICRO_SECOND = 1000000;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

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
            var memoryServer = new MemoryServiceServer(new MemoryServiceImpl(neutrino), Optional.empty(), Optional.empty());

            var localBuffer = neutrino.allocate(DEFAULT_BUFFER_SIZE);

            // Configure and create a RSocket passing it the infiniband transport and connect to the server
            RSocketFactory.connect()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor(rSocket -> new RequestHandlingRSocket(echoServer, memoryServer))
                    .transport(transport)
                    .start()
                    .doOnNext(ignored -> log.info("Client connected"))
                    .map(MemoryServiceClient::new)
                    .flatMap(client -> client.allocateBuffer(BufferRequest.newBuilder()
                                                                            .setCapacity(DEFAULT_BUFFER_SIZE)
                                                                            .build()))
                    .flatMap(remoteHandle -> localBuffer.flatMap(buffer -> neutrino.write(buffer, convert(null, remoteHandle))))
                    .subscribe(ignored -> {
                        log.info("Written data into remote buffer");
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

            // Create gRPC service server
            var echoServer = new EchoServiceServer(new EchoServiceImpl(), Optional.empty(), Optional.empty());
            var memoryServer = new MemoryServiceServer(new MemoryServiceImpl(neutrino), Optional.empty(), Optional.empty());

            // Configure and create an RSocket passing it the infiniband transport
            var rsocket = RSocketFactory.receive()
                    .fragment(DEFAULT_MTU)
                    .frameDecoder(PayloadDecoder.ZERO_COPY)
                    .acceptor((setup, reactiveSocket) -> {
                        log.info("Client connection accepted");
                        return Mono.just(new RequestHandlingRSocket(echoServer, memoryServer));
                    })
                    .transport(transport)
                    .start()
                    .subscribe();

            // Block the main thread
            LockSupport.parkNanos(Duration.ofSeconds(600).toNanos());
        } catch (IOException e) {
            log.error("An unexpected error occured", e);
        }
    }

    private static final class EchoServiceImpl implements EchoService {

        @Override
        public Mono<Empty> fireAndForget(SimpleMessage message, ByteBuf metadata) {
            return Mono.just(Empty.getDefaultInstance());
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

    private static de.hhu.bsinfo.neutrino.api.network.RemoteHandle convert(Connection connection, RemoteHandle handle) {
        return new de.hhu.bsinfo.neutrino.api.network.RemoteHandle() {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public long getAddress() {
                return handle.getAddress();
            }

            @Override
            public int getCapacity() {
                return handle.getCapacity();
            }

            @Override
            public int getRemoteKey() {
                return handle.getKey();
            }
        };
    }
}
