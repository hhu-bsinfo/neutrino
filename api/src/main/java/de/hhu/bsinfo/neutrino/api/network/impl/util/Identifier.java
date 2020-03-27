package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.util.BitMask;

public class Identifier {

    private static final byte CONTEXT_SHIFT     = 0;
    private static final byte ATTACHEMENT_SHIFT = 32;
    private static final byte FLAGS_SHIFT       = 48;

    /**
     *    8     7     6     5     4     3     2     1     0
     *    +-----+-----+-----+-----+-----+-----+-----+-----+
     *    |   FLAGS   |   ATTACH  |        CONTEXT        |
     *    +-----+-----+-----+-----+-----+-----+-----+-----+
     */
    public static long create(int context, short flags, short attachement) {
        return (long) flags << FLAGS_SHIFT | (long) attachement << ATTACHEMENT_SHIFT | context;
    }

    public static long create(int context, short flags) {
        return (long) flags << FLAGS_SHIFT | context;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public static int getContext(long identifier) {
        return (int) (identifier >> CONTEXT_SHIFT);
    }

    public static int getAttachement(long identifier) {
        return (short) (identifier >> ATTACHEMENT_SHIFT);
    }

    public static short getFlags(long identifier) {
        return (short) (identifier >> FLAGS_SHIFT);
    }

    public static boolean hasFlag(short flags, RequestFlag flag) {
        return BitMask.isSet(flags, flag);
    }
}
