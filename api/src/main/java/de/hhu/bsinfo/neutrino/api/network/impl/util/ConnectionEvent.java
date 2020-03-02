package de.hhu.bsinfo.neutrino.api.network.impl.util;

import java.util.Arrays;

public enum ConnectionEvent {
    RECEIVE_READY (0x1),
    SEND_READY    (0x2),
    QUEUE_READY   (0x4);

    private final int value;

    private static final ConnectionEvent[] VALUES;

    static {
        int arrayLength = Arrays.stream(values()).mapToInt(element -> element.value).max().orElseThrow() + 1;

        VALUES = new ConnectionEvent[arrayLength];

        for (var element : ConnectionEvent.values()) {
            VALUES[element.value] = element;
        }
    }

    ConnectionEvent(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }

    public static ConnectionEvent fromInt(int value) {
        return VALUES[value];
    }
}
