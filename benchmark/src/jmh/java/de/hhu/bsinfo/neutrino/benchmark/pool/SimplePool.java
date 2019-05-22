package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.util.Pool;
import de.hhu.bsinfo.neutrino.util.Poolable;
import java.util.function.Supplier;

public class SimplePool<T extends Poolable> extends Pool<T> {

    public SimplePool(final Supplier<T> supplier) {
        super(supplier);
    }

    @Override
    public T getInstance() {
        return getSupplier().get();
    }

    @Override
    public void returnInstance(T instance) {

    }
}
