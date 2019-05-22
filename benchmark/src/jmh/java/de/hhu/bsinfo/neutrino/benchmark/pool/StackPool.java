package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.NativeObjectFactory;
import java.util.Objects;
import java.util.function.Supplier;

public class StackPool<T extends NativeObject> extends StackStore<T> implements NativeObjectFactory<T> {

    private final Supplier<T> supplier;

    public StackPool(final int initialSize, final Supplier<T> supplier) {
        this.supplier = supplier;

        for(int i = 0; i < initialSize; i++) {
            storeInstance(supplier.get());
        }
    }

    @Override
    public T newInstance() {
        return Objects.requireNonNullElseGet(getInstance(), supplier);
    }
}
