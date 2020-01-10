package io.rsocket.transport.neutrino.server;

import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class InfinibandServerTransport implements ServerTransport<Closeable> {

    private final NetworkService networkService;
    private final Negotiator negotiator;

    @Override
    public Mono<Closeable> start(ConnectionAcceptor acceptor, int mtu) {
        Objects.requireNonNull(acceptor, "acceptor must not be null");

        var mtuEnum = Mtu.fromValue(mtu);
        log.info("Server connection mtu set to {}", mtuEnum.getMtuValue());
        var duplexConnection = networkService.connect(negotiator, mtuEnum)
                .map(it -> new InfinibandDuplexConnection(it, networkService))
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
