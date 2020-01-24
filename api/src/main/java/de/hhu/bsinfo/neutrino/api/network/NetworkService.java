package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NetworkService {

    Mono<Connection> connect(Negotiator negotiator, Mtu mtu);

    Mono<Void> send(Connection connection, Publisher<ByteBuf> frames);

    Flux<ByteBuf> receive(Connection connection);

    Mono<Void> write(Buffer buffer, RemoteHandle handle);

    Mono<Void> read(Buffer buffer, RemoteHandle handle);
}
