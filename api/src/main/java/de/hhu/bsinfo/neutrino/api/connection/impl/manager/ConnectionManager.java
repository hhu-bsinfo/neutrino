package de.hhu.bsinfo.neutrino.api.connection.impl.manager;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final Supplier<Connection> connectionSupplier;
    private final QueuePairConnector connector;

    public ConnectionManager(final Supplier<Connection> connectionSupplier, final QueuePairConnector connector) {
        this.connectionSupplier = connectionSupplier;
        this.connector = connector;
    }

    public Observable<Connection> listen(final InetSocketAddress bindAddress) {
        return Observable.create( emitter -> {
            try (var selector = Selector.open();
                 var serverChannel = ServerSocketChannel.open().bind(bindAddress)) {

                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                while (!emitter.isDisposed()) {
                    selector.select();
                    var selectedKeys = selector.selectedKeys();
                    var iterator = selectedKeys.iterator();
                    while(iterator.hasNext()) {
                        var key = iterator.next();
                        var handler = (ConnectionHandler) key.attachment();

                        if (key.isAcceptable()) {
                            var client = serverChannel.accept();
                            client.configureBlocking(false);
                            var clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            clientKey.attach(new ConnectionHandler(client, clientKey, connectionSupplier.get()));
                        }

                        if (handler != null && key.isReadable()) {
                            var remote = handler.read();
                            if (remote != null) {
                                connector.connect(handler.getConnection().getQueuePair(), remote);
                            }
                        }

                        if (handler != null && key.isWritable()) {
                            handler.write();
                        }

                        if (handler != null && handler.isFinished()) {
                            handler.cancel();
                            emitter.onNext(handler.getConnection());
                        }

                        iterator.remove();
                    }
                }
            }
        });
    }

    public Single<Connection> connect(final InetSocketAddress serverAddress) {
        return Single.fromCallable(() -> {
           try (var channel = SocketChannel.open(serverAddress)) {

               var connection = connectionSupplier.get();

               ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + Integer.BYTES + Byte.BYTES);
               buffer.putShort(connection.getLocalId())
                     .putInt(connection.getQueuePair().getQueuePairNumber())
                     .put(connection.getPortNumber())
                     .flip();

               channel.write(buffer);
               buffer.flip();
               channel.read(buffer);

               var remote = new RemoteQueuePair(buffer.flip());
               connector.connect(connection.getQueuePair(), remote);

               return connection;
           }
        });
    }
}
