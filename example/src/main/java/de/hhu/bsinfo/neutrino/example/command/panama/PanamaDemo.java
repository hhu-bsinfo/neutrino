package de.hhu.bsinfo.neutrino.example.command.panama;

import de.hhu.bsinfo.neutrino.verbs.panama.Context;
import de.hhu.bsinfo.neutrino.verbs.panama.Verbs;
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
        try (var deviceList = Verbs.queryDevices();
             var context = Verbs.openDevice(deviceList.get(0))) {

            var device = deviceList.get(0);
            log.info("Opening device '{}' with node type {} and transport type {} succeded", device.name(), device.nodeType(), device.transportType());

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
