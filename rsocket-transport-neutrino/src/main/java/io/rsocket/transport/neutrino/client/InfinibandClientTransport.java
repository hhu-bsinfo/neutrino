package io.rsocket.transport.neutrino.client;

import de.hhu.bsinfo.neutrino.api.Neutrino;
import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.ConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.api.util.QueuePairConnector;
import de.hhu.bsinfo.neutrino.api.util.ServiceProvider;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public final class InfinibandClientTransport implements ClientTransport {

    private static final Logger log = LoggerFactory.getLogger(InfinibandClientTransport.class);

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
        var mtuEnum = Mtu.fromValue(mtu);
        log.info("Client connection mtu set to {}", mtuEnum.getMtuValue());
        return connectionService.connect(connection, remote, mtuEnum)
                .map(it -> {
                    var duplexConnection = new InfinibandDuplexConnection(it, messageService);
                    return new FragmentationDuplexConnection(
                            duplexConnection,
                            ByteBufAllocator.DEFAULT,
                            mtuEnum.getMtuValue(),
                            false,
                            "client"
                    );
                });
    }
}
