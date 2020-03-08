package de.hhu.bsinfo.neutrino.example.command.rsocket;

import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.example.util.Result;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@CommandLine.Command(
        name = "rsocket",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class MessagingDemo extends RSocketDemo {

    private static final int DEFAULT_MESSAGE_SIZE = 64;

    private static final long DEFAULT_MESSAGE_COUNT = 50_000_000;

    @CommandLine.Option(
            names = {"-b", "--bytes"},
            description = "The number of bytes per message.")
    private int messageSize = DEFAULT_MESSAGE_SIZE;

    @CommandLine.Option(
            names = {"-m", "--messages"},
            description = "The number of messages.")
    private long messageCount = DEFAULT_MESSAGE_COUNT;

    @Override
    protected void onClientReady(InfinibandSocket infinibandSocket) {

        // Create payload
        final var data = new byte[messageSize];
        ThreadLocalRandom.current().nextBytes(data);
        final var payload = DefaultPayload.create(data);

        var startTime = new AtomicLong();
        Flux.just(payload)
                .repeat(messageCount - 1)
                .flatMap(infinibandSocket::fireAndForget)
                .doFirst(() -> startTime.set(System.nanoTime()))
                .blockLast();

        var result = new Result(messageCount, messageSize, System.nanoTime() - startTime.get());

        log.info("\n\n{}\n\n", result);
    }

    @Override
    protected AbstractRSocket getClientHandler() {
        return new EmptyMessageHandler();
    }

    @Override
    protected AbstractRSocket getServerHandler() {
        return new MessageHandler();
    }

    private static class MessageHandler extends AbstractRSocket {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public Mono<Void> fireAndForget(Payload payload) {
            counter.getAndIncrement();
            log.info("Received payload with size {} [{}]", payload.data().readableBytes(), counter.get());
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
