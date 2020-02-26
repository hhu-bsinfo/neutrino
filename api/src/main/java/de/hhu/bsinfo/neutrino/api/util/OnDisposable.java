package de.hhu.bsinfo.neutrino.api.util;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface OnDisposable extends Disposable {

    Mono<Void> onDispose();

    @SuppressWarnings("unchecked")
    default <T extends OnDisposable> T onDispose(Disposable disposable) {
        onDispose().subscribe(null, e -> disposable.dispose(), disposable::dispose);
        return (T) this;
    }
}
