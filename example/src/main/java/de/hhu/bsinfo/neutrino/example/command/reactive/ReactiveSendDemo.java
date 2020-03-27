package de.hhu.bsinfo.neutrino.example.command.reactive;

import de.hhu.bsinfo.neutrino.api.network.operation.DirectSendOperation;
import de.hhu.bsinfo.neutrino.api.network.operation.Operation;
import de.hhu.bsinfo.neutrino.example.util.Result;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.transport.neutrino.socket.InfinibandSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

@Slf4j
@Component
@CommandLine.Command(
        name = "send",
        description = "Demonstrates rsocket using neutrino as a transport.%n",
        showDefaultValues = true,
        separator = " ")
public class ReactiveSendDemo extends ReactiveDemo {

    private interface BenchmarkMethod {
        long execute(List<InfinibandSocket> sockets, byte[] data, long count);
    }

    private static final int DEFAULT_MESSAGE_SIZE = 64;

    private static final long DEFAULT_MESSAGE_COUNT = 50_000_000;

    private static final int DEFAULT_WARMUP_ITERATIONS = 1;

    private static final int DEFAULT_BENCHMARK_ITERATIONS = 1;

    private static final MessageMode DEFAULT_MESSAGE_MODE = MessageMode.RSOCKET;

    @CommandLine.Option(
            names = "--bytes",
            description = "The number of bytes per message.")
    private int messageSize = DEFAULT_MESSAGE_SIZE;

    @CommandLine.Option(
            names = "--warmups",
            description = "The number of warmup iterations.")
    private int warmups = DEFAULT_WARMUP_ITERATIONS;

    @CommandLine.Option(
            names = "--iterations",
            description = "The number of benchmark iterations.")
    private int iterations = DEFAULT_BENCHMARK_ITERATIONS;

    @CommandLine.Option(
            names = "--messages",
            description = "The number of messages.")
    private long messageCount = DEFAULT_MESSAGE_COUNT;

    @CommandLine.Option(
            names = "--mode",
            description = "The mode to use for sending messages.")
    private MessageMode mode = DEFAULT_MESSAGE_MODE;

    private static final EnumMap<MessageMode, BenchmarkMethod> METHODS = new EnumMap<>(Map.of(
            MessageMode.RSOCKET, ReactiveSendDemo::rsocketSend,
            MessageMode.DIRECT, ReactiveSendDemo::directSend
    ));

    @Override
    protected void onClientReady(List<InfinibandSocket> sockets) {

        // Create data buffer
        final var data = new byte[messageSize];
        ThreadLocalRandom.current().nextBytes(data);

        // Get selected benchmark method
        BenchmarkMethod method = METHODS.get(mode);
        log.info("Selected mode is {}", mode);

        // Perform warmup phase
        log.info("Starting warmup phase");
        for (int i = 0; i < warmups; i++) {
            log.info("| - Warmup iteration {} took {}ms", i, method.execute(sockets, data, messageCount) / 1_000_000.0);
        }

        // Perform benchmark phase
        var times = new long[iterations];
        log.info("Starting benchmark phase");
        for (int i = 0; i < iterations; i++) {
            times[i] = method.execute(sockets, data, messageCount);
            log.info("| - Benchmark iteration {} took {}ms", i, times[i] / 1_000_000.0);
        }

        // Calculate average time
        var time = (long) LongStream.of(times).summaryStatistics().getAverage();
        var result = new Result(messageCount, messageSize, time);

        // Print results
        log.info("");
        result.toString().lines().forEach(log::info);
        log.info("");

        // Close connection and wait until it is closed
        sockets.forEach(InfinibandSocket::dispose);
        sockets.forEach(socket -> socket.onClose().block());
    }

    @Override
    protected MessageMode getMessageMode() {
        return mode;
    }

    private static long rsocketSend(List<InfinibandSocket> sockets, byte[] data, long count) {

        final var messages = count / sockets.size();

        // Create payload
        final var payload = DefaultPayload.create(data);
        final var flux = Flux.<Payload>generate(sink -> sink.next(payload)).take(messages);

        final var requests = sockets.stream()
                .map(socket -> flux.flatMap(socket::fireAndForget))
                .map(Flux::last)
                .reduce(Mono::and)
                .orElseThrow();

        // Start sending messages and block until all completions arrive
        var startTime = System.nanoTime();
        requests.block();
        var duration = System.nanoTime() - startTime;

        payload.release();

        return duration;
    }

    private static long directSend(List<InfinibandSocket> sockets, byte[] data, long count) {

        final var messages = count / sockets.size();

        // Allocate buffer for direct send operation
        final var device = sockets.get(0).getDevice();
        final var buffer = device.allocateMemory(data.length);

        // Copy bytes to buffer
        buffer.writeBytes(data);

        // Create direct send operation using registered memory
        final var operation = new DirectSendOperation(buffer.getLocalHandle());
        final var flux = Flux.<Operation>generate(sink -> sink.next(operation)).take(messages);

        final var requests = sockets.stream()
                .map(socket -> socket.execute(flux))
                .reduce(Mono::and)
                .orElseThrow();

        var startTime = System.nanoTime();
        requests.block();
        var duration = System.nanoTime() - startTime;

        buffer.release();

        return duration;
    }

    @Override
    protected AbstractRSocket getClientHandler() {
        return new EmptyMessageHandler();
    }

    @Override
    protected AbstractRSocket getServerHandler() {
        return new MessageHandler();
    }

    private static class EmptyMessageHandler extends AbstractRSocket {}

    private static class MessageHandler extends AbstractRSocket {

        @Override
        public Mono<Void> fireAndForget(Payload payload) {
            payload.release();
            return Mono.empty();
        }
    }
}
