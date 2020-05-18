package de.hhu.bsinfo.neutrino.api.network.impl.accessor;

import de.hhu.bsinfo.neutrino.struct.StructInformation;
import de.hhu.bsinfo.neutrino.util.StructUtil;
import de.hhu.bsinfo.neutrino.util.UnsafeProvider;
import de.hhu.bsinfo.neutrino.verbs.ReceiveWorkRequest;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

public class ReceiveRequestAccessor {

    @SuppressWarnings("UseOfSunClasses")
    private static final sun.misc.Unsafe UNSAFE = UnsafeProvider.getUnsafe();

    private static final StructInformation STRUCT_INFO = StructUtil.getInfo(ReceiveWorkRequest.class);

    public static final int ELEMENT_SIZE = STRUCT_INFO.getSize();

    // Work request fields
    private static final int ID_OFFSET = STRUCT_INFO.getOffset("wr_id");
    private static final int NEXT_OFFSET = STRUCT_INFO.getOffset("next");
    private static final int LIST_OFFSET = STRUCT_INFO.getOffset("sg_list");
    private static final int LIST_LENGTH_OFFSET = STRUCT_INFO.getOffset("num_sge");

    public static void setId(long handle, long id) {
        UNSAFE.putLong(handle + ID_OFFSET, id);
    }

    public static long getNext(long handle) {
        return UNSAFE.getLong(handle + NEXT_OFFSET);
    }

    public static void setNext(long handle, long next) {
        UNSAFE.putLong(handle + NEXT_OFFSET, next);
    }

    public static void setListHandle(long handle, long list) {
        UNSAFE.putLong(handle + LIST_OFFSET, list);
    }

    public static void setListLength(long handle, int length) {
        UNSAFE.putInt(handle + LIST_LENGTH_OFFSET, length);
    }
}
