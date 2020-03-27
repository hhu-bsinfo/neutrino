package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.operation.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface NeutrinoOutbound {
    Mono<Void> execute(InternalConnection connection, Publisher<? extends Operation> publisher);
}
