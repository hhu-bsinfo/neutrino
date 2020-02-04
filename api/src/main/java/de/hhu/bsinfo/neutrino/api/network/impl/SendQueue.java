package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;

import java.util.Objects;

public class SendQueue {

    /**
     * The send completion queue.
     */
    private final CompletionQueue completionQueue;

    /**
     * The send completion channel.
     */
    private final CompletionChannel completionChannel;

    /**
     * The completion channel's file descriptor.
     */
    private final FileDescriptor fd;

    public SendQueue(InfinibandDevice device, NetworkConfiguration config) {

        completionChannel = Objects.requireNonNull(
                device.createCompletionChannel(),
                "Creating send completion channel failed"
        );

        completionQueue = Objects.requireNonNull(
                device.createCompletionQueue(config.getCompletionQueueSize(), completionChannel),
                "Creating send completion queue failed"
        );

        fd = FileDescriptor.create(completionChannel.getFileDescriptor());
        fd.setMode(FileDescriptor.OpenMode.NONBLOCK);
    }

    public void poll(CompletionQueue.WorkCompletionArray completions) {
        completionQueue.poll(completions);
    }

}
