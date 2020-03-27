package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

public interface NeutrinoInbound {
    Flux<ByteBuf> receive(InternalConnection connection);
}
