package de.hhu.bsinfo.neutrino.api.network.impl.operation;

import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

@FunctionalInterface
public interface Operation {

    /**
     * Transfers this operation into a SendWorkRequest.
     */
    void transfer(int id, SendWorkRequest request, ScatterGatherElement element);
}
