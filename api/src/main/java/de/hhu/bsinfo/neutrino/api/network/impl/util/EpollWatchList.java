package de.hhu.bsinfo.neutrino.api.network.impl.util;

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

    private static final long NOTIFIER_DATA = -1;

    private final Epoll epoll;

    private final Epoll.EventArray events;

    private final ManagedChannel[] channels;

    private final EventFileDescriptor notifier;

    private int channelIndex = 0;

    public EpollWatchList(int capacity) {
        epoll = Epoll.create(capacity);
        notifier = EventFileDescriptor.create(EventFileDescriptor.OpenMode.NONBLOCK);
        events = new Epoll.EventArray(capacity);
        channels = new ManagedChannel[capacity];

        // Register notifier so that we can wake up this watch list when needed
        epoll.add(notifier, NOTIFIER_DATA, Epoll.EventType.EPOLLIN);
    }

    public void add(CompletionChannel channel) {

        // Add channel to array of known channels
        var managedChannel = new ManagedChannel(channel);
        channels[channelIndex] = managedChannel;

        // Set completion channel mode to NONBLOCK
        var descriptor = managedChannel.getFileDescriptor();
        descriptor.setMode(FileDescriptor.OpenMode.NONBLOCK);

        // Add channel to epoll watch list
        epoll.add(descriptor, channelIndex, Epoll.EventType.EPOLLIN);

        // Increment channel index
        channelIndex++;
    }

    public void wake() {
        notifier.increment();
    }

    public int forEach(int timeout, Consumer<CompletionChannel> operation) {

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
            var channel = channels[(int) data].getCompletionChannel();

            // Perform operation on the completion channel
            operation.accept(channel);
        });

        return events.getLength();
    }
}
