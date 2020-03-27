package de.hhu.bsinfo.neutrino.api.network.operation;

import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public @Value class AtomicOperation implements Operation {

    @Override
    public void transfer(int context, SendWorkRequest request, ScatterGatherElement element) {
        throw new UnsupportedOperationException("not implemented");
    }
}
