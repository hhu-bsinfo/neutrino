package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Expose
public interface ConnectionService {

    /**
     * Establishes a connection with the specified remote.
     */
    Mono<Connection> connect(Connection connection, QueuePairAddress address, Mtu mtu);

    /**
     * Creates a new connection.
     */
    Mono<Connection> newConnection();

    /**
     * Listens for incoming connections.
     */
    Flux<Connection> listen();
}
