package de.hhu.bsinfo.neutrino.api.memory;

import de.hhu.bsinfo.neutrino.api.network.Connection;

public class RemoteHandle {

    private final Connection connection;

    private final long address;
    private final long capacity;
    private final int key;

    public RemoteHandle(Connection connection, long address, long capacity, int key) {
        this.connection = connection;
        this.address = address;
        this.capacity = capacity;
        this.key = key;
    }

    public Connection getConnection() {
        return connection;
    }

    public long getAddress() {
        return address;
    }

    public long getCapacity() {
        return capacity;
    }

    public int getKey() {
        return key;
    }
}
