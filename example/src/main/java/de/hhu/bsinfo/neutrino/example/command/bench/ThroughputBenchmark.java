package de.hhu.bsinfo.neutrino.example.command.bench;

import de.hhu.bsinfo.neutrino.api.network.InfinibandChannel;
import de.hhu.bsinfo.neutrino.api.network.NetworkHandler;
import de.hhu.bsinfo.neutrino.example.util.Result;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.util.MemoryUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.agrona.DirectBuffer;
import org.agrona.hints.ThreadHints;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Component
@CommandLine.Command(
        name = "throughput",
        description = "Runs a send throughput benchmark.%n",
        showDefaultValues = true,
        separator = " ")
public class ThroughputBenchmark extends BenchmarkDemo {

    private static final int DEFAULT_MESSAGE_SIZE = 0;

    private static final long DEFAULT_MESSAGE_COUNT = 1_000_000;

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
    private long messageCount = DEFAULT_MESSAGE_COUNT;

    @CommandLine.Option(
            names = "--output",
            description = "The output file.")
    private File output;

    private static final Histogram HISTOGRAM = new AtomicHistogram(3600000000000L, 3);

    private static final int[] SIZES = { 16, 32, 64, 128, 256, 512, 1024, 2048, 4096 };

    private static final String CSV_HEADER = "size,messages,iteration,duration\n";

    private static final AtomicLong warmupCounter = new AtomicLong();
    private static final AtomicLong iterationCounter = new AtomicLong();
    private static final AtomicLong requestCounter = new AtomicLong();

    @Override
    protected void onClientReady(List<InfinibandChannel> channels) {

        try (var writer = new FileWriter(output)) {

            writer.append(CSV_HEADER);

            // Only iterate through all message sizes if no size was set explicitly
            var sizes = messageSize != DEFAULT_MESSAGE_SIZE ? new int[]{messageSize} : SIZES;
            for (var messageSize : sizes) {

                // Create data buffer
                final var bytes = new byte[messageSize];
                final var data = MemoryUtil.allocateAligned(messageSize, MemoryAlignment.CACHE);
                ThreadLocalRandom.current().nextBytes(bytes);
                data.putBytes(0, bytes);

                var channelCount = channels.size();

                // Create benchmark threads
                var threads = channels.stream()
                        .map(channel -> new BenchmarkThread(channel, data, warmups, iterations, messageCount / channelCount, writer, messageCount))
                        .toArray(BenchmarkThread[]::new);

                log.info("Starting benchmark with message size {}B and {} producer threads", messageSize, channelCount);

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    }

    private static final class BenchmarkThread extends Thread {

        private final InfinibandChannel channel;

        private final DirectBuffer data;

        private int warmups;

        private final long durations[];

        private final long count;

        private final FileWriter writer;

        private final long totalMessages;

        private BenchmarkThread(InfinibandChannel channel, DirectBuffer data, int warmups, int iterations, long count, FileWriter writer, long totalMessages) {
            this.channel = channel;
            this.data = data;
            this.count = count;
            this.warmups = warmups;
            this.writer = writer;
            this.totalMessages = totalMessages;
            durations = new long[iterations];
        }

        @Override
        public void run() {

            for (int i = 0; i < warmups; i++) {

                // Send messages
                for (long message = 0; message < count; message++) {
                    channel.send(0, data, 0, data.capacity());
                }

                // Wait until all messages have been processed
                while (warmupCounter.get() != i + 1) {
                    ThreadHints.onSpinWait();
                }
            }

            for (int iteration = 0; iteration < durations.length; iteration++) {

                // Send messages
                var startTime = System.nanoTime();
                for (long message = 0; message < count; message++) {
                    channel.send(0, data, 0, data.capacity());
                }

                // Wait until all messages have been processed
                while (iterationCounter.get() != iteration + 1) {
                    ThreadHints.onSpinWait();
                }

                // Record duration
                var duration = System.nanoTime() - startTime;

                // First channel is responsible for writing results
                if (channel.getId() == 0) {
                    try {
                        writer.append(String.format(Locale.US, "%d,%d,%d,%.4f\n",
                                data.capacity(),
                                totalMessages,
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
