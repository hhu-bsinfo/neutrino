package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.DeviceAttributes;
import de.hhu.bsinfo.neutrino.verbs.PortAttributes;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;

import java.util.function.Consumer;

import static de.hhu.bsinfo.neutrino.api.util.Assert.assertNotNull;

public class CoreServiceImpl extends Service<CoreServiceConfig> implements InternalCoreService {

    private Context context;
    private ProtectionDomain protectionDomain;
    private DeviceAttributes deviceAttributes;
    private PortAttributes portAttributes;

    @Override
    protected void onInit(final CoreServiceConfig config) {
        context = assertNotNull(Context.openDevice(config.getDeviceNumber()), "Opening device context failed");
        deviceAttributes = assertNotNull(context.queryDevice(), "Querying device failed");
        portAttributes = assertNotNull(context.queryPort(config.getPortNumber()), "Querying device port failed");
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
    public PortAttributes getPortAttributes() {
        return portAttributes;
    }

    @Override
    public DeviceAttributes getDeviceAttributes() {
        return deviceAttributes;
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes) {
        return protectionDomain.createQueuePair(initialAttributes);
    }

    @Override
    public CompletionQueue createCompletionQueue(int capacity) {
        return context.createCompletionQueue(capacity);
    }

    @Override
    public SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes) {
        return protectionDomain.createSharedReceiveQueue(initialAttributes);
    }

    @Override
    public short getLocalId() {
        return portAttributes.getLocalId();
    }

    @Override
    public RegisteredBuffer allocateMemory(long capacity, AccessFlag... flags) {
        return protectionDomain.allocateMemory(capacity, flags);
    }
}
