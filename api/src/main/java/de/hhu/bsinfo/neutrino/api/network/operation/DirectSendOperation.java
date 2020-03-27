package de.hhu.bsinfo.neutrino.api.network.operation;

import de.hhu.bsinfo.neutrino.api.network.LocalHandle;
import de.hhu.bsinfo.neutrino.api.network.impl.util.Identifier;
import de.hhu.bsinfo.neutrino.api.network.impl.util.RequestFlag;
import de.hhu.bsinfo.neutrino.util.BitMask;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.Value;

public @Value class DirectSendOperation implements Operation {

    private static final SendWorkRequest.SendFlag[] DEFAULT_SEND_FLAGS = { SendWorkRequest.SendFlag.SIGNALED };

    private static final short IDENTIFIER_FLAGS = BitMask.shortOf(RequestFlag.DIRECT);

    LocalHandle localHandle;

    @Override
    public void transfer(int context, SendWorkRequest request, ScatterGatherElement element) {

        // Set scatter-gather element if there is data to send
        if (localHandle != null) {

            // Set scatter-gather element metadata
            element.setAddress(localHandle.getAddress());
            element.setLength(localHandle.getLength());
            element.setLocalKey(localHandle.getKey());
            request.setScatterGatherElement(element);
        }

        // Set work request metadata
        request.setId(Identifier.create(context, IDENTIFIER_FLAGS));
        request.setOpCode(SendWorkRequest.OpCode.SEND);
        request.setSendFlags(DEFAULT_SEND_FLAGS);
    }
}
