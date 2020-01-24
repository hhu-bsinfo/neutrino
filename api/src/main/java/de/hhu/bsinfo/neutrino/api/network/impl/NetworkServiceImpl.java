package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.*;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.BaseService;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import de.hhu.bsinfo.neutrino.verbs.*;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class NetworkServiceImpl extends BaseService<NetworkServiceConfig> implements NetworkService {

    /**
     * The Infiniband device used for communication.
     */
    private final InfinibandDevice device;

    /**
     * The Infiniband devices configuration.
     */
    private final InfinibandDeviceConfig deviceConfig;

    /**
     * The shared send completion queue.
     */
    private final CompletionQueue sendCompletionQueue;

    /**
     * The shared send completion channel.
     */
    private final CompletionChannel sendCompletionChannel;

    /**
     * The shared receive completion queue.
     */
    private final CompletionQueue receiveCompletionQueue;

    /**
     * The shared receive completion channel.
     */
    private final CompletionChannel receiveCompletionChannel;

    /**
     * The shared receive queue.
     */
    private final SharedReceiveQueue sharedReceiveQueue;

    /**
     * The pool containing registered chunks of memory for receive operations.
     */
    private final BufferPool receiveBufferPool;

    /**
     * The number of connections issued by this service.
     */
    private final AtomicInteger connectionCounter = new AtomicInteger();

    /**
     * All connections issued by this service.
     */
    private final Connection[] connections = new Connection[1024];

    /**
     * Metrics collected by the network service.
     */
    private final NetworkMetrics metrics;

    /**
     * The processor used for signaling incoming buffers.
     */
    private final EmitterProcessor<ByteBuf> in = EmitterProcessor.create();

    private final Scheduler asyncScheduler = Schedulers.newSingle("event");
    private final Scheduler sendScheduler = Schedulers.newSingle("send");
    private final Scheduler receiveScheduler = Schedulers.newSingle("receive");
    private final Scheduler sendProcessorScheduler = Schedulers.newSingle("procSend");
    private final Scheduler receiveProcessorScheduler = Schedulers.newSingle("procReceive");

    private Disposable sendDisposable;
    private Disposable receiveDisposable;
    private Disposable eventDisposable;
    private Disposable logDisposable;
    private Disposable outDisposable;

    private final int fillUpRate;
    private final SharedReceiveQueue.Attributes limitAttributes;
    private static final SharedReceiveQueue.AttributeFlag[] LIMIT_FLAGS = { SharedReceiveQueue.AttributeFlag.LIMIT };

    public NetworkServiceImpl(InfinibandDevice device, InfinibandDeviceConfig deviceConfig, NetworkMetrics metrics, NetworkServiceConfig config) {
        super(config);

        this.device = device;
        this.deviceConfig = deviceConfig;
        this.metrics = metrics;

        sendCompletionChannel = Objects.requireNonNull(
            device.createCompletionChannel(),
            "Creating send completion channel failed"
        );
        receiveCompletionChannel = Objects.requireNonNull(
            device.createCompletionChannel(),
            "Creating receive completion channel failed"
        );

        sendCompletionQueue = Objects.requireNonNull(
            device.createCompletionQueue(getConfig().getCompletionQueueSize(), sendCompletionChannel),
            "Creating send completion queue failed"
        );

        receiveCompletionQueue = Objects.requireNonNull(
            device.createCompletionQueue(getConfig().getCompletionQueueSize(), receiveCompletionChannel),
            "Creating receive completion queue failed"
        );

        sendCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);
        receiveCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);

        sharedReceiveQueue = Objects.requireNonNull(
            device.createSharedReceiveQueue(new SharedReceiveQueue.InitialAttributes.Builder(
                config.getSharedReceiveQueueSize(),
                config.getMaxScatterGatherElements()
            ).build()),
            "Creating shared receive queue failed");

        var attributes = Objects.requireNonNull(
            sharedReceiveQueue.queryAttributes(),
            "Querying shared receive queue attributes failed"
        );

        var maxMtu = device.getPortAttributes().getMaxMtu().getMtuValue();
        var maxWorkRequests = attributes.getMaxWorkRequests();
        receiveBufferPool = BufferPool.create(maxMtu, maxWorkRequests, device::wrapRegion);

        fillUpRate = maxWorkRequests / 4;
        limitAttributes = new SharedReceiveQueue.Attributes.Builder()
                .withLimit(maxWorkRequests / 2)
                .build();

        fillUpReceiveQueue(maxWorkRequests);
    }

    @Override
    protected void onStart() {
        eventDisposable = asyncEvents(device)
                .subscribeOn(asyncScheduler)
                .subscribe(this::handleAsyncEvent);

        receiveDisposable = workCompletions(receiveCompletionChannel, receiveCompletionQueue, getConfig().getCompletionQueueSize())
                .subscribeOn(receiveProcessorScheduler)
                .subscribe(this::onReceiveCompletion);

        sendDisposable = workCompletions(sendCompletionChannel, sendCompletionQueue, getConfig().getCompletionQueueSize())
                .subscribeOn(sendProcessorScheduler)
                .subscribe(this::onSendCompletion);
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
            var initialAttributes = new QueuePair.InitialAttributes.Builder(
                    QueuePair.Type.RC,
                    sendCompletionQueue,
                    receiveCompletionQueue,
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

            var attributes = queuePair.queryAttributes(QueuePair.AttributeFlag.CAP);
            var maxMtu = device.getPortAttributes().getMaxMtu().getMtuValue();
            var bufferPool = BufferPool.create(maxMtu, attributes.capabilities.getMaxSendWorkRequests(), device::wrapRegion);

            var connection = ConnectionImpl.builder()
                    .id(connectionCounter.getAndIncrement())
                    .localId(device.getPortAttributes().getLocalId())
                    .portNumber(deviceConfig.getPortNumber())
                    .queuePair(queuePair)
                    .sendCompletionQueue(sendCompletionQueue)
                    .receiveCompletionQueue(receiveCompletionQueue)
                    .bufferPool(bufferPool)
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

            connections[connection.getId()] = connection;
            return connection;
        });
    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {
        var queuePair = connection.getQueuePair();
        var bufferPool = connection.getBufferPool();
        return Flux.from(frames)
                .doOnNext(source -> {
                        var target = bufferPool.leaseNext();

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
                        sendRequest.setId((long) connection.getId() << 32 | target.getIndex());
                        sendRequest.setOpCode(OpCode.SEND);
                        sendRequest.setSendFlags(SendWorkRequest.SendFlag.SIGNALED);
                        sendRequest.setListHandle(element.getHandle());
                        sendRequest.setListLength(1);

                        var success = queuePair.postSend(sendRequest);
                        if (success) {
                            metrics.incrementSendRequests();
                        }

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

    @Override
    public Mono<Void> write(Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Mono<Void> read(Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }

    private void fillUpReceiveQueue(int count) {
        for (int i = 0; i < count; i++) {
            var buffer = receiveBufferPool.leaseNext();

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

            if (sharedReceiveQueue.postReceive(receiveRequest)) {
                metrics.incrementReceiveRequests();
            }

            element.releaseInstance();
            receiveRequest.releaseInstance();
        }

        armSharedReceiveQueue();
    }

    private void handleReceiveCompletion(WorkCompletion workCompletion) {
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();
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
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Send work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
        }

        connections[(int) (id >> 32)].getBufferPool().release((int) id);
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
            metrics.decrementReceiveRequests();
            handleReceiveCompletion(workCompletions.get(i));
        }
    }

    private void onSendCompletion(CompletionQueue.WorkCompletionArray workCompletions) {
        var workCompletionCount = workCompletions.getLength();
        for (int i = 0; i < workCompletionCount; i++) {
            metrics.decrementSendRequests();
            handleSendCompletion(workCompletions.get(i));
        }
    }

    private void armSharedReceiveQueue() {
        if (!sharedReceiveQueue.modify(limitAttributes, LIMIT_FLAGS)) {
            log.error("Arming shared receive queue failed");
        }
    }

    private static Flux<CompletionQueue.WorkCompletionArray> workCompletions(
            final CompletionChannel completionChannel,
            final CompletionQueue completionQueue,
            final int pollCount) {
        var completionArray = new CompletionQueue.WorkCompletionArray(pollCount);
        return Flux.create(sink -> {
            while (!sink.isCancelled()) {
                completionQueue.poll(completionArray);
                if (completionArray.getLength() != 0) {
                    sink.next(completionArray);
                    continue;
                }

                completionQueue.requestNotification(CompletionQueue.ALL_EVENTS);

                completionQueue.poll(completionArray);
                if (completionArray.getLength() != 0) {
                    sink.next(completionArray);
                    continue;
                }

                completionChannel.getCompletionEvent();
                completionQueue.acknowledgeEvents(1);
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
