package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.verbs.AsyncEvent;

import java.util.Arrays;

public enum EpollEvent {
    RECEIVE_READY (0x1),
    SEND_READY    (0x2),
    QUEUE_READY   (0x4);

    private final int value;

    private static final EpollEvent[] VALUES;

    static {
        int arrayLength = Arrays.stream(values()).mapToInt(element -> element.value).max().orElseThrow() + 1;

        VALUES = new EpollEvent[arrayLength];

        for (var element : EpollEvent.values()) {
            VALUES[element.value] = element;
        }
    }

    EpollEvent(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }

    public static EpollEvent fromInt(int value) {
        return VALUES[value];
    }
}
