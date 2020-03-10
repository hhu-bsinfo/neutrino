package de.hhu.bsinfo.neutrino.api.network.impl.agent;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.api.network.impl.SharedResources;
import de.hhu.bsinfo.neutrino.api.network.impl.util.*;
import de.hhu.bsinfo.neutrino.api.network.impl.buffer.BufferPool;
import de.hhu.bsinfo.neutrino.verbs.SharedReceiveQueue;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@Slf4j
public class ReceiveAgent extends EpollAgent implements  NeutrinoInbound {

    private static final int MAX_CHANNELS = 128;

    private static final int EPOLL_TIMEOUT = 500;

    private static final int POLL_COUNT = 512;

    private static final AtomicLongFieldUpdater<ReceiveAgent> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(ReceiveAgent.class, "requested");

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<ReceiveAgent, CoreSubscriber> SUBSCRIBER =
            AtomicReferenceFieldUpdater.newUpdater(ReceiveAgent.class, CoreSubscriber.class, "subscriber");
    /**
     * Flux emitting received buffers.
     */
    private final FluxReceive inbound = new FluxReceive();

    /**
     * The shared receive queue.
     */
    private final SharedReceiveQueue receiveQueue;

    /**
     * The buffer pool used for receiving data.
     */
    private final BufferPool bufferPool;

    /**
     * The number of requested messages.
     */
    private volatile long requested;

    /**
     * Enabled if the subscriber requested Long.MAX_VALUE elements.
     */
    private volatile boolean isUnbounded;

    /**
     * The subscriber.
     */
    private volatile CoreSubscriber<ByteBuf> subscriber;

    /**
     * Helper object to fill up the receive queue.
     */
    private final QueueFiller queueFiller;

    /**
     * Helper object used to poll completion queues.
     */
    private final QueuePoller queuePoller;

    public ReceiveAgent(SharedResources resources) {
        super(ConnectionEvent.RECEIVE_READY);
        receiveQueue = resources.sharedReceiveQueue();
        bufferPool = resources.bufferPool();
        queuePoller = new QueuePoller(POLL_COUNT);
        queueFiller = new QueueFiller(resources.bufferPool(), receiveQueue.queryAttributes().getMaxWorkRequests());
        queueFiller.fillUp(receiveQueue);
    }

    @Override
    public Flux<ByteBuf> receive() {
        return inbound;
    }

    @Override
    protected void processConnection(InternalConnection connection, ConnectionEvent event) {

        // Get the completion channel
        var channel = connection.getResources().getReceiveCompletionChannel();

        // TODO(krakowski):
        //  Do we really need to get the completion queue when we already have it?
        //  var queue = connection.getResources().getReceiveCompletionQueue();

        // Get the completion queue
        var queue = channel.getCompletionEvent();

        // Acknowledge the received event and request subsequent notifications
        queue.acknowledgeEvent();
        queue.requestNotification();

        // Process the completion queue until it is empty. It is important to drain
        // the queue until there are no more elements left. Otherwise, a race condition
        // might occur in which no new events are generated.
        var processed = queuePoller.drain(queue, this::handleWorkCompletion);

        // Fill up the shared receive queue with new receive work requests
        if (processed != 0) {
            queueFiller.fillUp(receiveQueue, processed);
        }
    }

    @Override
    protected void onConnection(InternalConnection connection) { /* No-Op */ }

    private void handleWorkCompletion(WorkCompletion workCompletion) {

        // Get work completion id and status
        var id = workCompletion.getId();
        var status = workCompletion.getStatus();

        // Check if work completion was successful
        if (status != WorkCompletion.Status.SUCCESS) {
            log.error("Receive work completion {} failed with status {}: {}", (int) id, status, workCompletion.getStatusMessage());
            bufferPool.release((int) id);
            subscriber.onError(new RuntimeException(workCompletion.getStatusMessage()));
        }

        // Remember the number of bytes received
        var bytesReceived = workCompletion.getByteCount();

        // Advance receive buffer by number of bytes received
        var source = bufferPool.get((int) workCompletion.getId());
        source.writerIndex(source.writerIndex() + bytesReceived);

        // Publish received data
        subscriber.onNext(source);
    }

    @Override
    public String roleName() {
        return "receive";
    }

    @Override
    public void onStart() {
        log.info("Starting agent");
    }

    @Override
    public void onClose() {
        inbound.cancel();
        log.debug("Closing");
    }

    private class FluxReceive extends Flux<ByteBuf> implements Subscription {

        @Override
        public void request(long n) {
            if (isUnbounded) {
                return;
            }

            if (n == Long.MAX_VALUE) {
                isUnbounded = true;
                requested = Long.MAX_VALUE;
                return;
            }

            Operators.addCap(REQUESTED, ReceiveAgent.this, n);
        }


        @Override
        public void cancel() {
            if (subscriber != null) {
                subscriber.onComplete();
            }

            log.debug("Subscription has been cancelled");
        }

        @Override
        public void subscribe(CoreSubscriber<? super ByteBuf> subscriber) {
            boolean result = SUBSCRIBER.compareAndSet(ReceiveAgent.this, null, subscriber);
            if (result) {
                subscriber.onSubscribe(this);
            } else {
                Operators.error(subscriber, Exceptions.duplicateOnSubscribeException());
            }
        }
    }
}
