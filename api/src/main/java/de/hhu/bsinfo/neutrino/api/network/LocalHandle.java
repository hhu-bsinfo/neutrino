package de.hhu.bsinfo.neutrino.api.network;
import lombok.Value;

public @Value class LocalHandle {
    private final long address;
    private final int length;
    private final int key;
}
