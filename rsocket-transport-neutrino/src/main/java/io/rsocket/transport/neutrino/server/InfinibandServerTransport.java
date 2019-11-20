package io.rsocket.transport.neutrino.server;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.api.util.ServiceProvider;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.frame.FragmentationFlyweight;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

public class InfinibandServerTransport implements ServerTransport<Closeable> {

    private static final Logger log = LoggerFactory.getLogger(InfinibandServerTransport.class);

    private final Connection connection;
    private final QueuePairAddress remote;
    private final MessageService messageService;
    private final ConnectionService connectionService;

    private InfinibandServerTransport(Connection connection, QueuePairAddress remote, ServiceProvider serviceProvider) {
        this.connection = connection;
        this.remote = remote;
        messageService = serviceProvider.getService(MessageService.class);
        connectionService = serviceProvider.getService(ConnectionService.class);
    }

    public static InfinibandServerTransport create(final Connection connection, final QueuePairAddress remote, final ServiceProvider serviceProvider) {
        return new InfinibandServerTransport(connection, remote, serviceProvider);
    }

    @Override
    public Mono<Closeable> start(ConnectionAcceptor acceptor, int mtu) {
        Objects.requireNonNull(acceptor, "acceptor must not be null");

        var mtuEnum = Mtu.fromValue(mtu);
        log.info("Server connection mtu set to {}", mtuEnum.getMtuValue());
        var duplexConnection = connectionService.connect(connection, remote, mtuEnum)
                .map(it -> new InfinibandDuplexConnection(it, messageService))
                .block();

        var fragmentationDuplexConnection = new FragmentationDuplexConnection(
                duplexConnection,
                ByteBufAllocator.DEFAULT,
                mtuEnum.getMtuValue(),
                false,
                "server");

        acceptor.apply(fragmentationDuplexConnection).subscribe(ignored -> log.info("Client connection processed"));

        return Mono.just(fragmentationDuplexConnection);
    }
}
