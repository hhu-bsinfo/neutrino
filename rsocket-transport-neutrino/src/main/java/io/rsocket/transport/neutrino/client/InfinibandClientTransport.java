package io.rsocket.transport.neutrino.client;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.api.util.QueuePairConnector;
import de.hhu.bsinfo.neutrino.api.util.ServiceProvider;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import reactor.core.publisher.Mono;

public final class InfinibandClientTransport implements ClientTransport {

    private final Connection connection;
    private final QueuePairAddress remote;
    private final MessageService messageService;
    private final ConnectionService connectionService;

    private InfinibandClientTransport(Connection connection, QueuePairAddress remote, ServiceProvider serviceProvider) {
        this.connection = connection;
        this.remote = remote;
        messageService = serviceProvider.getService(MessageService.class);
        connectionService = serviceProvider.getService(ConnectionService.class);
    }

    public static InfinibandClientTransport create(final Connection connection, final QueuePairAddress remote, final ServiceProvider serviceProvider) {
        return new InfinibandClientTransport(connection, remote, serviceProvider);
    }

    @Override
    public Mono<DuplexConnection> connect(int mtu) {
        return connectionService.connect(connection, remote)
                .map(it -> new InfinibandDuplexConnection(it, messageService));
    }
}
