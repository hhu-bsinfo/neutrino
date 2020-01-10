package de.hhu.bsinfo.neutrino.api.device.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.AsyncEvent;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.DeviceAttributes;
import de.hhu.bsinfo.neutrino.verbs.MemoryRegion;
import de.hhu.bsinfo.neutrino.verbs.PortAttributes;
import de.hhu.bsinfo.neutrino.verbs.ProtectionDomain;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Slf4j
@Component
public class InfinibandDeviceImpl implements InfinibandDevice {

    private final Context context;
    private final PortAttributes portAttributes;
    private final DeviceAttributes deviceAttributes;
    private final ProtectionDomain protectionDomain;

    public InfinibandDeviceImpl(InfinibandDeviceConfig config) {
        context = Objects.requireNonNull(Context.openDevice(config.getDeviceNumber()), "Opening device context failed");
        deviceAttributes = Objects.requireNonNull(context.queryDevice(), "Querying device failed");
        portAttributes = Objects.requireNonNull(context.queryPort(config.getPortNumber()), "Querying device port failed");
        protectionDomain = Objects.requireNonNull(context.allocateProtectionDomain(), "Allocating protection domain failed");
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
    public RegisteredBuffer allocateMemory(long capacity, AccessFlag... accessFlags) {
        return protectionDomain.allocateMemory(capacity, accessFlags);
    }

    @Override
    public MemoryRegion wrapRegion(long handle, long capacity, AccessFlag... accessFlags) {
        return protectionDomain.registerMemoryRegion(handle, capacity, accessFlags);
    }

    @Override
    public QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes) {
        return protectionDomain.createQueuePair(initialAttributes);
    }

    @Override
    public SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes) {
        return protectionDomain.createSharedReceiveQueue(initialAttributes);
    }

    @Override
    public CompletionQueue createCompletionQueue(int capacity, @Nullable CompletionChannel channel) {
        return context.createCompletionQueue(capacity, channel);
    }

    @Override
    public CompletionChannel createCompletionChannel() {
        return context.createCompletionChannel();
    }

    @Override
    public AsyncEvent getAsyncEvent() {
        return context.getAsyncEvent();
    }
}
