package de.hhu.bsinfo.neutrino.example.command.rsocket;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.network.LocalHandle;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.example.util.Result;
import io.netty.buffer.Unpooled;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@CommandLine.Command(
        name = "read",
        description = "Demonstrates neutrino's rdma read operation.%n",
        showDefaultValues = true,
        separator = " ")
public class ReadDemo extends RSocketDemo {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final int BUFFER_SIZE = 1024;

    @Autowired
    private Neutrino neutrino;

    @Override
    protected void onClientReady(InfinibandSocket infinibandSocket) {

        // The String we expect to read
        var expected = "Hello Infiniworld!";

        // Request buffer information from server
        var payload = DefaultPayload.create(expected, CHARSET);
        var response = infinibandSocket.requestResponse(payload).block();

        // Process response
        var data = response.sliceData();
        var remoteHandle = new RemoteHandle(data.readLong(), data.readInt());
        data.release();
        response.release();

        log.info("Reading from remote at virtual address 0x{} with remote key 0x{}",
                Long.toHexString(remoteHandle.getAddress()), remoteHandle.getKey());

        // Allocate local buffer and read data from remote
        var buffer = Objects.requireNonNull(neutrino.allocate(BUFFER_SIZE).block());
        var localHandle = new LocalHandle(buffer.memoryAddress() + buffer.writerIndex(), buffer.writableBytes(), buffer.localKey());
        infinibandSocket.read(localHandle, remoteHandle).block();

        log.info("Waiting for read operation to finish");
        while (buffer.getByte(0) == 0) {
            // Wait for data to be read
        }

        // Print data received through read operation
        var actual = buffer.getCharSequence(0, expected.length(), CHARSET);
        log.info("Expected : {}  |  Actual : {}", expected, actual);

        // Wait until connection is closed
        infinibandSocket.dispose();
        infinibandSocket.onClose().block();
    }

    @Override
    protected AbstractRSocket getClientHandler() {
        return new AbstractRSocket() {
            // Client does not handle requests, so we do not
            // need to implement any methods
        };
    }

    @Override
    protected AbstractRSocket getServerHandler() {
        return new AbstractRSocket() {

            private final Buffer buffer = neutrino.allocate(BUFFER_SIZE).block();

            @Override
            public Mono<Payload> requestResponse(Payload payload) {

                // Get data from request
                var data = payload.getDataUtf8();
                payload.release();

                // Copy data into our buffer
                buffer.writeCharSequence(data, CHARSET);
                log.info("Written \"{}\" into local buffer", data);

                // Create response
                var response = Unpooled.directBuffer()
                        .writeLong(buffer.memoryAddress())
                        .writeInt(buffer.remoteKey());

                log.info("Sending response with address 0x{} and remote key 0x{}",
                        Long.toHexString(buffer.memoryAddress()), Long.toHexString(buffer.remoteKey()));

                return Mono.just(DefaultPayload.create(response));
            }
        };
    }
}
