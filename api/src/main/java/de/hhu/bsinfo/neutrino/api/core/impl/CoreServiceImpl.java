package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.*;

import java.util.function.Consumer;

import static de.hhu.bsinfo.neutrino.api.util.Assert.assertNotNull;

public class CoreServiceImpl extends Service<CoreServiceConfig> implements InternalCoreService {

    private Context context;
    private Port port;
    private ProtectionDomain protectionDomain;
    private DeviceAttributes deviceAttributes;

    @Override
    protected void onInit(final CoreServiceConfig config) {
        context = assertNotNull(Context.openDevice(config.getDeviceNumber()), "Opening device context failed");
        deviceAttributes = assertNotNull(context.queryDevice(), "Querying device failed");
        port = assertNotNull(context.queryPort(config.getPortNumber()), "Querying device port failed");
        protectionDomain = assertNotNull(context.allocateProtectionDomain(), "Allocating protection domain failed");
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
