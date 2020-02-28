package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.util.Epoll;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@NotThreadSafe
public class EpollWatchList {

    public enum Mode {
        SEND, RECEIVE
    }

    private static final long NOTIFIER_DATA = -1;

    private final Epoll epoll;

    private final Epoll.EventArray events;

    private final InternalConnection[] connections;

    private final EventFileDescriptor notifier;

    private int index = 0;

    public EpollWatchList(int capacity) {
        epoll = Epoll.create(capacity);
        notifier = EventFileDescriptor.create(EventFileDescriptor.OpenMode.NONBLOCK);
        events = new Epoll.EventArray(capacity);
        connections = new InternalConnection[capacity];

        // Register notifier so that we can wake up this watch list when needed
        epoll.add(notifier, NOTIFIER_DATA, Epoll.EventType.EPOLLIN);
    }

    public void add(InternalConnection connection, EpollEvent... events) {

        // Add connection to array of known connections
        connections[index] = connection;

        for (var event : events) {
            switch (event) {
                case SEND_READY:
                    epoll.add(connection.getResources().getSendFileDescriptor(), toData(event, index), Epoll.EventType.EPOLLIN);
                    break;
                case RECEIVE_READY:
                    epoll.add(connection.getResources().getReceiveFileDescriptor(), toData(event, index), Epoll.EventType.EPOLLIN);
                    break;
                case QUEUE_READY:
                    epoll.add(connection.getQueueFileDescriptor(), toData(event, index), Epoll.EventType.EPOLLIN);
                    break;
            }
        }

        // Increment channel index
        index++;
    }

    public void wake() {
        notifier.increment();
    }

    public int forEach(int timeout, BiConsumer<InternalConnection, EpollEvent> operation) {

        // Wait for events
        epoll.wait(events, timeout);

        events.forEach(events.getLength(), event -> {

            // Get event data
            var data = event.getData();

            // Reset the notifier if it was triggered
            if (data == NOTIFIER_DATA) {
                notifier.reset();
                return;
            }

            // Get the associated connection
            var connection = connections[getIndex(data)];

            // Perform operation on the connection
            operation.accept(connection, getEvent(data));
        });

        return events.getLength();
    }

    private static long toData(EpollEvent event, int index) {
        return (long) event.toInt() << 32 | index;
    }

    private static int getIndex(long data) {
        return (int) data;
    }

    private static EpollEvent getEvent(long data) {
        return EpollEvent.fromInt((int) (data >> 32));
    }
}
