package de.hhu.bsinfo.neutrino.api.connection.impl;

import de.hhu.bsinfo.neutrino.api.util.service.ServiceConfig;

import java.net.InetSocketAddress;

public class ConnectionServiceConfig extends ServiceConfig {

    private byte portNumber = 1;

    private byte serviceLevel = 1;

    private byte timeout = 14;

    private byte retryCount = 7;

    private byte rnrTimer = 12;

    private byte rnrRetryCount = 7;

    private int completionQueueSize = 100;

    private int receiveQueueSize = 100;

    private int maxScatterGatherElements = 1;

    private int connectionBufferSize = 1024 * 1024 * 2;

    public byte getRnrTimer() {
        return rnrTimer;
    }

    public byte getServiceLevel() {
        return serviceLevel;
    }

    public byte getTimeout() {
        return timeout;
    }

    public byte getRetryCount() {
        return retryCount;
    }

    public byte getRnrRetryCount() {
        return rnrRetryCount;
    }

    public int getCompletionQueueSize() {
        return completionQueueSize;
    }

    public int getMaxScatterGatherElements() {
        return maxScatterGatherElements;
    }

    public byte getPortNumber() {
        return portNumber;
    }

    public int getReceiveQueueSize() {
        return receiveQueueSize;
    }

    public int getConnectionBufferSize() {
        return connectionBufferSize;
    }
}
