package de.hhu.bsinfo.neutrino.api.message;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.util.Expose;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Expose
public interface MessageService {

    Mono<Void> send(Connection connection, Publisher<ByteBuf> frames);

    Flux<ByteBuf> receive(Connection connection);
}
