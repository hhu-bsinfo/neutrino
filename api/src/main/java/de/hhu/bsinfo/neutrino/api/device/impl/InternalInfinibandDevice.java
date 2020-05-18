package de.hhu.bsinfo.neutrino.api.device.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.device.InfinibandDeviceConfig;
import de.hhu.bsinfo.neutrino.api.util.MemoryUtil;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.api.util.UnsafeRegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.verbs.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class InternalInfinibandDevice implements InfinibandDevice {

    /**
     * The device's context.
     */
    private final Context context;

    /**
     * The device's port attributes.
     */
    private final PortAttributes portAttributes;

    /**
     * The device's device attributes.
     */
    private final DeviceAttributes deviceAttributes;

    /**
     * The protection domain used by this InfiniBand Device.
     */
    private final ProtectionDomain protectionDomain;

    public InternalInfinibandDevice(InfinibandDeviceConfig config) throws IOException {
        context = Context.openDevice(config.getDeviceNumber());
        deviceAttributes = context.queryDevice();
        portAttributes = context.queryPort(config.getPortNumber());
        protectionDomain = context.allocateProtectionDomain();
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
    public RegisteredBuffer allocateMemory(int capacity) throws IOException {
        return allocateMemory(capacity, MemoryAlignment.CACHE);
    }

    @Override
    public RegisteredBuffer allocateMemory(int capacity, MemoryAlignment alignment) throws IOException {
        var memory = MemoryUtil.allocateAligned(capacity, alignment);
        var region = wrapRegion(memory.addressOffset(), memory.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
        return new UnsafeRegisteredBuffer(memory, region);
    }

    @Override
    public MemoryRegion wrapRegion(long handle, long capacity, AccessFlag... accessFlags) throws IOException {
        return protectionDomain.registerMemoryRegion(handle, capacity, accessFlags);
    }

    @Override
    public QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes) throws IOException {
        return protectionDomain.createQueuePair(initialAttributes);
    }

    @Override
    public SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes) throws IOException {
        return protectionDomain.createSharedReceiveQueue(initialAttributes);
    }

    @Override
    public CompletionQueue createCompletionQueue(int capacity, @Nullable CompletionChannel channel) throws IOException {
        return context.createCompletionQueue(capacity, channel);
    }

    @Override
    public CompletionChannel createCompletionChannel() throws IOException {
        return context.createCompletionChannel();
    }

    @Override
    public AsyncEvent getAsyncEvent() throws IOException {
        return context.getAsyncEvent();
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public ThreadDomain createThreadDomain(ThreadDomain.InitialAttributes attributes) throws IOException {
        return context.allocateThreadDomain(attributes);
    }
}
