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
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import io.netty.buffer.ByteBuf;
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
public class InternalNetworkService extends BaseService<NetworkConfiguration> implements NetworkService {

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
    private final SharedResources sharedResources;

    /**
     * Connection manager used to establish new connections.
     */
    private final ConnectionManager connectionManager;

    /**
     * Agent used for sending messages.
     */
    private ReceiveAgent receiveAgent;

    /**
     * Agent used for receiving messages.
     */
    private SendAgent sendAgent;

    /**
     * The event loop group used for handling outbound network operations.
     */
    private final EventLoopGroup sendGroup = new EventLoopGroup("send", 1, () -> BusySpinIdleStrategy.INSTANCE);

    /**
     * The event loop group used for handling inbound network operations.
     */
    private final EventLoopGroup receiveGroup = new EventLoopGroup("receive", 1, () -> BusySpinIdleStrategy.INSTANCE);

    public InternalNetworkService(InfinibandDevice device, InfinibandDeviceConfig deviceConfig, NetworkMetrics metrics, NetworkConfiguration networkConfig) {
        super(networkConfig);

        this.device = device;
        this.deviceConfig = deviceConfig;
        this.metrics = metrics;

        // Create initial attributes for shared receive queue
        var initialAttributes = new SharedReceiveQueue.InitialAttributes.Builder(
                networkConfig.getSharedReceiveQueueSize(),
                networkConfig.getMaxScatterGatherElements()
        ).build();

        // Create shared receive queue for posting receive work requests
        var sharedReceiveQueue = device.createSharedReceiveQueue(initialAttributes);

        // Create shared buffer pool for network operations
        var sharedBufferPool = BufferPool.create(
                "shared",
                networkConfig.getMtu(),
                networkConfig.getPoolSize(),
                device::wrapRegion
        );

        // Create shared resources for other network components
        sharedResources = SharedResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .bufferPool(sharedBufferPool)
                .sharedReceiveQueue(sharedReceiveQueue)
                .build();

        // Initialize connection manager
        connectionManager = new ConnectionManager(sharedResources);
    }

    @Override
    protected void onStart() {

        // Wait on event loop groups to start
        log.debug("Waiting on event loops to start");
        sendGroup.waitOnStart();
        receiveGroup.waitOnStart();
        log.debug("Event loops started");

        // Register receive agent within event loop group
        receiveAgent = new ReceiveAgent(sharedResources);
        receiveGroup.next().add(receiveAgent);

        // Register receive agent within event loop group
        sendAgent = new SendAgent(sharedResources);
        sendGroup.next().add(sendAgent);
    }

    @Override
    protected void onDestroy() {
        log.info("Schutting down");
    }

    @Override
    public Mono<Connection> connect(Negotiator negotiator, Mtu mtu) {
        return Mono.fromCallable(() -> {

            // Connect to the remote
            var connection = connectionManager.connect(negotiator, mtu);

            // Assign send agent to connection
            connection.setSendAgent(sendAgent);
            sendAgent.add(connection);

            // Assign receive agent to connection
            connection.setReceiveAgent(receiveAgent);
            receiveAgent.add(connection);

            // Return connection handle
            return new Connection(connection.getId());
        });
    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Get associated send agent
        var agent = internalConnection.getSendAgent();

        // Send frames using agent
        return agent.send(internalConnection, frames);
    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Get associated receive agent
        var agent = internalConnection.getReceiveAgent();

        // Receive frames using agent
        return agent.receive();
    }

    @Override
    public Mono<Void> write(Connection connection, Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Mono<Void> read(Connection connection, Buffer buffer, RemoteHandle handle) {
        throw new UnsupportedOperationException("not implemented");
    }
}
