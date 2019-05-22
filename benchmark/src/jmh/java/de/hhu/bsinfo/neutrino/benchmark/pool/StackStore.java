package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.NativeObjectStore;
import java.util.Stack;

public class StackStore<T extends NativeObject> implements NativeObjectStore<T> {

    private final Stack<T> stack;

    public StackStore() {
        stack = new Stack<>();
    }

    @Override
    public final void storeInstance(T instance) {
        stack.push(instance);
    }

    @Override
    public T getInstance() {
        return stack.empty() ? null : stack.pop();
    }
}