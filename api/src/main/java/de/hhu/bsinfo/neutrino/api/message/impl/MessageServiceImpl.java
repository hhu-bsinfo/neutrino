package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.message.impl.manager.CompletionManager;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest.OpCode;

import javax.inject.Inject;

public class MessageServiceImpl extends Service<NullConfig> implements MessageService {

    @Inject
    private InternalConnectionService connectionService;

    private CompletionManager completionManager;

    @Override
    protected void onInit(final NullConfig config) {

        completionManager = new CompletionManager(connectionService.getCompletionQueue());
    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public void send(Connection connection, NativeObject object) {
        var queuePair = connectionService.getQueuePair(connection);
        var sendBuffer = connectionService.getBuffer(connection);
        sendBuffer.putObject(object);

        var element = new ScatterGatherElement(configurator -> {
            configurator.setAddress(sendBuffer.getHandle());
            configurator.setLength((int) object.getNativeSize());
            configurator.setLocalKey(sendBuffer.getLocalKey());
        });

        queuePair.postSend(new SendWorkRequest(configurator -> {
            configurator.setOpCode(OpCode.SEND);
            configurator.setListHandle(element.getHandle());
            configurator.setListLength(1);
        }));
    }
}
