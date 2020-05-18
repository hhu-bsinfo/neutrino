package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.example.command.arrow.ArrowDemo;
import de.hhu.bsinfo.neutrino.example.command.arrow.DataGenerator;
import de.hhu.bsinfo.neutrino.example.command.bench.BenchmarkDemos;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = {
        DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
        ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class,
        CommunicationManagerDemo.class, ArrowDemo.class, DataGenerator.class,
        BenchmarkDemos.class
    }
)
public class Root implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}