package de.hhu.bsinfo.neutrino.api.connection.impl.buffer;

import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BufferPool {

    private final Map<Connection, RegisteredBuffer> buffers = new ConcurrentHashMap<>();
    private final Supplier<RegisteredBuffer> bufferSupplier;

    public BufferPool(Supplier<RegisteredBuffer> bufferSupplier) {
        this.bufferSupplier = bufferSupplier;
    }

    public RegisteredBuffer get(final Connection connection) {
        return buffers.computeIfAbsent(connection, key -> bufferSupplier.get());
    }
}
