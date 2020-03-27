package de.hhu.bsinfo.neutrino.example.command.arrow;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.example.command.reactive.ReactiveDemo;
import de.hhu.bsinfo.neutrino.example.proto.FieldMeta;
import de.hhu.bsinfo.neutrino.example.proto.VectorMeta;
import de.hhu.bsinfo.neutrino.example.util.PhoneBook;
import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.BaseAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@CommandLine.Command(
        name = "arrow",
        description = "Demonstrates the interaction between Apache Arrow and Neutrino.%n",
        showDefaultValues = true,
        separator = " ")
public class ArrowDemo extends ReactiveDemo {

    private static final int DEFAULT_ROW_COUNT = 100000;

    private static final int DEFAULT_WARMUP = 1000;

    private static final int DEFAULT_ITERATIONS = 1000;

    @CommandLine.Option(
            names = {"-r", "--rows"},
            description = "The number of rows to transfer.")
    private int rows = DEFAULT_ROW_COUNT;

    @CommandLine.Option(
            names = {"-i", "--iteration"},
            description = "The number of iterations to perform.")
    private int iteration = DEFAULT_ITERATIONS;

    @CommandLine.Option(
            names = {"-w", "--warmup"},
            description = "The number of warmups to perform.")
    private int warmup = DEFAULT_WARMUP;

    private final BaseAllocator allocator = new RootAllocator(Integer.MAX_VALUE);

    private PhoneBook book;

    @Autowired
    private InfinibandDevice device;

    @Override
    protected void onStart() {
        super.onStart();

        // Client does not need to allocate resources
        if (isClient()) {
            return;
        }

        // Create a phone book with the specified number of rows (entries)
        log.info("Allocating Arrow data structure. Please wait!");
        book = PhoneBook.create(allocator, rows);
        log.info("Allocated Arrow data structure. Please start client now!");
    }

    @Override
    protected AbstractRSocket getClientHandler() {
        return new ClientHandler();
    }

    @Override
    protected AbstractRSocket getServerHandler() {
        return new ServerHandler(this);
    }

    @Override
    protected void onClientReady(List<InfinibandSocket> sockets) {

        var socket = sockets.get(0);

        try {

            // Request info from server
            var request = Command.GET_INFO.toPayload();
            var response = Objects.requireNonNull(socket.requestResponse(request).block());
            var vectorMeta = VectorMeta.parseFrom(response.getData());

            // Release payloads
            request.release();
            response.release();

            // Start warmup
            log.info("Starting warmup phase");

            var tmp = PhoneBook.read(socket, vectorMeta, allocator)
                    .repeat(warmup - 1)
                    .blockLast();

            log.info("Ended warmup phase");


            // Start benchmark
            log.info("Starting benchmark phase");

            long startTime = System.nanoTime();

            var phoneBook = PhoneBook.read(socket, vectorMeta, allocator)
                    .repeat(iteration - 1)
                    .blockLast();

            long endTime = System.nanoTime();

            log.info("Ended benchmark phase");
            log.info("");

            // Print out first 10 lines
            Objects.requireNonNull(phoneBook)
                    .toString()
                    .lines()
                    .limit(10)
                    .forEach(log::info);

            // Print out results
            long size = Objects.requireNonNull(phoneBook).sizeInBytes();
            double microseconds = (endTime - startTime) / 1_000.0;
            double throughput = (((size * iteration) / microseconds) * 1_000_000) / (1024 * 1024 * 1024);

            log.info("");
            log.info("=====================================================");
            log.info("| Rows       : {}", vectorMeta.getRowCount());
            log.info("| Iterations : {}", iteration);
            log.info("| Warmups    : {}", warmup);
            log.info("| Size       : {} Bytes", size);
            log.info("| Time       : {} Microseconds", microseconds);
            log.info("| Throughput : {} GB/s", throughput);
            log.info("=====================================================");
            log.info("");

        } catch (IOException e) {
            log.error("Error", e);
        }
    }

    @Override
    protected ReactiveDemo.MessageMode getMessageMode() {
        return MessageMode.RSOCKET;
    }

    private static class ClientHandler extends AbstractRSocket {
        // Client will not receive requests
    }

    private static class ServerHandler extends AbstractRSocket {

        private final ArrowDemo demo;

        private ServerHandler(ArrowDemo demo) {
            this.demo = demo;
        }

        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public Mono<Payload> requestResponse(Payload payload) {

            // Resolve command sent by client
            var command = Command.resolve(payload.getMetadataUtf8());
            payload.release();

            // Execute command
            switch (command) {
                case GET_INFO:
                    log.info("Sending vector metadata to client");
                    return Mono.just(createVectorMeta());
                default:
                    log.warn("Received unknown command \"{}\"", command);
                    return Mono.empty();
            }
        }

        private Payload createVectorMeta() {
            var builder = VectorMeta.newBuilder();
            builder.setSchemaJson(demo.book.getSchema().toJson());
            builder.setRowCount(demo.book.getRowCount());
            demo.book.getVectors().stream()
                    .map(this::toMeta)
                    .forEach(meta -> builder.putFields(meta.getName(), meta));

            return DefaultPayload.create(builder.build().toByteArray());
        }

        private FieldMeta toMeta(FieldVector vector) {
            var device = demo.device;
            var validity = vector.getValidityBuffer();
            var offset = vector.getOffsetBuffer();
            var data = vector.getDataBuffer();

            // Register arrow buffers on-the-fly for RDMA read operations
            var validityRegion = device.wrapRegion(validity.memoryAddress(), validity.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
            var offsetRegion = device.wrapRegion(offset.memoryAddress(), offset.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
            var dataRegion = device.wrapRegion(data.memoryAddress(), data.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);

            // Build metadata for client
            return FieldMeta.newBuilder()
                    .setName(vector.getName())
                    .setValidityAddress(validityRegion.getAddress())
                    .setValidityLength(validityRegion.getLength())
                    .setValidityKey(validityRegion.getRemoteKey())
                    .setOffsetAddress(offsetRegion.getAddress())
                    .setOffsetLength(offsetRegion.getLength())
                    .setOffsetKey(offsetRegion.getRemoteKey())
                    .setDataAddress(dataRegion.getAddress())
                    .setDataLength(dataRegion.getLength())
                    .setDataKey(dataRegion.getRemoteKey())
                    .build();
        }
    }

    private enum Command {
        GET_INFO("get.info");

        private final String command;

        Command(String command) {
            this.command = command;
        }

        static Command resolve(String value) {
            return Arrays.stream(Command.values()).filter(it -> it.command.equals(value)).findFirst().orElseThrow();
        }

        Payload toPayload() {
            return DefaultPayload.create("", command);
        }
    }
}
