package de.hhu.bsinfo.neutrino.example.command.panama;

import de.hhu.bsinfo.neutrino.verbs.panama.Context;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

@Slf4j
@CommandLine.Command(
        name = "panama",
        description = "Demonstrates neutrino using Project Panama's Foreign Function Interface.%n",
        showDefaultValues = true,
        separator = " ")
public class PanamaDemo implements Runnable {

    @Override
    public void run() {
        try (var context = Context.openDevice(0)) {
            log.info("Opening context successful {}", context.memoryAddress());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
