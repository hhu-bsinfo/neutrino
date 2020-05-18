package de.hhu.bsinfo.neutrino.example.command.bench;

import de.hhu.bsinfo.neutrino.api.network.InfinibandChannel;
import de.hhu.bsinfo.neutrino.example.util.Result;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.util.MemoryUtil;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@CommandLine.Command(
        name = "latency",
        description = "Runs a send throughput benchmark.%n",
        showDefaultValues = true,
        separator = " ")
public class LatencyBenchmark extends BenchmarkDemo {

    private static final int DEFAULT_MESSAGE_SIZE = 0;

    private static final long DEFAULT_MESSAGE_COUNT = 100_000;

    private static final int DEFAULT_WARMUP_ITERATIONS = 5;

    private static final int DEFAULT_BENCHMARK_ITERATIONS = 20;

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

    private static AtomicLong[] startTimes;

    private static final String CSV_HEADER = "producer,size,latency,std,95,99,999\n";

    @Override
    protected void onClientReady(List<InfinibandChannel> channels) {

        try (var writer = new FileWriter(output)) {

            startTimes = new AtomicLong[channels.stream().mapToInt(InfinibandChannel::getId).max().orElseThrow() + 1];
            for (int i = 0; i < startTimes.length; i++) {
                startTimes[i] = new AtomicLong();
            }

            writer.append(CSV_HEADER);

            // Only iterate through all message sizes if no size was set explicitly
            var sizes = messageSize != DEFAULT_MESSAGE_SIZE ? new int[]{ messageSize } : SIZES;
            for (var messageSize : sizes) {

                // Create data buffer
                final var bytes = new byte[messageSize];
                final var data = MemoryUtil.allocateAligned(messageSize, MemoryAlignment.CACHE);
                ThreadLocalRandom.current().nextBytes(bytes);
                data.putBytes(0, bytes);

                var channelCount = channels.size();

                // Create benchmark threads
                var threads = channels.stream()
                        .map(channel -> new BenchmarkThread(channel, data, warmups, iterations, messageCount / channelCount))
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

                // Calculate result
                writer.append(String.format(Locale.US, "%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                        channelCount,
                        messageSize,
                        HISTOGRAM.getMean() / 1000.0,
                        HISTOGRAM.getStdDeviation() / 1000.0,
                        HISTOGRAM.getValueAtPercentile(95.0) / 1000.0,
                        HISTOGRAM.getValueAtPercentile(99.0) / 1000.0,
                        HISTOGRAM.getValueAtPercentile(99.9) / 1000.0
                )).flush();

                HISTOGRAM.reset();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestCompleted(InfinibandChannel channel, int id) {
        var channelId = channel.getId();
        var timer = startTimes[channelId];
        HISTOGRAM.recordValue(System.nanoTime() - timer.get());
        timer.set(0);
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

        private BenchmarkThread(InfinibandChannel channel, DirectBuffer data, int warmups, int iterations, long count) {
            this.channel = channel;
            this.data = data;
            this.count = count;
            this.warmups = warmups;
            durations = new long[iterations];
        }

        @Override
        public void run() {

            var id = channel.getId();
            var timer = startTimes[id];

            for (int i = 0; i < warmups; i++) {
                for (long message = 0; message < count; message++) {
                    timer.set(System.nanoTime());
                    channel.send(0, data, 0, data.capacity());

                    while (timer.get() != 0) {
                        ThreadHints.onSpinWait();
                    }
                }
            }

            for (int iteration = 0; iteration < durations.length; iteration++) {

                // Send messages
                var startTime = System.nanoTime();
                for (long message = 0; message < count; message++) {
                    timer.set(System.nanoTime());
                    channel.send(0, data, 0, data.capacity());

                    while (timer.get() != 0) {
                        ThreadHints.onSpinWait();
                    }
                }
            }
        }
    }
}
