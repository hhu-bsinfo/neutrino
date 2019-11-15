package io.rsocket.transport.neutrino.server;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.api.util.ServiceProvider;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

public class InfinibandServerTransport implements ServerTransport<InfinibandDuplexConnection> {

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
    public Mono<InfinibandDuplexConnection> start(ConnectionAcceptor acceptor, int mtu) {
        Objects.requireNonNull(acceptor, "acceptor must not be null");
        log.info("Server transport start called");

        var duplexConnection = connectionService.connect(connection, remote)
                .map(it -> new InfinibandDuplexConnection(it, messageService))
                .block();

        log.info("Server connection created");

        acceptor.apply(duplexConnection).subscribe();
        return Mono.just(duplexConnection);
//        return Mono.create(sink -> {
//            connectionService.connect(connection, remote)
//                    .map(it -> new InfinibandDuplexConnection(it, messageService))
//                    .doOnNext(it -> {
//                        acceptor.apply(it).doOnNext(v -> log.info("Connection processed")).subscribe();
//
//                        sink.success(it);
//                    });
//        });
    }
}
