package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.operation.Operation;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

import java.util.function.Consumer;

public final class OperationAggregator implements Consumer<Operation> {

    /**
     * The current index.
     */
    private int index;

    /**
     * The current identifier.
     */
    private int currentIdentifier;

    /**
     * The current work request.
     */
    private SendWorkRequest currentWorkRequest;

    /**
     * The previous work request.
     */
    private SendWorkRequest previousWorkRequest;

    /**
     * The current scatter-gather element.
     */
    private ScatterGatherElement currentElement;

    /**
     * The pool of work requests.
     */
    private final SendWorkRequest[] workRequestPool;

    /**
     * The pool of scatter-gather elements.
     */
    private final ScatterGatherElement[] scatterGatherPool;

    /**
     * This aggregator's capacity.
     */
    private final int capacity;

    private OperationAggregator(SendWorkRequest[] workRequestPool, ScatterGatherElement[] scatterGatherPool) {
        if (workRequestPool.length != scatterGatherPool.length) {
            throw new IllegalArgumentException("Pools must have same size");
        }

        this.workRequestPool = workRequestPool;
        this.scatterGatherPool = scatterGatherPool;
        capacity = workRequestPool.length;
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
        operation.transfer(currentIdentifier, currentWorkRequest, currentElement);

        // Link work requests
        if (previousWorkRequest != null) {
            previousWorkRequest.linkWith(currentWorkRequest);
        }

        // Increment index
        previousWorkRequest = currentWorkRequest;
        index++;
    }

    /**
     * Sets the identifier used for subsequent operations.
     */
    public void setCurrentIdentifier(int id) {
        currentIdentifier = id;
    }

    /**
     * Resets this aggregators state.
     */
    public void reset() {
        currentWorkRequest = null;
        previousWorkRequest = null;
        currentElement = null;
        index = 0;
        currentIdentifier = 0;
    }

    /**
     * Commits the aggregated operations to the specified queue pair returning the number of commited operations.
     */
    public int commit(QueuePair queuePair) {
        if (index == 0) {
            return 0;
        }

        currentWorkRequest.unlink();
        queuePair.postSend(workRequestPool[0]);
        return index;
    }

    /**
     * Returns the remaining space within this aggregator.
     */
    public int remaining() {
        return capacity - index;
    }

    /**
     * Creates a new aggregator with the specified capacity.
     */
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
