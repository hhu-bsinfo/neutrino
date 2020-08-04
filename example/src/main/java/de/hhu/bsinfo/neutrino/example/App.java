package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.example.command.Root;
import de.hhu.bsinfo.neutrino.example.util.InetSocketAddressConverter;
import de.hhu.bsinfo.neutrino.util.NativeLibrary;
import picocli.CommandLine;

import java.net.InetSocketAddress;

public final class App {

    static{
        NativeLibrary.load("neutrino");
    }

    @SuppressWarnings("CallToSystemExit")
    public static void main(String... args) {
        var exitCode = new CommandLine(new Root())
                .registerConverter(InetSocketAddress.class, new InetSocketAddressConverter(22222))
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);

        System.exit(exitCode);
    }
}
