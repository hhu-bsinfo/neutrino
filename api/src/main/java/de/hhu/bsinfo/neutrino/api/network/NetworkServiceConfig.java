package de.hhu.bsinfo.neutrino.api.network;

public interface NetworkServiceConfig {
    int getMtu();
    int getSharedReceiveQueueSize();
    int getMaxScatterGatherElements();
    int getCompletionQueueSize();
    int getQueuePairSize();

    byte getRnrTimer();
    byte getRnrRetryCount();

    byte getServiceLevel();
    byte getRetryCount();
    byte getTimeout();
}
