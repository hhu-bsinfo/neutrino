package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class QueueFiller {

    private final BufferPool bufferPool;

    private final int capacity;

    public QueueFiller(BufferPool bufferPool, int capacity) {
        this.bufferPool = bufferPool;
        this.capacity = capacity;
    }

    public void fillUp(SharedReceiveQueue receiveQueue) {
        fillUp(receiveQueue, capacity);
    }

    public void fillUp(SharedReceiveQueue receiveQueue, int count) {

        // Return if no requests should be posted
        if (count == 0) {
            return;
        }

        // Get next receive buffer
        var buffer = bufferPool.claim();

        // Prepare send work request
        final var firstRequest = buffer.getReceiveRequest();
        var currentReceiveWorkRequest = firstRequest;

        // Prepare following send work requests
        var previousReceiveWorkRequest = currentReceiveWorkRequest;
        for (int i = 1; i < count; i++) {

            // Get next receive buffer
            buffer = bufferPool.claim();

            // Prepare send work request
            currentReceiveWorkRequest = buffer.getReceiveRequest();

            // Link previous request with the current one
            previousReceiveWorkRequest.linkWith(currentReceiveWorkRequest);
            previousReceiveWorkRequest = currentReceiveWorkRequest;
        }

        // Unlink current request to end the list
        currentReceiveWorkRequest.unlink();

        // Post linked work requests
        receiveQueue.postReceive(firstRequest);
    }
}
