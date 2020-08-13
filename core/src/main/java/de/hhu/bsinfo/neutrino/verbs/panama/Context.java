package de.hhu.bsinfo.neutrino.verbs.panama;

import de.hhu.bsinfo.neutrino.verbs.panama.util.Struct;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import org.linux.rdma.ibverbs_h;

import static org.linux.rdma.ibverbs_h.ibv_context;

public final class Context extends Struct {

    public Context() {
        super(ibv_context.allocate());
    }

    public Context(MemoryAddress address) {
        super(address, ibv_context.$LAYOUT());
    }

    public MemoryAddress getDevice() {
        return ibv_context.device$get(segment());
    }

    public MemorySegment getOps() {
        return ibv_context.ops$addr(segment());
    }

    public int getCommandFileDescriptor() {
        return ibv_context.cmd_fd$get(segment());
    }

    public int getAsyncFileDescriptor() {
        return ibv_context.async_fd$get(segment());
    }

    public int getCompatibilityVectorCount() {
        return ibv_context.num_comp_vectors$get(segment());
    }

    public MemorySegment getMutex() {
        return ibv_context.mutex$addr(segment());
    }

    public MemoryAddress getAbiCompatibility() {
        return ibv_context.abi_compat$get(segment());
    }

    public void setDevice(final MemoryAddress value) {
        ibv_context.device$set(segment(), value);
    }

    public void setCommandFileDescriptor(final int value) {
        ibv_context.cmd_fd$set(segment(), value);
    }

    public void setAsyncFileDescriptor(final int value) {
        ibv_context.async_fd$set(segment(), value);
    }

    public void setCompatibilityVectorCount(final int value) {
        ibv_context.num_comp_vectors$set(segment(), value);
    }

    public void setAbiCompatibility(final MemoryAddress value) {
        ibv_context.abi_compat$set(segment(), value);
    }
}
