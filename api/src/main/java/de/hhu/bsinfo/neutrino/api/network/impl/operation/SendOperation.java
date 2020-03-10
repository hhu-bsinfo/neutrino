package de.hhu.bsinfo.neutrino.api.network.impl.operation;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public final @Value class SendOperation implements Operation {

    private final BufferPool.PooledByteBuf data;

    @Override
    public void transfer(int id, SendWorkRequest request, ScatterGatherElement element) {

        // Set scatter-gather element if there is data to send
        if (data != null) {

            // Remember number of bytes to send
            var messageSize = data.readableBytes();

            // Advance send buffer by number of written bytes
            var memoryAddress = data.memoryAddress() + data.readerIndex();
            data.readerIndex(data.writerIndex());

            // Set scatter-gather element metadata
            element.setAddress(memoryAddress);
            element.setLength(messageSize);
            element.setLocalKey(data.getLocalKey());
            request.setScatterGatherElement(element);
        }

        // Set work request metadata
        request.setId(Identifier.create(id, data.getIndex()));
        request.setOpCode(SendWorkRequest.OpCode.SEND);
        request.setSendFlags(SendWorkRequest.SendFlag.SIGNALED);
    }
}
