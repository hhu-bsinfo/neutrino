package de.hhu.bsinfo.neutrino.api.network.impl.accessor;

import de.hhu.bsinfo.neutrino.struct.StructInformation;
import de.hhu.bsinfo.neutrino.util.StructUtil;
import de.hhu.bsinfo.neutrino.util.UnsafeProvider;
import de.hhu.bsinfo.neutrino.verbs.ScatterGatherElement;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

public class ScatterGatherAccessor {

    @SuppressWarnings("UseOfSunClasses")
    private static final sun.misc.Unsafe UNSAFE = UnsafeProvider.getUnsafe();

    private static final StructInformation STRUCT_INFO = StructUtil.getInfo(ScatterGatherElement.class);

    public static final int ELEMENT_SIZE = STRUCT_INFO.getSize();

    private static final int ADDRESS_OFFSET = STRUCT_INFO.getOffset("addr");
    private static final int LENGTH_OFFSET = STRUCT_INFO.getOffset("length");
    private static final int LOCAL_KEY_OFFSET = STRUCT_INFO.getOffset("lkey");

    public static void setAddress(long handle, long address) {
        UNSAFE.putLong(handle + ADDRESS_OFFSET, address);
    }

    public static void setLength(long handle, int length) {
        UNSAFE.putInt(handle + LENGTH_OFFSET, length);
    }

    public static void setLocalKey(long handle, int localKey) {
        UNSAFE.putInt(handle + LOCAL_KEY_OFFSET, localKey);
    }
}
