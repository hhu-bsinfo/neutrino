package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.*;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class MessageServiceImpl extends Service<NullConfig> implements MessageService {

    @Inject
    private InternalConnectionService connectionService;

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {
        var queuePair = connection.getQueuePair();
        var sendBuffer = connection.getSendBuffer();
        var completionQueue = connection.getCompletionQueue();
        return Flux.from(frames)
                .doOnNext(source -> {
                    log.info("Sending {} bytes", source.readableBytes());
                    var target = sendBuffer.getBuffer();
                    var messageSize = source.readableBytes();
                    target.setBytes(0, source);
                    var element = new ScatterGatherElement(target.memoryAddress(), messageSize, sendBuffer.getLocalKey());
                    queuePair.postSend(new SendWorkRequest(configurator -> {
                        configurator.setOpCode(OpCode.SEND);
                        configurator.setListHandle(element.getHandle());
                        configurator.setListLength(1);
                    }));
                })
                .subscribeOn(Schedulers.parallel())
                .publishOn(Schedulers.parallel())
                .then();

    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {
        final var sharedReceiveQueue = connectionService.getSharedReceiveQueue();
        final var queuePair = connection.getQueuePair();
        final var receiveBuffer = connection.getReceiveBuffer();
        final var completionQueue = connection.getCompletionQueue();
        final var completionArray = new CompletionQueue.WorkCompletionArray(100);
        return Flux.<ByteBuf>create(emitter -> {
            fillUp(queuePair, receiveBuffer);
            log.info("Receiving messages");
            while (!emitter.isCancelled()) {
                completionQueue.poll(completionArray);
                for(int i = 0; i < completionArray.getLength(); i++) {
                    var workCompletion = completionArray.get(i);
                    if (workCompletion.getStatus() == WorkCompletion.Status.SUCCESS) {
                        log.info("Received {} bytes", workCompletion.getByteCount());
                        var buffer = receiveBuffer.getBuffer().slice(0, workCompletion.getByteCount());
                        buffer.retain();
                        emitter.next(buffer);
                    } else {
                        emitter.error(new IOException("Request failed"));
                    }
                    fillUp(queuePair, receiveBuffer);
                }
            }
        }).subscribeOn(Schedulers.parallel())
        .publishOn(Schedulers.parallel());
    }

    private void fillUp(QueuePair queuePair, RegisteredByteBuf receiveBuffer) {
        var buffer = receiveBuffer.getBuffer();
        var element = new ScatterGatherElement(buffer.memoryAddress(), buffer.capacity(), receiveBuffer.getLocalKey());

        queuePair.postReceive(new ReceiveWorkRequest(config -> {
            config.setListHandle(element.getHandle());
            config.setListLength(1);
        }));
    }
}
