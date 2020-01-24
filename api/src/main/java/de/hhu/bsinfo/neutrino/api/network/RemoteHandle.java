package de.hhu.bsinfo.neutrino.api.network;

public interface RemoteHandle {

    Connection getConnection();

    long getAddress();

    int getCapacity();

    int getRemoteKey();
}
