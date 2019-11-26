package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredByteBuf;
import de.hhu.bsinfo.neutrino.verbs.*;
import io.netty.buffer.ByteBuf;

import static de.hhu.bsinfo.neutrino.api.util.Assert.assertNotNull;

public class CoreServiceImpl extends Service<CoreServiceConfig> implements InternalCoreService {

    private Context context;
    private ProtectionDomain protectionDomain;
    private DeviceAttributes deviceAttributes;
    private PortAttributes portAttributes;
    private CompletionChannel completionChannel;

    private static final AccessFlag[] DEFAULT_ACCESS_FLAGS = {
            AccessFlag.LOCAL_WRITE,
            AccessFlag.REMOTE_READ,
            AccessFlag.REMOTE_WRITE,
            AccessFlag.MW_BIND
    };

    @Override
    protected void onInit(final CoreServiceConfig config) {
        context = assertNotNull(Context.openDevice(config.getDeviceNumber()), "Opening device context failed");
        deviceAttributes = assertNotNull(context.queryDevice(), "Querying device failed");
        portAttributes = assertNotNull(context.queryPort(config.getPortNumber()), "Querying device port failed");
        protectionDomain = assertNotNull(context.allocateProtectionDomain(), "Allocating protection domain failed");
        completionChannel = assertNotNull(context.createCompletionChannel(), "Creating completion channel failed");
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
    public RegisteredBuffer registerMemory(long capacity) {
        return protectionDomain.allocateMemory(capacity, DEFAULT_ACCESS_FLAGS);
    }

    @Override
    public RegisteredByteBuf registerBuffer(ByteBuf buffer) {
        if (!buffer.hasMemoryAddress()) {
            throw new IllegalArgumentException("buffer not direct");
        }

        var memoryAddress = buffer.memoryAddress();
        var memoryRegion = protectionDomain.registerMemoryRegion(memoryAddress, buffer.capacity(), DEFAULT_ACCESS_FLAGS);

        return new RegisteredByteBuf(buffer, memoryRegion);
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
        return context.createCompletionQueue(capacity, completionChannel);
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

    @Override
    public CompletionChannel getCompletionChannel() {
        return completionChannel;
    }
}
