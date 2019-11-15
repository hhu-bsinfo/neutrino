package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.api.connection.impl.ConnectionServiceConfig;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;

public final class QueuePairConnector {

    private static final ConnectionServiceConfig CONFIG = new ConnectionServiceConfig();

    private QueuePairConnector() {}

    public static void connect(final QueuePair queuePair, final QueuePairAddress remote) {
        queuePair.modify(QueuePair.Attributes.Builder
                .buildReadyToReceiveAttributesRC(remote.getQueuePairNumber(), remote.getLocalId(), remote.getPortNumber())
                .withPathMtu(Mtu.MTU_4096)
                .withReceivePacketNumber(0)
                .withMaxDestinationAtomicReads((byte) 1)
                .withMinRnrTimer(CONFIG.getRnrTimer())
                .withServiceLevel(CONFIG.getServiceLevel())
                .withSourcePathBits((byte) 0)
                .withIsGlobal(false));

        queuePair.modify(QueuePair.Attributes.Builder.buildReadyToSendAttributesRC()
                .withTimeout(CONFIG.getTimeout())
                .withRetryCount(CONFIG.getRetryCount())
                .withRnrRetryCount(CONFIG.getRnrRetryCount()));
    }
}
