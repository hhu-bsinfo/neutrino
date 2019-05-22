package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.NativeObjectFactory;
import de.hhu.bsinfo.neutrino.util.NativeObjectStore;
import java.util.function.Supplier;

public class SimplePool<T extends NativeObject> extends NativeObjectStore<T> implements NativeObjectFactory<T> {

    private final Supplier<T> supplier;

    public SimplePool(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void storeInstance(T instance) {}

    @Override
    public T getInstance() {
        return null;
    }

    @Override
    public T newInstance() {
        return supplier.get();
    }
}
