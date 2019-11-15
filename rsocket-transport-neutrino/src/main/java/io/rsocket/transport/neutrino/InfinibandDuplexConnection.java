package io.rsocket.transport.neutrino;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.internal.BaseDuplexConnection;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InfinibandDuplexConnection extends BaseDuplexConnection {

    private final Connection connection;
    private final MessageService messageService;

    public InfinibandDuplexConnection(Connection connection, MessageService messageService) {
        this.connection = connection;
        this.messageService = messageService;
    }

    @Override
    protected void doOnClose() {

    }

    @Override
    public Mono<Void> send(Publisher<ByteBuf> frames) {
        return messageService.send(connection, frames);
    }

    @Override
    public Flux<ByteBuf> receive() {
        return messageService.receive(connection);
    }

}
