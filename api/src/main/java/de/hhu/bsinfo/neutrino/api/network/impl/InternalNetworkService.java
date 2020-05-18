package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.network.*;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.event.EventLoopGroup;
import de.hhu.bsinfo.neutrino.api.util.BaseService;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.ThreadDomain;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public InternalNetworkService(InfinibandDevice device, InfinibandDeviceConfig deviceConfig, NetworkConfiguration networkConfig, MeterRegistry meterRegistry) throws IOException {
        super(networkConfig);

        this.device = device;
        this.deviceConfig = deviceConfig;
        this.metrics = new NetworkMetrics(meterRegistry);

        // Calculate how many workers should be created
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        log.debug("{} available processors detected", availableProcessors);

        var sendWorkerCount = networkConfig.getSendWorker() == 0 ? availableProcessors / 2 : networkConfig.getSendWorker();
        var receiveWorkerCount = networkConfig.getReceiveWorker() == 0 ? availableProcessors / 2 : networkConfig.getReceiveWorker();

        // Create event loop groups
        sendGroup = new EventLoopGroup<>("send", sendWorkerCount, () -> BusySpinIdleStrategy.INSTANCE);
        receiveGroup = new EventLoopGroup<>("receive", receiveWorkerCount, () -> BusySpinIdleStrategy.INSTANCE);

        // Check if Thread Domains are supported
        ThreadDomain threadDomain = null;
        var attributes = new ThreadDomain.InitialAttributes();

        try {
            threadDomain = device.createThreadDomain(attributes);
        } catch (IOException e) {
            log.warn("Thread domains are not supported, falling back to global protection domain!");
        } finally {
            if (threadDomain != null) {
                threadDomain.close();
            }
        }

        // Create shared resources for other network components
        sharedResources = SharedResources.builder()
                .device(device)
                .deviceConfig(deviceConfig)
                .networkConfig(networkConfig)
                .networkMetrics(metrics)
                .meterRegistry(meterRegistry)
                .build();

        // Initialize connection manager
        connectionManager = new ConnectionManager(sharedResources);
    }

    @Override
    protected void onStart() throws Exception {

        // Wait on event loop groups to start
        log.debug("Waiting on event loops to start");
        List.of(sendGroup, receiveGroup).forEach(EventLoopGroup::waitOnStart);
        log.debug("Event loops started");

        // Register receive agents within event loop group
        for (int i = 0; i < receiveGroup.size(); i++) {
            receiveGroup.next().add(new ReceiveAgent(i, sharedResources));
        }

        // Register receive agent within event loop group
        for (int i = 0; i < sendGroup.size(); i++) {
            sendGroup.next().add(new SendAgent(i, sharedResources));
        }
    }

    @Override
    protected void onDestroy() {
        log.info("Schutting down");
    }

    public InfinibandChannel listen(InetSocketAddress serverAddress) {
        // TODO(krakowski):
        //  Use RDMA Communication Manager to listen for new connections
        throw new UnsupportedOperationException("not implemented");
    }

    public InfinibandChannel connect(InetSocketAddress serverAddress) {
        // TODO(krakowski):
        //  Use RDMA Communication Manager to establish connection with server
        throw new UnsupportedOperationException("not implemented");
    }

    public void send(InfinibandChannel channel, int id, DirectBuffer buffer, int offset, int length) {

        // Retrieve the actual connection
        var internalConnection = connectionManager.get(channel);

        // Send the data using the actual connection
        internalConnection.send(id, buffer, offset, length);
    }

    @Override
    public void read(InfinibandChannel channel, int id, RemoteHandle handle, RegisteredBuffer buffer, int offset, int length) {

        // Retrieve the actual connection
        var internalConnection = connectionManager.get(channel);

        // Send the data using the actual connection
        internalConnection.read(id, handle, buffer, offset, length);
    }

    @Override
    public void write(InfinibandChannel channel, int id, RegisteredBuffer buffer, int offset, int length, RemoteHandle handle) {

        // Retrieve the actual connection
        var internalConnection = connectionManager.get(channel);

        // Send the data using the actual connection
        internalConnection.write(id, buffer, offset, length, handle);
    }

    @Override
    public InfinibandDevice getDevice() {
        return device;
    }

    @Override
    public InfinibandChannel connect(Negotiator negotiator, NetworkHandler networkHandler, Mtu mtu) throws IOException {
        // Get next send and receive agent
        var sendAgent = sendGroup.next().getAgent();
        var receiveAgent = receiveGroup.next().getAgent();

        // Connect to the remote
        var connection = connectionManager.connect(
                negotiator,
                mtu,
                networkHandler,
                receiveAgent.getReceiveQueue(),
                sendAgent.getResources(),
                this
        );

        // Assign send agent to connection
        connection.setSendAgent(sendAgent);
        sendAgent.add(connection);

        // Assign receive agent to connection
        connection.setReceiveAgent(receiveAgent);
        receiveAgent.add(connection);

        // Return channel
        return connection.getChannel();
    }
}
