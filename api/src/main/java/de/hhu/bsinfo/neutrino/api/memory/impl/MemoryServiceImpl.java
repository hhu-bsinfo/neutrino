package de.hhu.bsinfo.neutrino.api.memory.impl;

import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.memory.RemoteHandle;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

import javax.inject.Inject;

public class MemoryServiceImpl extends Service<NullConfig> implements MemoryService {

    @Inject
    private InternalCoreService coreService;

    @Inject
    private InternalConnectionService connectionService;

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public void read(RemoteHandle source, RegisteredBuffer target) {
        execute(SendWorkRequest.OpCode.RDMA_READ, source, target);
    }

    @Override
    public void read(RemoteHandle source, long sourceOffset, RegisteredBuffer target, long targetOffset, long length) {
        execute(SendWorkRequest.OpCode.RDMA_READ, sourceOffset, source, target, targetOffset, length);
    }

    @Override
    public void write(RegisteredBuffer source, RemoteHandle target) {
        execute(SendWorkRequest.OpCode.RDMA_WRITE, target, source);
    }

    @Override
    public void write(RegisteredBuffer source, long sourceOffset, RemoteHandle target, long targetOffset, long length) {
        execute(SendWorkRequest.OpCode.RDMA_WRITE, sourceOffset, target, source, targetOffset, length);
    }

    private void execute(final SendWorkRequest.OpCode operation, RemoteHandle remoteHandle, RegisteredBuffer buffer) {
        execute(operation, 0, remoteHandle, buffer, 0, buffer.capacity());
    }

    private void execute(final SendWorkRequest.OpCode operation, long index, RemoteHandle remoteHandle, RegisteredBuffer local, long offset, long length) {
        var queuePair = connectionService.getQueuePair(remoteHandle.getConnection());

        var elements = local.split(offset, length);
        var request = new SendWorkRequest.RdmaBuilder(operation, elements,
                remoteHandle.getAddress() + index, remoteHandle.getKey())
                .build();

        queuePair.postSend(request);
    }
}
