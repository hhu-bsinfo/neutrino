package de.hhu.bsinfo.neutrino.api.util;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Builder
public @Value class QueuePairAddress implements Serializable {
    private final short localId;
    private final int queuePairNumber;
    private final byte portNumber;
}
