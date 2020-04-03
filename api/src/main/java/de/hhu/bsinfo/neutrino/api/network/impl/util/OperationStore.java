package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.subscriber.OperationSubscriber;
import org.agrona.collections.ArrayUtil;
import org.agrona.collections.Int2ObjectHashMap;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@NotThreadSafe
public final class OperationStore {

    /**
     * Map used to retrieve subscribers based on their identifiers.
     */
    private final Int2ObjectHashMap<OperationSubscriber> subscriberMap = new Int2ObjectHashMap<>();

    /**
     * An array of active subscribers.
     */
    private volatile OperationSubscriber[] subscribers = new OperationSubscriber[0];
    private static final AtomicReferenceFieldUpdater<OperationStore, OperationSubscriber[]> SUBSCRIBERS =
            AtomicReferenceFieldUpdater.newUpdater(OperationStore.class, OperationSubscriber[].class, "subscribers");

    /**
     * Adds a new operation subscriber to this store.
     */
    public void addSubscriber(OperationSubscriber subscriber) {
        OperationSubscriber[] oldArray;
        OperationSubscriber[] newArray;

        do {
            oldArray = subscribers;
            newArray = ArrayUtil.add(oldArray, subscriber);
        } while (!SUBSCRIBERS.compareAndSet(this, oldArray, newArray));

        subscriberMap.put(subscriber.getId(), subscriber);
    }

    /**
     * Removes an operation subscriber from the array of actives subscribers.
     */
    public void onCompletion(OperationSubscriber subscriber) {
        OperationSubscriber[] oldArray;
        OperationSubscriber[] newArray;

        do {
            oldArray = subscribers;
            newArray = ArrayUtil.remove(oldArray, subscriber);
        } while (!SUBSCRIBERS.compareAndSet(this, oldArray, newArray));
    }

    /**
     * Removes the mapping associated with the specified operation subscriber.
     */
    public void remove(OperationSubscriber subscriber) {
        subscriberMap.remove(subscriber.getId(), subscriber);
    }

    /**
     * Returns the operation subscriber with the specified identifier.
     */
    public OperationSubscriber get(int id) {
        return subscriberMap.get(id);
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public OperationSubscriber[] getSubscribers() {
        return subscribers;
    }
}
