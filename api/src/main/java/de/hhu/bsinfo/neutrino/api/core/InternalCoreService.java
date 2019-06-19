package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.*;

import java.util.function.Consumer;

public interface InternalCoreService extends CoreService {

    Context getContext();

    ProtectionDomain getProtectionDomain();

    QueuePair createQueuePair(QueuePair.InitialAttributes initialAttributes);

    CompletionQueue createCompletionQueue(int capacity);

    SharedReceiveQueue createSharedReceiveQueue(SharedReceiveQueue.InitialAttributes initialAttributes);

    RegisteredBuffer allocateMemory(long capacity, AccessFlag... flags);

    short getLocalId();
}
