package de.hhu.bsinfo.neutrino.verbs;

import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.struct.Result;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Context implements NativeObject, AutoCloseable {

    static {
        System.loadLibrary("neutrino");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Context.class);

    private final long handle;

    @SuppressWarnings("FieldNamingConvention")
    private static final long nullptr = 0L;

    Context(long handle) {
        this.handle = handle;
    }

    @Override
    public long getHandle() {
        return handle;
    }

    @Override
    public long getNativeSize() {
        return -1;
    }

    @Nullable
    public static Context openDevice(int index) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.openDevice(index, result.getHandle());
        if (result.isError()) {
            LOGGER.error("Opening device {} failed with error [{}]", index, result.getStatus());
        }

        return result.getAndRelease(Context::new);
    }

    @Override
    public void close() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.closeDevice(getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Closing device failed with error [{}]", result.getStatus());
        }

        result.releaseInstance();
    }

    public String getDeviceName() {
        return Verbs.getDeviceName(getHandle());
    }

    @Nullable
    public Device queryDevice() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);
        var device = new Device();

        Verbs.queryDevice(getHandle(), device.getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Querying device failed with error [{}]", result.getStatus());
            device = null;
        }

        result.releaseInstance();

        return device;
    }

    @Nullable
    public Port queryPort(int portNumber) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);
        var port = new Port();

        Verbs.queryPort(getHandle(), port.getHandle(), portNumber, result.getHandle());
        if (result.isError()) {
            LOGGER.error("Querying port failed with error [{}]", result.getStatus());
            port = null;
        }

        result.releaseInstance();

        return port;
    }

    @Nullable
    public ProtectionDomain allocateProtectionDomain() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.allocateProtectionDomain(getHandle(), result.getHandle());
        if(result.isError()) {
            LOGGER.error("Allocating protection domain failed with error [{}]", result.getStatus());
        }

        return result.getAndRelease(ProtectionDomain::new);
    }

    @Nullable
    public CompletionChannel createCompletionChannel() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.createCompletionChannel(getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Creating completion channel failed with error [{}]", result.getStatus());
        }

        return result.getAndRelease(CompletionChannel::new);
    }

    @Nullable
    public CompletionQueue createCompletionQueue(int numElements, @Nullable CompletionChannel channel) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.createCompletionQueue(getHandle(), numElements, nullptr, channel == null ? nullptr : channel.getHandle(), 0, result.getHandle());
        if (result.isError()) {
            LOGGER.error("Creating completion queue failed with error [{}]", result.getStatus());
        }

        return result.getAndRelease(CompletionQueue::new);
    }

    @Nullable
    public ExtendedCompletionQueue createExtendedCompletionQueue(ExtendedCompletionQueue.InitialAttributes initialAttributes) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.createExtendedCompletionQueue(getHandle(), initialAttributes.getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Creating extended completion queue failed with error [{}]", result.getStatus());
        }

        return result.getAndRelease(ExtendedCompletionQueue::new);
    }
}
