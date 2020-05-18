package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import lombok.Data;
import org.agrona.DirectBuffer;

import java.util.concurrent.atomic.AtomicInteger;

public @Data class InfinibandChannel {

    /**
     * The connection's id.
     */
    private final int id;

    /**
     * The network service.
     */
    private final NetworkService network;

    public final void send(int id, DirectBuffer buffer, int offset, int length) {
        network.send(this, id, buffer, offset, length);
    }

    public final void read(int id, RemoteHandle handle, RegisteredBuffer buffer, int offset, int length) {
        network.read(this, id, handle, buffer, offset, length);
    }

    public final void write(int id, RegisteredBuffer buffer, int offset, int length, RemoteHandle handle) {
        network.write(this, id, buffer, offset, length, handle);
    }
}
