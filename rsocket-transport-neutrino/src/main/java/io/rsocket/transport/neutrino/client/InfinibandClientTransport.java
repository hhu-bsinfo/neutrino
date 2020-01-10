package io.rsocket.transport.neutrino.client;

import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public final class InfinibandClientTransport implements ClientTransport {

    private final NetworkService networkService;
    private final Negotiator negotiator;

    @Override
    public Mono<DuplexConnection> connect(int mtu) {
        var mtuEnum = Mtu.fromValue(mtu);
        log.info("Client connection mtu set to {}", mtuEnum.getMtuValue());
        return  networkService.connect(negotiator, mtuEnum)
                .map(it -> {
                    var duplexConnection = new InfinibandDuplexConnection(it, networkService);
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
