package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.example.command.Root;
import de.hhu.bsinfo.neutrino.example.util.InetSocketAddressConverter;
import java.net.InetSocketAddress;
import picocli.CommandLine;

public class App {

    public static void main(String... args) {
        CommandLine cli = new CommandLine(new Root());
        cli.registerConverter(InetSocketAddress.class, new InetSocketAddressConverter(22222));
        cli.setCaseInsensitiveEnumValuesAllowed(true);
        cli.parseWithHandlers(
            new CommandLine.RunLast().useOut(System.out),
            CommandLine.defaultExceptionHandler().useErr(System.err),
            args);
    }
}
