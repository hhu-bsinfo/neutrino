package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.util.Pool;
import de.hhu.bsinfo.neutrino.util.Poolable;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Supplier;

public class StackPool<T extends Poolable> extends Pool<T> {

    private final Stack<T> stack;

    public StackPool(final int initialSize, final Supplier<T> supplier) {
        super(supplier);

        stack = new Stack<>();

        for(int i = 0; i < initialSize; i++) {
            stack.push(supplier.get());
        }
    }

    @Override
    public T getInstance() {
        return Objects.requireNonNullElseGet(stack.pop(), getSupplier());
    }

    @Override
    public void returnInstance(T instance) {
        stack.push(instance);
    }
}
