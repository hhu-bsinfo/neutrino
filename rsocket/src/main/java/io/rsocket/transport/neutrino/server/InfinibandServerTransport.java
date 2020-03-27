package io.rsocket.transport.neutrino.server;

import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.util.DefaultNegotiator;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.neutrino.InfinibandDuplexConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class InfinibandServerTransport implements ServerTransport<Closeable>, Closeable {

    private final NetworkService networkService;
    private final ServerSocket serverSocket;

    private final Scheduler serverScheduler = Schedulers.newSingle("acceptor");

    private final MonoProcessor<Void> onClose = MonoProcessor.create();

    private Disposable serverDisposable;

    @Override
    public Mono<Closeable> start(ConnectionAcceptor acceptor, int mtu) {
        Objects.requireNonNull(acceptor, "acceptor must not be null");

        var mtuEnum = Mtu.fromValue(mtu);
        log.info("Server connection mtu set to {}", mtuEnum.getMtuValue());

        var sockets = Flux.<Socket>generate(sink -> {
            try {
                var socket = serverSocket.accept();
                log.info("Accepted new client connection from {}", socket.getInetAddress());
                sink.next(socket);
            } catch (IOException e) {
                sink.error(e);
            }
        });

        serverDisposable = sockets.map(DefaultNegotiator::fromSocket)
                .flatMap(negotiator -> networkService.connect(negotiator, mtuEnum))
                .map(connection -> new InfinibandDuplexConnection(connection, networkService))
                .map(duplexConnection -> new FragmentationDuplexConnection(duplexConnection, ByteBufAllocator.DEFAULT, mtuEnum.getMtuValue(), false, "server"))
                .flatMap(acceptor)
                .subscribeOn(serverScheduler)
                .subscribe();

        return Mono.just(this);

//        var duplexConnection = networkService.connect(negotiator, mtuEnum)
//                .map(it -> new InfinibandDuplexConnection(it, networkService))
//                .block();
//
//        var fragmentationDuplexConnection = new FragmentationDuplexConnection(
//                duplexConnection,
//                ByteBufAllocator.DEFAULT,
//                mtuEnum.getMtuValue(),
//                false,
//                "server");
//
//        acceptor.apply(fragmentationDuplexConnection).subscribe(ignored -> log.info("Client connection processed"));
//
//        return Mono.just(fragmentationDuplexConnection);
    }

    @Override
    public Mono<Void> onClose() {
        return onClose;
    }

    @Override
    public void dispose() {
        serverDisposable.dispose();
        onClose.onComplete();
    }
}
