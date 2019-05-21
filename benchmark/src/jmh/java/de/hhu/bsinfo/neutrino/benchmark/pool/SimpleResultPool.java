package de.hhu.bsinfo.neutrino.benchmark.pool;

import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.util.ObjectPool;

public class SimpleResultPool implements ObjectPool<Result> {

    @Override
    public Result getInstance() {
        return new Result();
    }

    @Override
    public void returnInstance(Result result) {}
}
