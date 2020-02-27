package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.OnDisposable;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

public interface NeutrinoInbound {
    Flux<ByteBuf> receive();
}
