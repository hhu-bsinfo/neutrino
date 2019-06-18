package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.connection.InternalConnectionService;
import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.message.impl.manager.CompletionManager;
import de.hhu.bsinfo.neutrino.api.message.impl.processor.QueueProcessor;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MessageServiceImpl extends Service<NullConfig> implements MessageService {

    @Inject
    private InternalConnectionService connectionService;

    private CompletionManager completionManager;

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }
}
