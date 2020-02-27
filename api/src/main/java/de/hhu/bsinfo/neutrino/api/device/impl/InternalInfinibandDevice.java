package de.hhu.bsinfo.neutrino.api.device.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
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
public class InternalInfinibandDevice implements InfinibandDevice {

    private final Context context;
    private final PortAttributes portAttributes;
    private final DeviceAttributes deviceAttributes;
    private final ProtectionDomain protectionDomain;

    public InternalInfinibandDevice(InfinibandDeviceConfig config) {
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
    public Buffer allocateMemory(int capacity) {
        return Buffer.allocate(capacity, this::wrapRegion);
    }

    @Override
    public MemoryRegion wrapRegion(long handle, long capacity, AccessFlag... accessFlags) {
        return Objects.requireNonNull(protectionDomain.registerMemoryRegion(handle, capacity, accessFlags), "Registering memory region failed");
    }

    @Override
    public QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes) {
        return Objects.requireNonNull(protectionDomain.createQueuePair(initialAttributes), "Creating queue pair failed");
    }

    @Override
    public SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes) {
        return Objects.requireNonNull(protectionDomain.createSharedReceiveQueue(initialAttributes), "Creating shared receive queue failed");
    }

    @Override
    public CompletionQueue createCompletionQueue(int capacity, @Nullable CompletionChannel channel) {
        return Objects.requireNonNull(context.createCompletionQueue(capacity, channel), "Creating completion queue failed");
    }

    @Override
    public CompletionChannel createCompletionChannel() {
        return Objects.requireNonNull(context.createCompletionChannel(), "Creating completion channel failed");
    }

    @Override
    public AsyncEvent getAsyncEvent() {
        return context.getAsyncEvent();
    }
}
