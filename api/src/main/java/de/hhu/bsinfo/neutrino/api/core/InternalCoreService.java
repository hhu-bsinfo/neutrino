package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.verbs.*;

import java.util.function.Consumer;

public interface InternalCoreService extends CoreService {

    Context getContext();

    Port getPort();

    ProtectionDomain getProtectionDomain();

    QueuePair createQueuePair(Consumer<QueuePair.InitialAttributes> configurator);

    CompletionQueue createCompletionQueue(int capacity);

    SharedReceiveQueue createSharedReceiveQueue(Consumer<SharedReceiveQueue.InitialAttributes> configurator);

    short getLocalId();
}
