package de.hhu.bsinfo.neutrino.api.network.impl.util;

import lombok.Value;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final @Value class SingleDrainable<T> implements Drainable<T> {

    private final T element;

    private final AtomicBoolean completed = new AtomicBoolean();

    @Override
    public int drain(Consumer<T> consumer) {
        consumer.accept(element);
        completed.set(true);
        return 1;
    }

    @Override
    public int drain(Consumer<T> consumer, int limit) {
        if (limit == 0) {
            return 0;
        }

        consumer.accept(element);
        completed.set(true);
        return 1;
    }

    @Override
    public boolean hasCompleted() {
        return completed.get();
    }
}
