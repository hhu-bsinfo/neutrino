package de.hhu.bsinfo.neutrino.api.network.impl.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data class QueuePairState {

    /**
     * The queue pair's size.
     */
    private final int size;

    /**
     * Work requests pending within the queue pair.
     */
    private int pending;

    public int remaining() {
        return size - pending;
    }

    public void incrementPending(int value) {
        pending += value;
    }

    public void decrementPending(int value) {
        pending -= value;
    }
}
