package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class QueueFiller {

    private final ReceiveWorkRequest[] workRequestPool;

    private final ScatterGatherElement[] scatterGatherPool;

    private final BufferPool bufferPool;

    private final int capacity;

    public QueueFiller(BufferPool bufferPool, int capacity) {
        var builder = new ReceiveWorkRequest.Builder();
        workRequestPool = new ReceiveWorkRequest[capacity];
        scatterGatherPool = new ScatterGatherElement[capacity];
        for (int i = 0; i < workRequestPool.length; i++) {
            workRequestPool[i] = builder.build();
            scatterGatherPool[i] = new ScatterGatherElement();
        }

        this.bufferPool = bufferPool;
        this.capacity = capacity;
    }

    public void fillUp(SharedReceiveQueue receiveQueue) {
        fillUp(receiveQueue, capacity);
    }

    public void fillUp(SharedReceiveQueue receiveQueue, int count) {

        // Get next receive buffer
        var buffer = bufferPool.leaseNext();

        // Prepare send work request
        var currentReceiveWorkRequest = workRequestPool[0];
        var currentElement = scatterGatherPool[0];
        fillScatterGatherElement(currentElement, buffer);
        fillReceiveWorkRequest(currentReceiveWorkRequest, currentElement.getHandle(), buffer.getIndex());

        // Post send work request immediately if
        // no more requests should be posted
        if (count == 1) {

            // Unlink current request to end the list
            currentReceiveWorkRequest.unlink();

            // Post single work requests
            receiveQueue.postReceive(currentReceiveWorkRequest);
            return;
        }

        // Prepare following send work requests
        var previousReceiveWorkRequest = currentReceiveWorkRequest;
        for (int i = 1; i < count; i++) {

            // Get next receive buffer
            buffer = bufferPool.leaseNext();

            // Prepare send work request
            currentReceiveWorkRequest = workRequestPool[i];
            currentElement = scatterGatherPool[i];
            fillScatterGatherElement(currentElement, buffer);
            fillReceiveWorkRequest(currentReceiveWorkRequest, currentElement.getHandle(), buffer.getIndex());

            // Link previous request with the current one
            previousReceiveWorkRequest.linkWith(currentReceiveWorkRequest);
            previousReceiveWorkRequest = currentReceiveWorkRequest;
        }

        // Unlink current request to end the list
        currentReceiveWorkRequest.unlink();

        // Post linked work requests
        receiveQueue.postReceive(workRequestPool[0]);
    }

    private static void fillScatterGatherElement(ScatterGatherElement element, BufferPool.IndexedByteBuf buffer) {
        element.setAddress(buffer.memoryAddress());
        element.setLength(buffer.writableBytes());
        element.setLocalKey(buffer.getLocalKey());
    }

    private static void fillReceiveWorkRequest(ReceiveWorkRequest request, final long listHandle, final int id) {
        request.setId(id);
        request.setListHandle(listHandle);
        request.setListLength(1);
    }
}
