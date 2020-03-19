package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

public interface NetworkService {

    Mono<Connection> connect(Negotiator negotiator, Mtu mtu);

    Mono<Void> send(Connection connection, Publisher<ByteBuf> frames);

    Flux<ByteBuf> receive(Connection connection);

    Mono<Void> write(Connection connection, LocalHandle localHandle, RemoteHandle handle);

    Mono<Void> read(Connection connection, LocalHandle localHandle, RemoteHandle handle);

    Mono<Connection> connect(InetSocketAddress serverAddress);

    Flux<Connection> listen(InetSocketAddress bindAddress);

    InfinibandDevice getDevice();
}
