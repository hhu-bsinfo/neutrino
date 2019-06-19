package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.memory.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.data.NativeInteger;
import de.hhu.bsinfo.neutrino.data.NativeLong;
import de.hhu.bsinfo.neutrino.data.NativeString;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.CustomStruct;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@CommandLine.Command(
        name = "demo",
        description = "Starts the neutrino high-level api demo%n",
        showDefaultValues = true,
        separator = " ")
public class Demo implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final long RDMA_BUFFER_SIZE = 1024;
    private static final long MAGIC = 0xC0FFEE;

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

    private CoreService coreService;
    private ConnectionService connectionService;
    private MessageService messageService;
    private MemoryService memoryService;

    private RegisteredBuffer localBuffer;

    @Override
    public void run() {
        coreService = neutrino.getService(CoreService.class);
        connectionService = neutrino.getService(ConnectionService.class);
        messageService = neutrino.getService(MessageService.class);
        memoryService = neutrino.getService(MemoryService.class);

        localBuffer = coreService.registerMemory(RDMA_BUFFER_SIZE);

        if (isServer) {
            runServer();
        } else {
            runClient();
        }
    }

    private void runServer() {
        localBuffer.putLong(0, MAGIC);
        connectionService.listen(bindAddress)
                .forEach(connection -> {
                    LOGGER.info("New client connection from {}", connection);
                    var remoteHandle = messageService.receive(connection, BufferInformation::new)
                            .map(info -> info.toRemoteHandle(connection))
                            .blockingFirst();

                    LOGGER.info("Writing magic number into remote buffer on connection {}", connection);
                    memoryService.write(localBuffer, 0, remoteHandle, 0, Long.BYTES);
                });
    }

    private void runClient() {
        localBuffer.putLong(0, 0);
        var connection = connectionService.connect(serverAddress).blockingGet();

        LOGGER.info("Established server connection with {}", connection);
        messageService.send(connection, BufferInformation.from(localBuffer));

        while (localBuffer.getLong(0) != MAGIC) {
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(1));
        }

        LOGGER.info("Change detected within local buffer");
    }

    @CustomStruct(2 * Long.BYTES + Integer.BYTES)
    private static final class BufferInformation extends Struct {

        private final NativeLong address = longField();
        private final NativeLong capacity = longField();
        private final NativeInteger key = integerField();

        public BufferInformation(long address, long capacity, int key) {
            this.address.set(address);
            this.capacity.set(capacity);
            this.key.set(key);
        }

        public BufferInformation(LocalBuffer buffer, long offset) {
            super(buffer, offset);
        }

        public static BufferInformation from(RegisteredBuffer buffer) {
            return new BufferInformation(buffer.getHandle(), buffer.capacity(), buffer.getRemoteKey());
        }

        public RemoteHandle toRemoteHandle(Connection connection) {
            return new RemoteHandle(connection, address.get(), capacity.get(), key.get());
        }
    }
}
