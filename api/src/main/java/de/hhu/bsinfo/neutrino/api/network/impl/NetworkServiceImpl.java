package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.event.EventLoopGroup;
import de.hhu.bsinfo.neutrino.api.network.*;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.util.BaseService;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class NetworkServiceImpl extends BaseService<NetworkConfiguration> implements NetworkService {

    /**
     * The Infiniband device used for communication.
     */
    private final InfinibandDevice device;

    /**
     * The Infiniband devices configuration.
     */
    private final InfinibandDeviceConfig deviceConfig;

    /**
     * The number of connections issued by this service.
     */
    private final AtomicInteger connectionCounter = new AtomicInteger();

    /**
     * Metrics collected by the network service.
     */
    private final NetworkMetrics metrics;

    /**
     * The resources used for network operations.
     */
    private final NetworkResources resources;

    /**
     * Agent used for receiving messages.
     */
    private final ReceiveAgent receiveAgent;

    /**
     * Connection manager used to establish new connections.
     */
    private final ConnectionManager connectionManager;

    /**
     * Mapping from connection id to send agent.
     */
    private final IntObjectHashMap<SendAgent> agentMap = new IntObjectHashMap<>();

    /**
     * The event loop group used for handling network events.
     */
    private final EventLoopGroup eventLoopGroup = new EventLoopGroup("network", 2, () -> BusySpinIdleStrategy.INSTANCE);

    public NetworkServiceImpl(InfinibandDevice device, InfinibandDeviceConfig deviceConfig, NetworkMetrics metrics, NetworkConfiguration networkConfig) {
        super(networkConfig);

        this.device = device;
        this.deviceConfig = deviceConfig;
        this.metrics = metrics;

        var sendCompletionChannel = Objects.requireNonNull(
            device.createCompletionChannel(),
            "Creating send completion channel failed"
        );
        var receiveCompletionChannel = Objects.requireNonNull(
            device.createCompletionChannel(),
            "Creating receive completion channel failed"
        );

        var sendCompletionQueue = Objects.requireNonNull(
            device.createCompletionQueue(networkConfig.getCompletionQueueSize(), sendCompletionChannel),
            "Creating send completion queue failed"
        );

        var receiveCompletionQueue = Objects.requireNonNull(
            device.createCompletionQueue(networkConfig.getCompletionQueueSize(), receiveCompletionChannel),
            "Creating receive completion queue failed"
        );

        sendCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);
        receiveCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);

        var sharedReceiveQueue = Objects.requireNonNull(
            device.createSharedReceiveQueue(new SharedReceiveQueue.InitialAttributes.Builder(
                networkConfig.getSharedReceiveQueueSize(),
                networkConfig.getMaxScatterGatherElements()
            ).build()),
            "Creating shared receive queue failed");

        var attributes = Objects.requireNonNull(
            sharedReceiveQueue.queryAttributes(),
            "Querying shared receive queue attributes failed"
        );

        var maxMtu = device.getPortAttributes().getMaxMtu().getMtuValue();
        var maxWorkRequests = attributes.getMaxWorkRequests();
        var receiveBufferPool = BufferPool.create("receive", maxMtu, maxWorkRequests, device::wrapRegion);

        resources = NetworkResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .receiveBufferPool(receiveBufferPool)
                .sharedReceiveQueue(sharedReceiveQueue)
                .receiveCompletionQueue(receiveCompletionQueue)
                .receiveCompletionChannel(receiveCompletionChannel)
                .sendCompletionQueue(sendCompletionQueue)
                .sendCompletionChannel(sendCompletionChannel)
                .build();

        log.debug("Filled up shared receive queue with {} work requests", maxWorkRequests);

        connectionManager = new ConnectionManager(resources);

        receiveAgent = new ReceiveAgent(resources);
        receiveAgent.add(receiveCompletionChannel);
    }

    @Override
    protected void onStart() {
        eventLoopGroup.waitOnStart();
        eventLoopGroup.next().add(receiveAgent);
    }

    @Override
    protected void onDestroy() {
        log.info("Schutting down");
    }

    @Override
    public Mono<Connection> connect(Negotiator negotiator, Mtu mtu) {
        return Mono.fromCallable(() -> {
            var connection = connectionManager.connect(negotiator, mtu);
            var sendAgent = new SendAgent(resources, connection);

            agentMap.put(connection.getId(), sendAgent);
            eventLoopGroup.next().add(sendAgent);

            return connection;
        });
    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {
        var sendAgent = agentMap.get(connection.getId());
        return sendAgent.send(frames);
    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {
        return receiveAgent.receive();
    }

    @Override
    public Mono<Void> write(Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Mono<Void> read(Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }
}
