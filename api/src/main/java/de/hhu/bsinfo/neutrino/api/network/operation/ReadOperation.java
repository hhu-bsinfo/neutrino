package de.hhu.bsinfo.neutrino.api.network.operation;

import de.hhu.bsinfo.neutrino.api.network.LocalHandle;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public final @Value class ReadOperation implements Operation {

    private static final short FLAGS = 0;

    LocalHandle localHandle;

    RemoteHandle remoteHandle;

    @Override
    public void transfer(int context, SendWorkRequest request, ScatterGatherElement element) {

        // Set scatter-gather element properties
        element.setAddress(localHandle.getAddress());
        element.setLength(localHandle.getLength());
        element.setLocalKey(localHandle.getKey());

        // Set work request properties
        request.setId(Identifier.create(context, FLAGS));
        request.setScatterGatherElement(element);
        request.setOpCode(SendWorkRequest.OpCode.RDMA_READ);
        request.setSendFlags(SendWorkRequest.SendFlag.SIGNALED);
        request.rdma.setRemoteAddress(remoteHandle.getAddress());
        request.rdma.setRemoteKey(remoteHandle.getKey());
    }
}
