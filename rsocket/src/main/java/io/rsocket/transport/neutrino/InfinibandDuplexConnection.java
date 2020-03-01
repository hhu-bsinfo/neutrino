package io.rsocket.transport.neutrino;

import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import io.netty.buffer.ByteBuf;
import io.rsocket.internal.BaseDuplexConnection;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InfinibandDuplexConnection extends BaseDuplexConnection {

    private final Connection connection;
    private final NetworkService networkService;

    public InfinibandDuplexConnection(Connection connection, NetworkService networkService) {
        this.connection = connection;
        this.networkService = networkService;
    }

    @Override
    protected void doOnClose() {

    }

    @Override
    public Mono<Void> send(Publisher<ByteBuf> frames) {
        return networkService.send(connection, frames);
    }

    @Override
    public Flux<ByteBuf> receive() {
        return networkService.receive(connection);
    }

}
