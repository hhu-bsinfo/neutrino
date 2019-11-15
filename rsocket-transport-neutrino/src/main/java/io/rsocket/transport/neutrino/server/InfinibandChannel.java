package io.rsocket.transport.neutrino.server;

import io.rsocket.Closeable;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public class InfinibandChannel implements Closeable {

    private boolean isDisposed;

    @Override
    public Mono<Void> onClose() {
        return Mono.empty();
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
