package de.hhu.bsinfo.neutrino.api.message.impl;

import de.hhu.bsinfo.neutrino.api.message.MessageService;
import de.hhu.bsinfo.neutrino.api.util.NullConfig;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageServiceImpl extends Service<NullConfig> implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Override
    protected void onInit(final NullConfig config) {

    }

    @Override
    protected void onShutdown() {

    }
}
