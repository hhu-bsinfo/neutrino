package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class CoreServiceImpl extends Service<CoreServiceConfig> implements CoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreServiceImpl.class);

    private Context context;
    private Port port;
    private ProtectionDomain protectionDomain;

    @Override
    protected void onInit(final CoreServiceConfig config) {
        context = Context.openDevice(config.getDeviceNumber());
        if (context == null) {
            throw new InitializationException("Opening device context failed");
        }

        port = context.queryPort(config.getPortNumber());
        if (port == null) {
            throw new InitializationException("Querying device port failed");
        }

        protectionDomain = context.allocateProtectionDomain();
        if (protectionDomain == null) {
            throw new InitializationException("Allocating protection domain failed");
        }
    }

    @Override
    protected void onShutdown() {
        context.close();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public Port getPort() {
        return port;
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public QueuePair createQueuePair(Consumer<QueuePair.InitialAttributes> configurator) {
        return protectionDomain.createQueuePair(configurator);
    }

    @Override
    public CompletionQueue createCompletionQueue(int capacity) {
        return context.createCompletionQueue(capacity);
    }

    @Override
    public SharedReceiveQueue createSharedReceiveQueue(Consumer<SharedReceiveQueue.InitialAttributes> configurator) {
        return protectionDomain.createSharedReceiveQueue(configurator);
    }

    @Override
    public short getLocalId() {
        return port.getLocalId();
    }

    @Override
    public RegisteredBuffer allocateMemory(long capacity, AccessFlag... flags) {
        return protectionDomain.allocateMemory(capacity, flags);
    }


}
