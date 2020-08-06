package de.hhu.bsinfo.neutrino.verbs.panama;

import static org.linux.rdma.ibverbs_h.*;

import de.hhu.bsinfo.neutrino.verbs.panama.util.Struct;
import jdk.incubator.foreign.MemoryAddress;

public final class CompletionChannel extends Struct {

    public CompletionChannel() {
        super(ibv_comp_channel.allocate());
    }

    public CompletionChannel(MemoryAddress address) {
        super(address, ibv_comp_channel.$LAYOUT());
    }

    public MemoryAddress getContext() {
        return ibv_comp_channel.context$get(segment());
    }

    public int getFileDescriptor() {
        return ibv_comp_channel.fd$get(segment());
    }

    public int getReferenceCount() {
        return ibv_comp_channel.refcnt$get(segment());
    }

    public void setContext(final MemoryAddress value) {
        ibv_comp_channel.context$set(segment(), value);
    }

    public void setFileDescriptor(final int value) {
        ibv_comp_channel.fd$set(segment(), value);
    }

    public void setReferenceCount(final int value) {
        ibv_comp_channel.refcnt$set(segment(), value);
    }
}