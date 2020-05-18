package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.accessor.SendRequestAccessor;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;

import java.io.IOException;

@Slf4j
public final class RequestProcessor implements MessageHandler {

    /**
     * Helper object used for wrapping the first request.
     */
    private final SendWorkRequest request = new SendWorkRequest(0);

    private final ScatterGatherElement element = new ScatterGatherElement(0);

    /**
     * The first request's virtual memory address.
     */
    private long first;

    /**
     * The current request's virtual memory address.
     */
    private long current;

    /**
     * The number of processed elements.
     */
    private int count;

    /**
     * Resets this processors state.
     */
    public void reset() {
        first = 0;
        current = 0;
        count = 0;
    }

    /**
     * Commits the aggregated operations to the specified queue pair returning the number of commited operations.
     */
    public int commit(QueuePair queuePair) throws IOException {
        if (first == 0) {
            return 0;
        }

//        request.wrap(first);
//        while (request.getHandle() != 0) {
//            log.info("{}", request);
//            log.info("{}", element.wrap(request.getListHandle()));
//            request.wrap(request.getNext());
//        }

        queuePair.postSend(request.wrap(first));
        return count;
    }

    @Override
    public void onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
        // Calculate the request's virtual memory address
        var request = buffer.addressOffset() + index;

        // Remember the first request to post it later
        if (first == 0) {
            first = request;
            current = request;
            count = 1;
            return;
        }

        // Link the current element with the next element
        SendRequestAccessor.setNext(current, request);
        current = request;
        count++;
    }
}
