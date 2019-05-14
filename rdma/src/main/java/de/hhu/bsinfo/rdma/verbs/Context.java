package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.Result;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

    static {
        System.loadLibrary("rdma");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Context.class);

    private final long handle;

    private Context(long handle) {
        this.handle = handle;
    }

    @Nullable
    public static Context openDevice(int index) {
        Result result = new Result();
        Verbs.openDevice(index, result.getHandle());
        if (result.isError()) {
            LOGGER.error("Could not open device with index {}", index);
            return null;
        }

        return new Context(result.resultHandle.get());
    }

    @Nullable
    public Device queryDevice() {
        Result result = new Result();
        Device device = new Device();
        Verbs.queryDevice(handle, device.getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Could not query device");
            return null;
        }

        return device;
    }
}
