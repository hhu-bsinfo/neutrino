package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.impl.event.EventLoopGroup;
import de.hhu.bsinfo.neutrino.api.network.*;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import de.hhu.bsinfo.neutrino.api.network.operation.*;
import de.hhu.bsinfo.neutrino.api.util.BaseService;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

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
     * The event loop group used for handling outbound network operations.
     */
    private final EventLoopGroup<SendAgent> sendGroup;

    /**
     * The event loop group used for handling inbound network operations.
     */
    private final EventLoopGroup<ReceiveAgent> receiveGroup;

    public InternalNetworkService(InfinibandDevice device, InfinibandDeviceConfig deviceConfig, NetworkMetrics metrics, NetworkConfiguration networkConfig) {
        super(networkConfig);

        this.device = device;
        this.deviceConfig = deviceConfig;
        this.metrics = metrics;

        // Calculate how many workers should be created
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        log.debug("{} available processors detected", availableProcessors);

        var sendWorkerCount = networkConfig.getSendWorker() == 0 ? availableProcessors / 2 : networkConfig.getSendWorker();
        var receiveWorkerCount = networkConfig.getReceiveWorker() == 0 ? availableProcessors / 2 : networkConfig.getReceiveWorker();

        // Create event loop groups
        sendGroup = new EventLoopGroup<>("send", sendWorkerCount, () -> BusySpinIdleStrategy.INSTANCE);
        receiveGroup = new EventLoopGroup<>("receive", receiveWorkerCount, () -> BusySpinIdleStrategy.INSTANCE);

        // Create shared resources for other network components
        sharedResources = SharedResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .networkMetrics(metrics)
                .build();

        // Initialize connection manager
        connectionManager = new ConnectionManager(sharedResources);
    }

    @Override
    protected void onStart() {

        // Wait on event loop groups to start
        log.debug("Waiting on event loops to start");
        List.of(sendGroup, receiveGroup).forEach(EventLoopGroup::waitOnStart);
        log.debug("Event loops started");

        // Register receive agents within event loop group
        for (int i = 0; i < receiveGroup.size(); i++) {
            receiveGroup.next().add(new ReceiveAgent(sharedResources));
        }

        // Register receive agent within event loop group
        for (int i = 0; i < sendGroup.size(); i++) {
            sendGroup.next().add(new SendAgent(sharedResources));
        }
    }

    @Override
    protected void onDestroy() {
        log.info("Schutting down");
    }

    @Override
    public Mono<Connection> connect(Negotiator negotiator, Mtu mtu) {
        return Mono.fromCallable(() -> {

            // Get next send and receive agent
            var sendAgent = sendGroup.next().getAgent();
            var receiveAgent = receiveGroup.next().getAgent();

            var sendResources = sendAgent.getResources();
            var receiveResources = receiveAgent.getResources();

            // Connect to the remote
            var connection = connectionManager.connect(negotiator, mtu, receiveAgent.getReceiveQueue(), sendResources, receiveResources);

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
    public Mono<Void> execute(Connection connection, Publisher<? extends Operation> publisher) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        return execute(internalConnection, publisher);
    }

    @Override
    public Mono<Void> send(Connection connection, Publisher<ByteBuf> frames) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Get associated send agent
        var agent = internalConnection.getSendAgent();

        // Send a single buffer
        if (frames instanceof Mono) {
            return execute(internalConnection, ((Mono<ByteBuf>) frames).map(agent::copyOf).map(SendOperation::new));
        }

        // Send a stream of buffers
        return execute(internalConnection, ((Flux<ByteBuf>) frames).map(agent::copyOf).map(SendOperation::new));
    }

    @Override
    public Mono<Void> sendDirect(Connection connection, Publisher<LocalHandle> publisher) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Send single message
        if (publisher instanceof Mono) {
            return execute(internalConnection, ((Mono<LocalHandle>) publisher).map(DirectSendOperation::new));
        }

        // Send stream of messages
        return execute(internalConnection, ((Flux<LocalHandle>) publisher).map(DirectSendOperation::new));
    }

    @Override
    public Flux<ByteBuf> receive(Connection connection) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Receive frames using agent
        return internalConnection.getInbound();
    }

    @Override
    public Mono<Void> write(Connection connection, LocalHandle localHandle, RemoteHandle remoteHandle) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Create write operation
        var operation = new WriteOperation(localHandle, remoteHandle);

        // Queue write operation within the associated agent
        return execute(internalConnection, Mono.just(operation));
    }

    @Override
    public Mono<Void> read(Connection connection, LocalHandle localHandle, RemoteHandle remoteHandle) {

        // Retrieve actual connection
        var internalConnection = connectionManager.get(connection);

        // Create write operation
        var operation = new ReadOperation(localHandle, remoteHandle);

        // Queue write operation within the associated agent
        return execute(internalConnection, Mono.just(operation));
    }

    private Mono<Void> execute(InternalConnection connection, Publisher<? extends Operation> publisher) {
        return Mono.defer(() -> {

            // Create a subscriber for operations
            var subscriber = new OperationSubscriber();

            // Add the subscriber to the connection
            connection.addSubscriber(subscriber);

            // Subscribe to the publisher's operations using our subscriber
            publisher.subscribe(subscriber);

            // Return a publisher which completes after all operations were acknowledged
            return subscriber.then();
        });
    }

    @Override
    public Flux<Connection> listen(InetSocketAddress serverAddress) {
        // TODO(krakowski):
        //  Use RDMA Communication Manager to listen for new connections
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Mono<Connection> connect(InetSocketAddress serverAddress) {
        // TODO(krakowski):
        //  Use RDMA Communication Manager to establish connection with server
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public InfinibandDevice getDevice() {
        return device;
    }
}
