package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.data.NativeArray;
import de.hhu.bsinfo.neutrino.example.command.Root;
import de.hhu.bsinfo.neutrino.example.util.InetSocketAddressConverter;
import de.hhu.bsinfo.neutrino.util.StructUtil;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.Device;
import de.hhu.bsinfo.neutrino.verbs.Port;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
