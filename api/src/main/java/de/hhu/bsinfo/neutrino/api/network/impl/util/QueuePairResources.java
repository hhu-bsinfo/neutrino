package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.NetworkConfiguration;
import de.hhu.bsinfo.neutrino.util.EventFileDescriptor;
import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import lombok.Data;

public @Data class QueuePairResources {

    private final CompletionQueue sendCompletionQueue;

    private final CompletionChannel sendCompletionChannel;

    private final FileDescriptor sendFileDescriptor;

    private final CompletionQueue receiveCompletionQueue;

    private final CompletionChannel receiveCompletionChannel;

    private final FileDescriptor receiveFileDescriptor;

    public static QueuePairResources create(InfinibandDevice device, NetworkConfiguration config) {

        var sendCompletionChannel = device.createCompletionChannel();
        var sendCompletionQueue = device.createCompletionQueue(config.getCompletionQueueSize(), sendCompletionChannel);
        var sendFileDescriptor = FileDescriptor.create(sendCompletionChannel.getFileDescriptor());
        sendFileDescriptor.setMode(FileDescriptor.OpenMode.NONBLOCK);
        sendCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);

        var receiveCompletionChannel = device.createCompletionChannel();
        var receiveCompletionQueue = device.createCompletionQueue(config.getCompletionQueueSize(), receiveCompletionChannel);
        var receiveFileDescriptor = FileDescriptor.create(receiveCompletionChannel.getFileDescriptor());
        receiveFileDescriptor.setMode(FileDescriptor.OpenMode.NONBLOCK);
        receiveCompletionQueue.requestNotification(CompletionQueue.ALL_EVENTS);

        return new QueuePairResources(
                sendCompletionQueue, sendCompletionChannel, sendFileDescriptor,
                receiveCompletionQueue, receiveCompletionChannel, receiveFileDescriptor
        );
    }
}
