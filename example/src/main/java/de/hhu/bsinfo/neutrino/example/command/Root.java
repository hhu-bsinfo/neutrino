package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.example.command.arrow.DataGenerator;
import de.hhu.bsinfo.neutrino.example.command.rsocket.ArrowDemo;
import de.hhu.bsinfo.neutrino.example.command.rsocket.GrpcDemo;
import de.hhu.bsinfo.neutrino.example.command.rsocket.MessagingDemo;
import de.hhu.bsinfo.neutrino.example.command.rsocket.ReadDemo;
import de.hhu.bsinfo.neutrino.verbs.CommunicationManager;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = {
        DeviceInfo.class, MessagingTest.class, RdmaTest.class, WindowTest.class,
        ExtendedDeviceInfo.class, ExtendedMessagingTest.class, ExtendedRdmaTest.class, MessagingDemo.class,
        ReadDemo.class, GrpcDemo.class, CommunicationManagerDemo.class, ArrowDemo.class, DataGenerator.class
    }
)
public class Root implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}