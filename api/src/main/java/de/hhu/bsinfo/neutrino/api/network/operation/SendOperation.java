package de.hhu.bsinfo.neutrino.api.network.operation;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public @Value class SendOperation implements Operation {

    private static final SendWorkRequest.SendFlag[] DEFAULT_SEND_FLAGS = { SendWorkRequest.SendFlag.SIGNALED };

    private static final short IDENTIFIER_FLAGS = 0;

    BufferPool.PooledBuffer data;

    @Override
    public void transfer(int context, SendWorkRequest request, ScatterGatherElement element) {

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
        request.setId(Identifier.create(context, IDENTIFIER_FLAGS));
        request.setOpCode(SendWorkRequest.OpCode.SEND);
        request.setSendFlags(DEFAULT_SEND_FLAGS);
    }
}
