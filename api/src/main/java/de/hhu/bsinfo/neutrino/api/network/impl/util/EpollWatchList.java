package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.network.impl.InternalConnection;
import de.hhu.bsinfo.neutrino.util.Epoll;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;

@Slf4j
@NotThreadSafe
public class EpollWatchList  {

    public enum Mode {
        SEND, RECEIVE
    }

    private static final long NOTIFIER_DATA = -1;

    private final Epoll epoll;

    private final Epoll.EventArray events;

    private final InternalConnection[] connections;

    private final EventFileDescriptor notifier;

    private final Mode mode;

    private int index = 0;

    public EpollWatchList(int capacity, Mode mode) {
        epoll = Epoll.create(capacity);
        notifier = EventFileDescriptor.create(EventFileDescriptor.OpenMode.NONBLOCK);
        events = new Epoll.EventArray(capacity);
        connections = new InternalConnection[capacity];
        this.mode = mode;

        // Register notifier so that we can wake up this watch list when needed
        epoll.add(notifier, NOTIFIER_DATA, Epoll.EventType.EPOLLIN);
    }

    public void add(InternalConnection connection) {

        // Add channel to array of known channels
        connections[index] = connection;

        // Get file descriptor corresponding to the configured mode
        FileDescriptor fileDescriptor = null;
        switch (mode) {
            case RECEIVE:
                fileDescriptor = connection.getResources().getReceiveFileDescriptor();
                break;
            case SEND:
                fileDescriptor = connection.getResources().getSendFileDescriptor();
                break;
        }

        // Add channel to epoll watch list
        epoll.add(fileDescriptor, index, Epoll.EventType.EPOLLIN);

        // Increment channel index
        index++;
    }

    public void wake() {
        notifier.increment();
    }

    public int forEach(int timeout, Consumer<InternalConnection> operation) {

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

            // Get the associated completion channel
            var connection = connections[(int) data];

            // Perform operation on the completion channel
            operation.accept(connection);
        });

        return events.getLength();
    }
}
