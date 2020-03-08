package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.ReadOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.SendOperation;
import de.hhu.bsinfo.neutrino.api.network.impl.operation.WriteOperation;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface NeutrinoOutbound {
    Mono<Void> send(InternalConnection connection, Publisher<SendOperation> operations);

    Mono<Void> write(InternalConnection connection, Publisher<WriteOperation> operations);

    Mono<Void> read(InternalConnection connection, Publisher<ReadOperation> operation);
}
