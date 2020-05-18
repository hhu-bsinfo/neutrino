package de.hhu.bsinfo.neutrino.api.network;

import org.agrona.DirectBuffer;

public interface NetworkHandler {

    void onRequestCompleted(InfinibandChannel channel, int id);

    void onRequestFailed(InfinibandChannel channel, int id);

    void onMessage(InfinibandChannel channel, DirectBuffer buffer, int offset, int length);

}
