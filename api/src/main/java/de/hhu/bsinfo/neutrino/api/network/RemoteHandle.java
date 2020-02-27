package de.hhu.bsinfo.neutrino.api.network;
import lombok.Value;

public @Value class RemoteHandle {
    private final long address;
    private final int key;
}
