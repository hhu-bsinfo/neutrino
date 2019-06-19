package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.NativeString;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.CustomStruct;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "demo",
        description = "Starts the neutrino high-level api demo%n",
        showDefaultValues = true,
        separator = " ")
public class Demo implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

    private static final int DEFAULT_SERVER_PORT = 2998;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = "--bind",
            description = "The servers bind address.")
    private InetSocketAddress bindAddress = new InetSocketAddress(DEFAULT_SERVER_PORT);

    @CommandLine.Option(
            names = "--connect",
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    private final Neutrino neutrino = Neutrino.newInstance();

    private ConnectionService connectionService;
    private MessageService messageService;

    @Override
    public void run() {
        connectionService = neutrino.getService(ConnectionService.class);
        messageService = neutrino.getService(MessageService.class);

        if (isServer) {
            runServer();
        } else {
            runClient();
        }
    }

    private void runServer() {
        connectionService.listen(bindAddress)
                .forEach(connection -> {
                    var message = messageService.receive(connection, Message::new).blockingFirst();
                    LOGGER.info(message.toString());
                });
    }

    private void runClient() {
        var server = connectionService.connect(serverAddress).blockingGet();
        messageService.send(server, new Message("Hello InfiniBand!"));
    }

    @CustomStruct(128)
    private static final class Message extends Struct {

        private final NativeString message = stringField(128);

        public Message(String message) {
            this.message.set(message);
        }

        public Message(LocalBuffer buffer, long offset) {
            super(buffer, offset);
        }

        @Override
        public String toString() {
            return message.get();
        }
    }
}
