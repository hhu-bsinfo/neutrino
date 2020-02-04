package de.hhu.bsinfo.neutrino.api.event;

import lombok.extern.slf4j.Slf4j;
import org.agrona.CloseHelper;
import org.agrona.concurrent.DynamicCompositeAgent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.hints.ThreadHints;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class EventLoopGroup implements AutoCloseable {

    /**
     * The runtime used by the current application.
     */
    private static final Runtime RUNTIME = Runtime.getRuntime();

    /**
     * The event loops contained within this group.
     */
    private final EventLoop[] eventLoops;

    /**
     * The index used for retrieving the next event loop.
     */
    private final AtomicInteger index = new AtomicInteger();

    public EventLoopGroup(String name, Supplier<IdleStrategy> idleStrategySupplier) {
        this(name, RUNTIME.availableProcessors(), idleStrategySupplier);
    }

    public EventLoopGroup(String name, int workerCount, Supplier<IdleStrategy> idleStrategySupplier) {
        log.debug("Using {} worker threads with idle strategy {}", workerCount, idleStrategySupplier.get());
        eventLoops = new EventLoop[workerCount];
        for (int i = 0; i < workerCount; i++) {
            eventLoops[i] = new EventLoop(name + "-" + i, idleStrategySupplier.get());
        }
    }

    public EventLoop next() {
        return eventLoops[Math.floorMod(index.getAndIncrement(), eventLoops.length)];
    }

    public int size() {
        return eventLoops.length;
    }

    public void join() throws InterruptedException {
        for (EventLoop loop: eventLoops) {
            loop.join();
        }
    }

    public void waitOnStart() {
        for (EventLoop loop: eventLoops) {
            while (loop.status() != DynamicCompositeAgent.Status.ACTIVE) {
                ThreadHints.onSpinWait();
            }
        }
    }

    @Override
    public void close() {
        CloseHelper.quietCloseAll(eventLoops);
    }
}
