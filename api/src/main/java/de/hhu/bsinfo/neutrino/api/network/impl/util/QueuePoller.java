package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import org.agrona.BitUtil;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.function.BiConsumer;
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

    public int drain(CompletionQueue completionQueue, InternalConnection connection, BiConsumer<InternalConnection, WorkCompletion> operation) throws IOException {

        // Remember how many work completions we processed in total
        var processed = 0;

        do { // Poll completion queue until no work completions are left
            processed += poll(completionQueue, connection, operation);
        } while (!completions.isEmpty());

        // Return number of processed work completions
        return processed;
    }

    public int poll(CompletionQueue completionQueue, InternalConnection connection, BiConsumer<InternalConnection, WorkCompletion> operation) throws IOException {

        // Poll the completion queue
        completionQueue.poll(completions);

        // Iterate over all work completions
        var length = completions.getLength();

        // Fill up receive queue

        for (int i = 0; i < length; i++) {
            operation.accept(connection, completions.get(i));
        }

        // Return number of processed work completions
        return length;
    }

    public CompletionQueue.WorkCompletionArray poll(CompletionQueue completionQueue) throws IOException {
        completionQueue.poll(completions);
        return completions;
    }
}
