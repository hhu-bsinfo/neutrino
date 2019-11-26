package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.*;

import java.util.function.Consumer;

public interface InternalCoreService extends CoreService {

    /**
     * The {@link Context} belonging to the current infiniband device.
     */
    Context getContext();

    /**
     * The {@link ProtectionDomain} belonging to the current infiniband device.
     */
    ProtectionDomain getProtectionDomain();

    /**
     * Creates a new {@link QueuePair} using the provided {@link QueuePair.InitialAttributes}.
     */
    QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes);

    /**
     * Creates a new {@link CompletionQueue} with the specified maximum capacity.
     */
    CompletionQueue createCompletionQueue(int capacity);

    /**
     * Creates a new {@link SharedReceiveQueue} using the provided {@link SharedReceiveQueue.InitialAttributes}.
     */
    SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes);

    /**
     * Creates a new {@link RegisteredBuffer} used for RDMA operations.
     */
    RegisteredBuffer allocateMemory(long capacity, AccessFlag... flags);

    /**
     * The {@link CompletionChannel} associated with all {@link QueuePair} instances.
     */
    CompletionChannel getCompletionChannel();
}
