package de.hhu.bsinfo.neutrino.api.network.impl.operation;

import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public final @Value class AtomicOperation implements Operation {


    @Override
    public void transfer(SendWorkRequest request, ScatterGatherElement element) {

    }
}
