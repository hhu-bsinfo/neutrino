package de.hhu.bsinfo.neutrino.api.event;

import lombok.extern.slf4j.Slf4j;
import org.agrona.CloseHelper;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.DynamicCompositeAgent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.hints.ThreadHints;

@Slf4j
public final class EventLoop implements AutoCloseable {

    /**
     * The composite agent performing work for this event loop.
     */
    private final DynamicCompositeAgent compositeAgent;

    /**
     * The runner used by this event loop.
     */
    private final AgentRunner runner;

    /**
     * The thread on which this event loop is run.
     */
    private final Thread thread;

    public EventLoop(String name, IdleStrategy idleStrategy) {
        compositeAgent = new DynamicCompositeAgent(name);
        runner = new AgentRunner(idleStrategy, EventLoop::errorHandler, null, compositeAgent);
        thread = AgentRunner.startOnThread(runner);
    }

    public void add(Agent agent) {
        while (!compositeAgent.tryAdd(agent)) {
            ThreadHints.onSpinWait();
        }
    }

    public void join(int timeout) throws InterruptedException {
        thread.join(timeout);
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    public DynamicCompositeAgent.Status status() {
        return compositeAgent.status();
    }

    @Override
    public void close() {
        CloseHelper.quietClose(runner);
    }

    private static void errorHandler(Throwable throwable) {
        log.error("Encountered unexpected error", throwable);
    }
}
