package de.hhu.bsinfo.neutrino.example.command.reactive;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "reactive",
        description = "",
        subcommands = { ReactiveSendDemo.class, ReactiveReadDemo.class }
)
public class ReactiveDemos implements Runnable {

    @Override
    public void run() {

    }
}
