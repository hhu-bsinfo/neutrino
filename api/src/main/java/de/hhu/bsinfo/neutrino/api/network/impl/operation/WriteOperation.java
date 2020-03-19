package de.hhu.bsinfo.neutrino.api.network.impl.operation;

import de.hhu.bsinfo.neutrino.api.network.LocalHandle;
import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public final @Value class WriteOperation implements Operation {

    private final LocalHandle localHandle;

    private final RemoteHandle remoteHandle;

    @Override
    public void transfer(int id, SendWorkRequest request, ScatterGatherElement element) {
        element.setAddress(localHandle.getAddress());
        element.setLength(localHandle.getLength());
        element.setLocalKey(localHandle.getKey());

        // TODO(krakowski)
        //  Encode operation type and identifier within work request id
        request.setId(Identifier.create(id, 0));
        request.setScatterGatherElement(element);
        request.setOpCode(SendWorkRequest.OpCode.RDMA_WRITE);
        request.setSendFlags(SendWorkRequest.SendFlag.SIGNALED);
        request.rdma.setRemoteAddress(remoteHandle.getAddress());
        request.rdma.setRemoteAddress(remoteHandle.getKey());
    }
}
