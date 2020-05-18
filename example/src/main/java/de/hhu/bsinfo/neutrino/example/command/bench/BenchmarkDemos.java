package de.hhu.bsinfo.neutrino.example.command.bench;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "bench",
        description = "",
        subcommands = { ThroughputBenchmark.class, LatencyBenchmark.class, ReadWriteBenchmark.class }
)
public class BenchmarkDemos implements Runnable {

    @Override
    public void run() {

    }
}
