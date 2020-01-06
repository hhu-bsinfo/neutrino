package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.message.impl.manager.CompletionManager;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.ValueFactory;
import de.hhu.bsinfo.neutrino.verbs.*;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;
import io.reactivex.Observable;

import javax.inject.Inject;
import java.io.IOError;
import java.io.IOException;

public class MessageServiceImpl extends Service<NullConfig> implements MessageService {

    @Inject
    private InternalConnectionService connectionService;

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public void send(Connection connection, NativeObject object) {
        var queuePair = connectionService.getQueuePair(connection);
        var sendBuffer = connectionService.getSendBuffer(connection);
        var completionQueue = connectionService.getCompletionQueue();
        sendBuffer.putObject(object);

        var element = new ScatterGatherElement(sendBuffer.getHandle(), (int) object.getNativeSize(), sendBuffer.getLocalKey());

        queuePair.postSend(new SendWorkRequest.MessageBuilder(OpCode.SEND, element).build());
    }

    @Override
    public <T extends NativeObject> Observable<T> receive(Connection connection, ValueFactory<T> factory) {
        final var sharedReceiveQueue = connectionService.getSharedReceiveQueue();
        final var queuePair = connectionService.getQueuePair(connection);
        final var receiveBuffer = connectionService.getReceiveBuffer(connection);
        final var completionQueue = connectionService.getCompletionQueue();
        final var completionArray = new CompletionQueue.WorkCompletionArray(100);
        return Observable.create(emitter -> {
            fillUp(queuePair, receiveBuffer);
            while (!emitter.isDisposed()) {
                completionQueue.poll(completionArray);
                for(int i = 0; i < completionArray.getLength(); i++) {
                    var workCompletion = completionArray.get(i);
                    if (workCompletion.getStatus() == WorkCompletion.Status.SUCCESS) {
                        var buffer = LocalBuffer.allocate(receiveBuffer.capacity());
                        buffer.putBuffer(receiveBuffer);
                        emitter.onNext(factory.newInstance(buffer, 0));
                    } else {
                        emitter.onError(new IOException("Request failed"));
                    }
                    fillUp(queuePair, receiveBuffer);
                }
            }
        });
    }

    private void fillUp(QueuePair queuePair, RegisteredBuffer receiveBuffer) {
        var element = new ScatterGatherElement(receiveBuffer.getHandle(), (int) receiveBuffer.capacity(), receiveBuffer.getLocalKey());

        queuePair.postReceive(new ReceiveWorkRequest.Builder().withScatterGatherElement(element).build());
    }
}
