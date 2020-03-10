package de.hhu.bsinfo.neutrino.api.network.impl.util;

public class Identifier {

    public static long create(int requestId, int bufferId) {
        return (long) bufferId << 32 | requestId;
    }

    public static int getRequestId(long identifier) {
        return (int) identifier;
    }

    public static int getBufferId(long identifier) {
        return (int) (identifier >> 32);
    }

}
