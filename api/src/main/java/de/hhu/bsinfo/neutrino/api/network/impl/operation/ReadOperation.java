package de.hhu.bsinfo.neutrino.api.network.impl.operation;

import de.hhu.bsinfo.neutrino.api.network.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.util.Buffer;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final @Value class ReadOperation implements Operation {

    private final Buffer buffer;

    private final RemoteHandle handle;

    @Override
    public void transfer(SendWorkRequest request, ScatterGatherElement element) {
        element.setAddress(buffer.memoryAddress() + buffer.readerIndex());
        element.setLength(buffer.writableBytes());
        element.setLocalKey(buffer.localKey());

        // TODO(krakowski)
        //  Encode operation type and identifier within work request id
        request.setId(0);
        request.setScatterGatherElement(element);
        request.setOpCode(SendWorkRequest.OpCode.RDMA_READ);
        request.setSendFlags(SendWorkRequest.SendFlag.SIGNALED);
        request.rdma.setRemoteAddress(handle.getAddress());
        request.rdma.setRemoteKey(handle.getKey());
    }
}
