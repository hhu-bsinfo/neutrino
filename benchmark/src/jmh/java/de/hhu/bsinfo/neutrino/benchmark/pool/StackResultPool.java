package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;
import java.util.Stack;

public class StackResultPool implements ObjectPool<Result> {

    private final Stack<Result> stack = new Stack<>();

    public StackResultPool(final int initialSize) {
        for(int i = 0; i < initialSize; i++) {
            stack.push(new Result());
        }
    }

    @Override
    public Result getInstance() {
        return stack.isEmpty() ? new Result() : stack.pop();
    }

    @Override
    public void returnInstance(Result result) {
        stack.push(result);
    }
}
