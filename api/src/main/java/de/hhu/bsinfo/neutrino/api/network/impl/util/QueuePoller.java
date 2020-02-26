package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import org.agrona.BitUtil;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;

@NotThreadSafe
public class QueuePoller {

    private final CompletionQueue.WorkCompletionArray completions;

    public QueuePoller(int capacity) {
        var size = BitUtil.findNextPositivePowerOfTwo(capacity);
        completions = new CompletionQueue.WorkCompletionArray(size);
    }

    public int capacity() {
        return completions.getCapacity();
    }

    public int drain(CompletionQueue completionQueue, Consumer<WorkCompletion> operation) {
        var processed = 0;
        do {
            processed += poll(completionQueue, operation);
        } while (!completions.isEmpty());

        return processed;
    }

    public int poll(CompletionQueue completionQueue, Consumer<WorkCompletion> operation) {
        completionQueue.poll(completions);
        completions.forEach(operation);
        return completions.getLength();
    }
}
