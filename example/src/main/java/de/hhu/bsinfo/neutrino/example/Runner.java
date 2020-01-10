package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.example.command.Root;
import de.hhu.bsinfo.neutrino.example.util.InetSocketAddressConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.InetSocketAddress;

@Component
public class Runner implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine.IFactory factory;
    private final Root command;
    private int exitCode;

    public Runner(CommandLine.IFactory factory, Root command) {
        this.factory = factory;
        this.command = command;
    }

    @Override
    public void run(String... args) {
        CommandLine cli = new CommandLine(command, factory);
        cli.registerConverter(InetSocketAddress.class, new InetSocketAddressConverter(22222));
        cli.setCaseInsensitiveEnumValuesAllowed(true);
        exitCode = cli.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
