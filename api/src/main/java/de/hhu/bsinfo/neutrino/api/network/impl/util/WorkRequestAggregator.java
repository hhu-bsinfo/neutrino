package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

import java.util.function.Consumer;

public class WorkRequestAggregator implements Consumer<BufferPool.PooledBuffer> {

    private int index;

    private SendWorkRequest currentWorkRequest;

    private SendWorkRequest previousWorkRequest;

    private ScatterGatherElement currentElement;

    private final SendWorkRequest[] workRequestPool;

    private final ScatterGatherElement[] scatterGatherPool;

    public WorkRequestAggregator(int capacity) {
        var builder = new SendWorkRequest.Builder()
                .withOpCode(SendWorkRequest.OpCode.SEND)
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED);

        workRequestPool = new SendWorkRequest[capacity];
        scatterGatherPool = new ScatterGatherElement[capacity];
        for (int i = 0; i < workRequestPool.length; i++) {
            workRequestPool[i] = builder.build();
            scatterGatherPool[i] = new ScatterGatherElement();
        }
    }

    @Override
    public void accept(BufferPool.PooledBuffer data) {
        currentWorkRequest = workRequestPool[index];
        currentElement = scatterGatherPool[index];

        // Remember number of bytes to send
        var messageSize = data.readableBytes();

        // Advance send buffer by number of written bytes
        var memoryAddress = data.memoryAddress() + data.readerIndex();
        data.readerIndex(data.writerIndex());

        currentElement.setAddress(memoryAddress);
        currentElement.setLength(messageSize);
        currentElement.setLocalKey(data.getLocalKey());

        currentWorkRequest.setId(data.getIdentifier());
        currentWorkRequest.setListHandle(currentElement.getHandle());
        currentWorkRequest.setListLength(1);

        if (previousWorkRequest != null) {
            previousWorkRequest.linkWith(currentWorkRequest);
        }

        previousWorkRequest = currentWorkRequest;
        index++;
    }

    public void reset() {
        currentWorkRequest = null;
        previousWorkRequest = null;
        currentElement = null;
        index = 0;
    }

    public int commit(QueuePair queuePair) {
        if (index == 0) {
            return 0;
        }

        currentWorkRequest.unlink();
        queuePair.postSend(workRequestPool[0]);
        return index;
    }
}
