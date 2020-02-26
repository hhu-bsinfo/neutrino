package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.util.FileDescriptor;
import de.hhu.bsinfo.neutrino.verbs.CompletionChannel;
import lombok.Value;

public @Value class ManagedChannel {

    private final CompletionChannel completionChannel;
    private final FileDescriptor fileDescriptor;

    public ManagedChannel(CompletionChannel completionChannel) {
        this.completionChannel = completionChannel;
        fileDescriptor = FileDescriptor.create(completionChannel.getFileDescriptor());
    }
}
