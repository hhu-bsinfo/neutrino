package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.network.InfinibandChannel;
import de.hhu.bsinfo.neutrino.api.network.NetworkHandler;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.ReceiveAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.agent.SendAgent;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.RequestBuffer;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.QueuePairState;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@Slf4j
@Builder
public final @Data class InternalConnection {

    /**
     * This connection's unique identifier.
     */
    private final int id;

    /**
     * The queue pair's local id.
     */
    private final short localId;

    /**
     * The queue pair's port number.
     */
    private final byte portNumber;

    /**
     * The queue pair used by this connection.
     */
    private final QueuePair queuePair;

    /**
     * The associated queue pair's resources.
     */
    private final QueuePairResources resources;

    /**
     * The associated queue pair's state.
     */
    private final QueuePairState state;

    /**
     * Used for signaling free slots on the queue pair.
     */
    private final EventFileDescriptor queueFileDescriptor;

    /**
     * A ring buffer used for storing requests.
     */
    private final RequestBuffer requestBuffer = new RequestBuffer(128 * MemoryAlignment.PAGE.value());

    /**
     * The network handler associated with this connection.
     */
    private final NetworkHandler networkHandler;

    /**
     * The channel associated with this connection.
     */
    private final InfinibandChannel channel;

    /**
     * The send agent assigned to this connection.
     */
    private volatile SendAgent sendAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, SendAgent> SEND_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, SendAgent.class, "sendAgent");

    /**
     * The receive agent assigned to this connection.
     */
    private volatile ReceiveAgent receiveAgent;
    private static final AtomicReferenceFieldUpdater<InternalConnection, ReceiveAgent> RECEIVE_AGENT =
            AtomicReferenceFieldUpdater.newUpdater(InternalConnection.class, ReceiveAgent.class, "receiveAgent");

    /**
     * Assigns a send agent to this connection.
     */
    public void setSendAgent(SendAgent agent) {
        if (!SEND_AGENT.compareAndSet(this, null, agent)) {
            throw new IllegalStateException("Send agent already set");
        }
    }

    /**
     * Assigns a receive agent to this connection.
     */
    public void setReceiveAgent(ReceiveAgent agent) {
        if (!RECEIVE_AGENT.compareAndSet(this, null, agent)) {
            throw new IllegalStateException("Receive agent already set");
        }
    }

    public void send(int id, DirectBuffer buffer, int offset, int length) {
        Requests.appendSend(id, sendAgent.claim(), requestBuffer, buffer, offset, length);
    }

    public void read(int id, RemoteHandle handle, RegisteredBuffer buffer, int offset, int length) {
        Requests.appendRead(id, requestBuffer, handle, buffer, offset, length);
    }

    public void write(int id, RegisteredBuffer buffer, int offset, int length, RemoteHandle handle) {
        Requests.appendWrite(id, requestBuffer, buffer, offset, length, handle);
    }


    public long freeSlots() {
        return queueFileDescriptor.read();
    }

    public void freeSlots(long value) {
        queueFileDescriptor.increment(value);
    }


}
