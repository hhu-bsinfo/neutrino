package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.example.command.arrow.DataGenerator;
import de.hhu.bsinfo.neutrino.example.command.arrow.ArrowDemo;
import de.hhu.bsinfo.neutrino.example.command.grpc.GrpcDemo;
import de.hhu.bsinfo.neutrino.example.command.reactive.ReactiveDemos;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = {
        DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
        ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class,
        GrpcDemo.class, CommunicationManagerDemo.class, ArrowDemo.class, DataGenerator.class,
        ReactiveDemos.class
    }
)
public class Root implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}