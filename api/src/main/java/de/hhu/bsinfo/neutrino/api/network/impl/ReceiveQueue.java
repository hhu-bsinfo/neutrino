package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;

import java.util.Objects;

public class ReceiveQueue {

    /**
     * The completion queue.
     */
    private final CompletionQueue completionQueue;

    /**
     * The completion channel.
     */
    private final CompletionChannel completionChannel;

    /**
     * The completion channel's file descriptor.
     */
    private FileDescriptor fd;


    public ReceiveQueue(InfinibandDevice device, NetworkConfiguration config) {

        completionChannel = Objects.requireNonNull(
                device.createCompletionChannel(),
                "Creating receive completion channel failed"
        );

        completionQueue = Objects.requireNonNull(
                device.createCompletionQueue(config.getCompletionQueueSize(), completionChannel),
                "Creating receive completion queue failed"
        );
    }
}
