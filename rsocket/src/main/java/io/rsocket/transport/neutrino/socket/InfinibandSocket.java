package io.rsocket.transport.neutrino.socket;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.LocalHandle;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.operation.Operation;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class InfinibandSocket implements RSocket {

    private final RSocket delegate;

    private final Connection connection;

    private final NetworkService networkService;

    private InfinibandSocket(RSocket delegate, Connection connection, NetworkService networkService) {
        this.delegate = delegate;
        this.connection = connection;
        this.networkService = networkService;
    }

    public static InfinibandSocket create(RSocket rSocket, Connection connection, NetworkService networkService) {
        return new InfinibandSocket(rSocket, connection, networkService);
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        return delegate.fireAndForget(payload);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        return delegate.requestResponse(payload);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return delegate.requestStream(payload);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return delegate.requestChannel(payloads);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        return delegate.metadataPush(payload);
    }

    public Mono<Void> execute(Publisher<? extends Operation> publisher) {
        return networkService.execute(connection, publisher);
    }

    public Mono<Void> sendDirect(LocalHandle localHandle) {
        return sendDirect(Mono.just(localHandle));
    }

    public Mono<Void> sendDirect(Publisher<LocalHandle> publisher) {
        return networkService.sendDirect(connection, publisher);
    }

    public Mono<Void> read(LocalHandle localHandle, RemoteHandle remoteHandle) {
        return networkService.read(connection, localHandle, remoteHandle);
    }

    public Mono<Void> write(LocalHandle localHandle, RemoteHandle remoteHandle) {
        return networkService.write(connection, localHandle, remoteHandle);
    }

    public InfinibandDevice getDevice() {
        return networkService.getDevice();
    }

    @Override
    public Mono<Void> onClose() {
        return delegate.onClose();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
