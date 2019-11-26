package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.connection.Connection;
import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.message.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.message.impl.processor.CompletionHandler;
import de.hhu.bsinfo.neutrino.api.message.impl.processor.QueueProcessor;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.*;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.io.IOException;

@Slf4j
public class MessageServiceImpl extends Service<NullConfig> implements MessageService, CompletionHandler {

    @Inject
    private InternalConnectionService connectionService;

    @Inject
    private InternalCoreService coreService;

    private final Scheduler sendScheduler = Schedulers.newSingle("sender");
    private final Scheduler receiveScheduler = Schedulers.newSingle("receiver");

    private BufferPool bufferPool;
    private QueueProcessor queueProcessor;

    private final EmitterProcessor<ByteBuf> in = EmitterProcessor.create();

    @Override
    protected void onInit(final NullConfig config) {
        bufferPool = BufferPool.create(4096, 1024, coreService::registerBuffer);
        queueProcessor = new QueueProcessor(this, coreService.getCompletionChannel());
        queueProcessor.start();
    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {
        var queuePair = connection.getQueuePair();
        return Flux.from(frames)
                .doOnNext(source -> {
                        var indexedBuffer = bufferPool.leaseNext();
                        var target = indexedBuffer.getActualBuffer();

                        // Remember number of bytes to send
                        var messageSize = source.readableBytes();
                        log.debug("Sending {} bytes", messageSize);

                        // Copy bytes into send buffer
                        target.writeBytes(source);
                        source.release();

                        // Advance send buffer by number of written bytes
                        var memoryAddress = target.memoryAddress() + target.readerIndex();
                        target.readerIndex(target.writerIndex());

                        // Create scatter gather element containing written bytes
                        var element = new ScatterGatherElement(memoryAddress, messageSize, indexedBuffer.getBuffer().getLocalKey());
                        var isSuccess = queuePair.postSend(new SendWorkRequest(configurator -> {
                            configurator.setId(indexedBuffer.getIndex());
                            configurator.setOpCode(OpCode.SEND);
                            configurator.setFlags(SendWorkRequest.SendFlag.SIGNALED);
                            configurator.setListHandle(element.getHandle());
                            configurator.setListLength(1);
                        }));
                })
                .then()
                .subscribeOn(sendScheduler);

    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {
        final var queuePair = connection.getQueuePair();
        return in.doOnNext(ignored -> fillUp(queuePair, bufferPool.leaseNext())).subscribeOn(receiveScheduler);

//        fillUp(queuePair, receiveBuffer);
//        return Flux.just(0).repeat()
//                .<ByteBuf>handle((ignored, sink) -> {
//                    // Poll receive work completion queue on each iteration
//                    completionQueue.poll(completionArray);
//                    for (int i = 0; i < completionArray.getLength(); i++) {
//                        var workCompletion = completionArray.get(i);
//                        if (workCompletion.getStatus() == WorkCompletion.Status.SUCCESS) {
//                            // Remember the number of bytes received
//                            var bytesReceived = workCompletion.getByteCount();
//
//                            // Advance receive buffer by number of bytes received
//                            var source = receiveBuffer.getBuffer();
//                            source.writerIndex(source.writerIndex() + bytesReceived);
//
//                            // Read slice containing received bytes
//                            var buffer = source.readRetainedSlice(bytesReceived);
//
//
//
//                            // Publish received data to rsocket
//                            sink.next(buffer);
//                        } else {
//                            log.error("Receive work completion status: {}[{}]", workCompletion.getStatusMessage(), workCompletion.getStatus().getValue());
//                            sink.error(new IOException("Request failed"));
//                        }
//                    }
//                })
//                .subscribeOn(receiveScheduler);
    }

    private static void fillUp(QueuePair queuePair, BufferPool.IndexedByteBuf indexedByteBuf) {
        var registeredByteBuf = indexedByteBuf.getBuffer();
        var buffer = registeredByteBuf.getBuffer();
        var element = new ScatterGatherElement(buffer.memoryAddress(), buffer.writableBytes(), registeredByteBuf.getLocalKey());

        var isSuccess = queuePair.postReceive(new ReceiveWorkRequest(config -> {
            config.setId(indexedByteBuf.getIndex());
            config.setListHandle(element.getHandle());
            config.setListLength(1);
        }));

        if (!isSuccess) {
            var attribtues = queuePair.queryAttributes(QueuePair.AttributeFlag.STATE, QueuePair.AttributeFlag.CUR_STATE);
            log.error("Receive queue pair state: {} - {}", attribtues.getState(), attribtues.getCurrentState());
            throw new RuntimeException("Posting receive work request failed");
        } else {
            log.debug("Posted receive work request");
        }
    }

    @Override
    public void onComplete(long id) {
        log.info("Work completion {} successful", id);
        bufferPool.release((int) id);
    }

    @Override
    public void onError(long id, WorkCompletion.Status status) {
        log.error("Work completion {} failed with status {}", id, status);
    }
}
