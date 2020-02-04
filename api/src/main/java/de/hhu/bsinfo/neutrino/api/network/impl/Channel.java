package de.hhu.bsinfo.neutrino.api.network.impl;

import de.hhu.bsinfo.neutrino.verbs.QueuePair;

public class Channel {

    /**
     * This channels unique identifier.
     */
    private int id;

    /**
     * The queue pair used by this channel.
     */
    private QueuePair queuePair;

    private SendQueue sendQueue;

    private ReceiveQueue receiveQueue;
}
