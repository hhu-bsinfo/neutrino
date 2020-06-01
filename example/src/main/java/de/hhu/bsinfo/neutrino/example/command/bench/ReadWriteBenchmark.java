package de.hhu.bsinfo.neutrino.example.command.bench;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.InfinibandChannel;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.util.MemoryUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.hints.ThreadHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@CommandLine.Command(
        name = "memory",
        description = "Runs a remote memory access throughput benchmark.%n",
        showDefaultValues = true,
        separator = " ")
public class ReadWriteBenchmark extends BenchmarkDemo {

    private static final int DEFAULT_MESSAGE_SIZE = 0;

    private static final int DEFAULT_MESSAGE_COUNT = 100;

    private static final int DEFAULT_WARMUP_ITERATIONS = 10;

    private static final int DEFAULT_BENCHMARK_ITERATIONS = 30;

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
    private int messageCount = DEFAULT_MESSAGE_COUNT;

    @CommandLine.Option(
            names = "--output",
            description = "The output file.")
    private File output;

    private static final int[] SIZES = { 1024 * 512, 1024 * 1024, 1024 * 1024 * 2, 1024 * 1024 * 4, 1024 * 1024 * 8, 1024 * 1024 * 16, 1024 * 1024 * 32 };

    private static final String CSV_HEADER = "size,producer,messages,iteration,duration\n";

    private static final AtomicLong warmupCounter = new AtomicLong();
    private static final AtomicLong iterationCounter = new AtomicLong();
    private static final AtomicLong requestCounter = new AtomicLong();

    private final AtomicReference<RemoteHandle> remoteRegion = new AtomicReference<>();

    @Autowired
    private InfinibandDevice device;

    private RegisteredBuffer serverBuffer;
    private RegisteredBuffer clientBuffer;

    @Override
    protected void onClientReady(List<InfinibandChannel> channels) {

        try (var writer = new FileWriter(output)) {

            writer.append(CSV_HEADER);

            // Only iterate through all message sizes if no size was set explicitly
            var sizes = messageSize != DEFAULT_MESSAGE_SIZE ? new int[]{messageSize} : SIZES;
            for (var bufferSize : sizes) {

                // Create client buffer for each connection
                clientBuffer = device.allocateMemory(bufferSize, MemoryAlignment.PAGE);
                final var bytes = new byte[bufferSize];
                ThreadLocalRandom.current().nextBytes(bytes);
                clientBuffer.putBytes(0, bytes);

                // Request remote memory information
                var remoteHandle = requestMemory(channels.get(0), bufferSize);

                var channelCount = channels.size();

                // Create benchmark threads
                var threads = channels.stream()
                        .map(channel -> new BenchmarkThread(channel, clientBuffer, remoteHandle, warmups, iterations, messageCount / channelCount, writer, messageCount))
                        .toArray(BenchmarkThread[]::new);

                log.info("Starting benchmark with message size {}B and {} producer threads", bufferSize, channelCount);

                // Start benchmark threads
                for (BenchmarkThread thread : threads) {
                    thread.start();
                }

                // Wait for all benchmark threads to finish
                for (BenchmarkThread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                writer.flush();
                requestCounter.set(0);
                iterationCounter.set(0);
                warmupCounter.set(0);
                remoteRegion.set(null);
            }
        } catch (IOException  e) {
            e.printStackTrace();
        }
    }

    private RemoteHandle requestMemory(InfinibandChannel channel, int capacity) {
        var request = MemoryUtil.allocateAligned(64, MemoryAlignment.CACHE);
        request.putInt(0, capacity);

        channel.send(0, request, 0, request.capacity());

        while (remoteRegion.get() == null) {
            ThreadHints.onSpinWait();
        }

        return remoteRegion.get();
    }

    @Override
    public void onRequestCompleted(InfinibandChannel channel, int id) {
        if (requestCounter.incrementAndGet() == messageCount) {
            requestCounter.set(0);

            if (warmupCounter.incrementAndGet() > warmups) {
                iterationCounter.incrementAndGet();
            }
        }
    }

    @Override
    public void onRequestFailed(InfinibandChannel channel, int id) {

    }

    @Override
    public void onMessage(InfinibandChannel channel, DirectBuffer buffer, int offset, int length) {

        // Server responds to memory allocation requests
        if (isServer()) {
            var capacity = buffer.getInt(offset);

            log.info("Allocating buffer with size {}B", capacity);

            try {
                serverBuffer = device.allocateMemory(capacity, MemoryAlignment.PAGE);
            } catch (IOException e) {
                log.error("Unexpected error", e);
            }

            serverBuffer.putInt(0, 0x42);

            var response = MemoryUtil.allocateAligned(64, MemoryAlignment.CACHE);
            response.putLong(0, serverBuffer.addressOffset());
            response.putInt(Long.BYTES, serverBuffer.remoteKey());
            channel.send(0, response, 0, response.capacity());
        }

        // Client receives response from server and creates remote handle to start benchmark
        if (isClient()) {
            var handle = new RemoteHandle(buffer.getLong(0), buffer.getInt(Long.BYTES));
            log.info("Received {}", handle);
            remoteRegion.set(handle);
        }
    }

    private static final class BenchmarkThread extends Thread {

        private final InfinibandChannel channel;

        private final RegisteredBuffer buffer;

        private final RemoteHandle handle;

        private final int localCount;

        private final int globalCount;

        private final int warmups;

        private final int iterations;

        private final FileWriter writer;

        private BenchmarkThread(InfinibandChannel channel, RegisteredBuffer buffer, RemoteHandle handle, int warmups, int iterations, int localCount, FileWriter writer, int globalCount) {
            this.channel = channel;
            this.buffer = buffer;
            this.handle = handle;
            this.localCount = localCount;
            this.warmups = warmups;
            this.writer = writer;
            this.globalCount = globalCount;
            this.iterations = iterations;
        }

        @Override
        public void run() {
            log.info("Starting with buffer {}", buffer);

            for (int warmup = 0; warmup < warmups; warmup++) {

                // Send messages
                for (int count = 1; count <= localCount; count++) {
                    channel.write(count, buffer, 0, buffer.capacity(), handle);
                }

                // Wait until all messages have been processed
                while (warmupCounter.get() != warmup + 1) {
                    ThreadHints.onSpinWait();
                }
            }

            for (int iteration = 0; iteration < iterations; iteration++) {

                // Send messages
                var startTime = System.nanoTime();
                for (int count = 1; count <= localCount; count++) {
                    channel.write(count, buffer, 0, buffer.capacity(), handle);
                }

                // Wait until all messages have been processed
                while (iterationCounter.get() != iteration + 1) {
                    ThreadHints.onSpinWait();
                }

                // Record duration
                var duration = System.nanoTime() - startTime;

                log.info("Benchmark iteration {} took {}ms", iteration, duration / 1_000_000.0);

                // First channel is responsible for writing results
                if (channel.getId() == 0) {
                    try {
                        writer.append(String.format(Locale.US, "%d,%s,%d,%d,%.4f\n",
                                buffer.capacity(),
                                "One Connection",
                                globalCount,
                                iteration,
                                duration / 1_000_000.0
                        ));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
