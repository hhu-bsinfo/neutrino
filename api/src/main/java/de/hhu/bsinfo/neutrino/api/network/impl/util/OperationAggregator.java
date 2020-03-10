package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.operation.Operation;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

import java.util.function.Consumer;

public final class OperationAggregator implements Consumer<Operation> {

    private int index;

    private int currentId;

    private SendWorkRequest currentWorkRequest;

    private SendWorkRequest previousWorkRequest;

    private ScatterGatherElement currentElement;

    private final SendWorkRequest[] workRequestPool;

    private final ScatterGatherElement[] scatterGatherPool;

    private OperationAggregator(SendWorkRequest[] workRequestPool, ScatterGatherElement[] scatterGatherPool) {
        this.workRequestPool = workRequestPool;
        this.scatterGatherPool = scatterGatherPool;
    }

    @Override
    public void accept(Operation operation) {

        // Get current Work Request
        currentWorkRequest = workRequestPool[index];
        currentElement = scatterGatherPool[index];

        // Clear out old data
        currentWorkRequest.clear();
        currentElement.clear();

        // Transfer the operation into our send work request and scatter-gather element
        operation.transfer(currentId, currentWorkRequest, currentElement);

        // Link work requests
        if (previousWorkRequest != null) {
            previousWorkRequest.linkWith(currentWorkRequest);
        }

        // Increment index
        previousWorkRequest = currentWorkRequest;
        index++;
    }

    public void setCurrentId(int id) {
        currentId = id;
    }

    public void reset() {
        currentWorkRequest = null;
        previousWorkRequest = null;
        currentElement = null;
        index = 0;
        currentId = 0;
    }

    public int commit(QueuePair queuePair) {
        if (index == 0) {
            return 0;
        }

        currentWorkRequest.unlink();
        queuePair.postSend(workRequestPool[0]);
        return index;
    }

    public static OperationAggregator create(int capacity) {
        var builder = new SendWorkRequest.Builder()
                .withOpCode(SendWorkRequest.OpCode.SEND)
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED);

        var workRequestPool = new SendWorkRequest[capacity];
        var scatterGatherPool = new ScatterGatherElement[capacity];
        for (int i = 0; i < workRequestPool.length; i++) {
            workRequestPool[i] = builder.build();
            scatterGatherPool[i] = new ScatterGatherElement();
        }

        return new OperationAggregator(workRequestPool, scatterGatherPool);
    }
}
