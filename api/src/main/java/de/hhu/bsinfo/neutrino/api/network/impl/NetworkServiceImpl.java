package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.Connection;
import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import de.hhu.bsinfo.neutrino.api.network.NetworkServiceConfig;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.BaseService;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.AsyncEvent;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import de.hhu.bsinfo.neutrino.verbs.Verbs;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class NetworkServiceImpl extends BaseService<NetworkServiceConfig> implements NetworkService {

    private final InfinibandDevice device;
    private final InfinibandDeviceConfig deviceConfig;

    private final CompletionChannel sendCompletionChannel;
    private final CompletionChannel receiveCompletionChannel;
    private final SharedReceiveQueue sharedReceiveQueue;

    public NetworkServiceImpl(NetworkServiceConfig config, InfinibandDevice device, InfinibandDeviceConfig deviceConfig) {
        super(config);

        this.device = device;
        this.deviceConfig = deviceConfig;

        sendCompletionChannel = Objects.requireNonNull(device.createCompletionChannel());
        receiveCompletionChannel = Objects.requireNonNull(device.createCompletionChannel());
        sharedReceiveQueue = Objects.requireNonNull(device.createSharedReceiveQueue(new SharedReceiveQueue.InitialAttributes.Builder(
                config.getSharedReceiveQueueSize(),
                config.getMaxScatterGatherElements()
        ).build()), "Creating shared receive queue failed");
    }

    private final Scheduler asyncScheduler = Schedulers.newSingle("event");
    private final Scheduler sendScheduler = Schedulers.newSingle("send");
    private final Scheduler receiveScheduler = Schedulers.newSingle("receive");
    private final Scheduler sendProcessorScheduler = Schedulers.newSingle("procSend");
    private final Scheduler receiveProcessorScheduler = Schedulers.newSingle("procReceive");

    private Disposable sendDisposable;
    private Disposable receiveDisposable;
    private Disposable eventDisposable;
//    private Disposable logDisposable;

    private BufferPool sendBufferPool;
    private BufferPool receiveBufferPool;

    private final EmitterProcessor<ByteBuf> in = EmitterProcessor.create();

    private int fillUpRate;
    private SharedReceiveQueue.Attributes limitAttributes;
    private static final SharedReceiveQueue.AttributeFlag[] LIMIT_FLAGS = { SharedReceiveQueue.AttributeFlag.LIMIT };

//    private final AtomicLong sendWorkRequests = new AtomicLong();
//    private final AtomicLong receiveWorkRequests = new AtomicLong();

    @Override
    protected void onStart() {
        var attributes = sharedReceiveQueue.queryAttributes();

        sendBufferPool = BufferPool.create(4096, attributes.getMaxWorkRequests(), device::wrapRegion);
        receiveBufferPool = BufferPool.create(4096, attributes.getMaxWorkRequests(), device::wrapRegion);
        fillUpRate = attributes.getMaxWorkRequests() / 4;
        limitAttributes = new SharedReceiveQueue.Attributes.Builder().withLimit(attributes.getMaxWorkRequests() / 2).build();

        fillUpReceiveQueue(attributes.getMaxWorkRequests());

        eventDisposable = asyncEvents(device)
                .subscribeOn(asyncScheduler)
                .subscribe(this::handleAsyncEvent);

        receiveDisposable = workCompletions(receiveCompletionChannel, getConfig().getCompletionQueueSize())
                .subscribeOn(receiveProcessorScheduler)
                .subscribe(this::onReceiveCompletion);

        sendDisposable = workCompletions(sendCompletionChannel, getConfig().getCompletionQueueSize())
                .subscribeOn(sendProcessorScheduler)
                .subscribe(this::onSendCompletion);

//        logDisposable = Flux.interval(Duration.ofSeconds(1))
//                .subscribeOn(Schedulers.parallel())
//                .subscribe(ignored -> log.info("Send: {}, Receive: {}", sendWorkRequests.get(), receiveWorkRequests.get()));
    }

    @Override
    protected void onDestroy() {
        log.info("Schutting down");
        receiveDisposable.dispose();
        sendDisposable.dispose();
    }

    @Override
    public Mono<Connection> connect(Negotiator negotiator, Mtu mtu) {
        return Mono.fromCallable(() -> {
            var sendQueue = device.createCompletionQueue(getConfig().getCompletionQueueSize(), sendCompletionChannel);
            var receiveQueue = device.createCompletionQueue(getConfig().getCompletionQueueSize(), receiveCompletionChannel);

            sendQueue.requestNotification(CompletionQueue.ALL_EVENTS);
            receiveQueue.requestNotification(CompletionQueue.ALL_EVENTS);

            var initialAttributes = new QueuePair.InitialAttributes.Builder(
                    QueuePair.Type.RC,
                    sendQueue,
                    receiveQueue,
                    getConfig().getQueuePairSize(),
                    getConfig().getQueuePairSize(),
                    getConfig().getMaxScatterGatherElements(),
                    getConfig().getMaxScatterGatherElements())
                    .withSharedReceiveQueue(sharedReceiveQueue)
                    .build();

            var queuePair = device.createQueuePair(initialAttributes);

            queuePair.modify(new QueuePair.Attributes.Builder()
                    .withState(QueuePair.State.INIT)
                    .withPartitionKeyIndex((short) 0)
                    .withPortNumber(deviceConfig.getPortNumber())
                    .withAccessFlags(AccessFlag.LOCAL_WRITE, AccessFlag.REMOTE_WRITE, AccessFlag.REMOTE_READ));

            var connection = ConnectionImpl.builder()
                    .localId(device.getPortAttributes().getLocalId())
                    .portNumber(deviceConfig.getPortNumber())
                    .queuePair(queuePair)
                    .sendCompletionQueue(sendQueue)
                    .receiveCompletionQueue(receiveQueue)
                    .build();

            var remote = negotiator.exchange(QueuePairAddress.builder()
                    .localId(connection.getLocalId())
                    .portNumber(connection.getPortNumber())
                    .queuePairNumber(queuePair.getQueuePairNumber()).build());

            queuePair.modify(QueuePair.Attributes.Builder
                    .buildReadyToReceiveAttributesRC(remote.getQueuePairNumber(), remote.getLocalId(), remote.getPortNumber())
                    .withPathMtu(mtu)
                    .withReceivePacketNumber(0)
                    .withMaxDestinationAtomicReads((byte) 1)
                    .withMinRnrTimer(getConfig().getRnrTimer())
                    .withServiceLevel(getConfig().getServiceLevel())
                    .withSourcePathBits((byte) 0)
                    .withIsGlobal(false));

            queuePair.modify(QueuePair.Attributes.Builder.buildReadyToSendAttributesRC()
                    .withTimeout(getConfig().getTimeout())
                    .withRetryCount(getConfig().getRetryCount())
                    .withRnrRetryCount(getConfig().getRnrRetryCount()));

            log.debug("Established connection with {}:{}", remote.getLocalId(), remote.getQueuePairNumber());

            return connection;
        });
    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {
        var queuePair = connection.getQueuePair();
        return Flux.from(frames)
                .doOnNext(source -> {
                        var then = System.currentTimeMillis();
                        var target = sendBufferPool.leaseNext();
                        if (System.currentTimeMillis() - then > Duration.ofSeconds(1).toMillis()) {
                            log.warn("Leasing send buffer took {} seconds", (System.currentTimeMillis() - then) / 1000);
                        }

                        // Remember number of bytes to send
                        var messageSize = source.readableBytes();

                        // Copy bytes into send buffer
                        target.writeBytes(source);
                        source.release();

                        // Advance send buffer by number of written bytes
                        var memoryAddress = target.memoryAddress() + target.readerIndex();
                        target.readerIndex(target.writerIndex());

                        // Create scatter gather element containing written bytes
                        var element = Verbs.getPoolableInstance(ScatterGatherElement.class);

                        element.clear();
                        element.setAddress(memoryAddress);
                        element.setLength(messageSize);
                        element.setLocalKey(target.getLocalKey());

                        var sendRequest = Verbs.getPoolableInstance(SendWorkRequest.class);

                        sendRequest.clear();
                        sendRequest.setId(target.getIndex());
                        sendRequest.setOpCode(OpCode.SEND);
                        sendRequest.setFlags(SendWorkRequest.SendFlag.SIGNALED);
                        sendRequest.setListHandle(element.getHandle());
                        sendRequest.setListLength(1);

                        var isSuccess = queuePair.postSend(sendRequest);
//                        sendWorkRequests.incrementAndGet();

                        element.releaseInstance();
                        sendRequest.releaseInstance();
                })
                .then()
                .subscribeOn(sendScheduler);
    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {
        return in.publishOn(receiveScheduler);
    }

    private void fillUpReceiveQueue(int count) {
        for (int i = 0; i < count; i++) {
            var then = System.currentTimeMillis();
            var buffer = receiveBufferPool.leaseNext();
            if (System.currentTimeMillis() - then > Duration.ofSeconds(1).toMillis()) {
                log.warn("Leasing receive buffer took {} seconds", (System.currentTimeMillis() - then) / 1000);
            }

            var element = Verbs.getPoolableInstance(ScatterGatherElement.class);

            element.clear();
            element.setAddress(buffer.memoryAddress());
            element.setLength(buffer.writableBytes());
            element.setLocalKey(buffer.getLocalKey());

            var receiveRequest = Verbs.getPoolableInstance(ReceiveWorkRequest.class);

            receiveRequest.clear();
            receiveRequest.setId(buffer.getIndex());
            receiveRequest.setListHandle(element.getHandle());
            receiveRequest.setListLength(1);

            var isSuccess = sharedReceiveQueue.postReceive(receiveRequest);
//            receiveWorkRequests.incrementAndGet();

            element.releaseInstance();
            receiveRequest.releaseInstance();

            if (!isSuccess) {
                break;
            }
        }

        armSharedReceiveQueue();
    }

    private void handleReceiveCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
//        receiveWorkRequests.decrementAndGet();

        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Receive work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
            receiveBufferPool.release((int) id);
            return;
        }

        // Remember the number of bytes received
        var bytesReceived = workCompletion.getByteCount();

        // Advance receive buffer by number of bytes received
        var source = receiveBufferPool.get((int) workCompletion.getId());
        source.writerIndex(source.writerIndex() + bytesReceived);

        // Read slice containing received bytes
        var buffer = source.readSlice(bytesReceived);

        // Publish received data to rsocket
        in.onNext(buffer);
    }

    private void handleSendCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
//        sendWorkRequests.decrementAndGet();

        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
        }

        sendBufferPool.release((int) id);
    }

    private void handleAsyncEvent(AsyncEvent asyncEvent) {
        switch (asyncEvent.getEventType()) {
            case SRQ_LIMIT_REACHED:
                fillUpReceiveQueue(fillUpRate);
                break;
            default:
                log.warn("Received async event {}", asyncEvent.getEventType());
                break;
        }

        asyncEvent.acknowledge();
    }

    private void onReceiveCompletion(CompletionQueue.WorkCompletionArray workCompletions) {
        var workCompletionCount = workCompletions.getLength();
        for (int i = 0; i < workCompletionCount; i++) {
            handleReceiveCompletion(workCompletions.get(i));
        }
    }

    private void onSendCompletion(CompletionQueue.WorkCompletionArray workCompletions) {
        var workCompletionCount = workCompletions.getLength();
        for (int i = 0; i < workCompletionCount; i++) {
            handleSendCompletion(workCompletions.get(i));
        }
    }

    private void armSharedReceiveQueue() {
        if (!sharedReceiveQueue.modify(limitAttributes, LIMIT_FLAGS)) {
            log.error("Arming shared receive queue failed");
        }
    }

    private static Flux<CompletionQueue.WorkCompletionArray> workCompletions(CompletionChannel completionChannel, int pollCount) {
        var completionArray = new CompletionQueue.WorkCompletionArray(pollCount);
        return Flux.create(sink -> {
            while (!sink.isCancelled()) {
                var completionQueue = completionChannel.getCompletionEvent();
                completionQueue.poll(completionArray);

                sink.next(completionArray);

                completionQueue.acknowledgeEvents(1);
                completionQueue.requestNotification(CompletionQueue.ALL_EVENTS);
            }
        });
    }

    private static Flux<AsyncEvent> asyncEvents(InfinibandDevice device) {
        return Flux.create(sink -> {
            while (!sink.isCancelled()) {
                sink.next(device.getAsyncEvent());
            }
        });
    }
}
