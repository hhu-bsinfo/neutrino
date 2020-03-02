package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.util.Epoll;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.BiConsumer;

@Slf4j
@NotThreadSafe
public class EpollWatchList<T, S> {

    private static final long NOTIFIER_DATA = -1;

    private final Epoll epoll;

    private final Epoll.EventArray events;

    private final T[] objects;

    private final S[] attachments;

    private final EventFileDescriptor notifier;

    private int index = 0;

    @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
    public EpollWatchList(int capacity) {

        // Create a new epoll instance and preallocate an array of epoll events
        epoll = Epoll.create(capacity);
        events = new Epoll.EventArray(capacity);

        // Create arrays for watchable objects
        objects = (T[]) new Object[capacity];
        attachments = (S[]) new Object[capacity];

        // Register notifier so that we can wake up this watch list when needed
        notifier = EventFileDescriptor.create(EventFileDescriptor.OpenMode.NONBLOCK);
        epoll.add(notifier, NOTIFIER_DATA, Epoll.EventType.EPOLLIN);
    }

    public void add(FileDescriptor descriptor, T object, S attachement, Epoll.EventType... eventTypes) {

        // Add object and attachement to our array
        objects[index] = object;
        attachments[index] = attachement;

        // Add file descriptor to our epoll instance
        epoll.add(descriptor, index, eventTypes);

        // Increment index
        index++;
    }

    public void wake() {
        notifier.increment();
    }

    public int forEach(int timeout, BiConsumer<T, S> operation) {

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

            // Get the associated object and attachement
            var eventIndex = getIndex(data);
            var object = objects[eventIndex];
            var attachement = attachments[eventIndex];

            // Perform operation on the connection
            operation.accept(object, attachement);
        });

        return events.getLength();
    }

    private static int getIndex(long data) {
        return (int) data;
    }
}
