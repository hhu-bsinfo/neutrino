package de.hhu.bsinfo.neutrino.api;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class Neutrino {

    private final InfinibandDevice device;
    private final NetworkService networkService;

    public Neutrino(InfinibandDevice device, NetworkService networkService) {
        this.device = device;
        this.networkService = networkService;
    }

    public Mono<Buffer> allocate(int capacity) {
        return Mono.just(device.allocateMemory(capacity));
    }

    public Mono<Void> send(Connection connection, Publisher<ByteBuf> buffers) {
        return networkService.send(connection, buffers);
    }

    public Flux<ByteBuf> receive(Connection connection) {
        return networkService.receive(connection);
    }

    public Mono<Void> write(Buffer buffer, RemoteHandle remoteHandle) {
        return networkService.write(buffer, remoteHandle);
    }

    public Mono<Void> read(Buffer buffer, RemoteHandle remoteHandle) {
        return networkService.read(buffer, remoteHandle);
    }
}
