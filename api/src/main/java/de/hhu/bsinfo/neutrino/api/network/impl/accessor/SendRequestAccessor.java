package de.hhu.bsinfo.neutrino.api.network.impl.accessor;

import de.hhu.bsinfo.neutrino.struct.StructInformation;
import de.hhu.bsinfo.neutrino.util.StructUtil;
import de.hhu.bsinfo.neutrino.util.UnsafeProvider;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

public class SendRequestAccessor {

    @SuppressWarnings("UseOfSunClasses")
    private static final sun.misc.Unsafe UNSAFE = UnsafeProvider.getUnsafe();

    private static final StructInformation STRUCT_INFO = StructUtil.getInfo(SendWorkRequest.class);

    public static final int ELEMENT_SIZE = STRUCT_INFO.getSize();

    // Work request fields
    private static final int ID_OFFSET = STRUCT_INFO.getOffset("wr_id");
    private static final int NEXT_OFFSET = STRUCT_INFO.getOffset("next");
    private static final int LIST_OFFSET = STRUCT_INFO.getOffset("sg_list");
    private static final int LIST_LENGTH_OFFSET = STRUCT_INFO.getOffset("num_sge");
    private static final int OPCODE_OFFSET = STRUCT_INFO.getOffset("opcode");
    private static final int FLAGS_OFFSET = STRUCT_INFO.getOffset("send_flags");
    private static final int IMMEDIATE_OFFSET = STRUCT_INFO.getOffset("imm_data");
    private static final int INV_REMOTE_KEY_OFFSET = STRUCT_INFO.getOffset("invalidate_rkey");

    // RDMA fields
    private static final int RDMA_ADDRESS_OFFSET = STRUCT_INFO.getOffset("wr.rdma.remote_addr");
    private static final int RDMA_KEY_OFFSET = STRUCT_INFO.getOffset("wr.rdma.rkey");

    // Atomic fields
    private static final int ATOMIC_ADDRESS_OFFSET = STRUCT_INFO.getOffset("wr.atomic.remote_addr");
    private static final int ATOMIC_COMPARE_OFFSET = STRUCT_INFO.getOffset("wr.atomic.compare_add");
    private static final int ATOMIC_SWAP_OFFSET = STRUCT_INFO.getOffset("wr.atomic.swap");
    private static final int ATOMIC_KEY_OFFSET = STRUCT_INFO.getOffset("wr.atomic.rkey");

    // UD fields
    private static final int UD_ADDRESS_HANDLE_OFFSET = STRUCT_INFO.getOffset("wr.ud.ah");
    private static final int UD_REMOTE_QP_NUM_OFFSET = STRUCT_INFO.getOffset("wr.ud.remote_qpn");
    private static final int UD_REMOTE_QP_KEY_OFFSET = STRUCT_INFO.getOffset("wr.ud.remote_qkey");

    public static void setId(long handle, long id) {
        UNSAFE.putLong(handle + ID_OFFSET, id);
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

    public static void setOpCode(long handle, int opCode) {
        UNSAFE.putInt(handle + OPCODE_OFFSET, opCode);
    }

    public static void setFlags(long handle, int flags) {
        UNSAFE.putInt(handle + FLAGS_OFFSET, flags);
    }

    public static void setImmediateData(long handle, int data) {
        UNSAFE.putInt(handle + IMMEDIATE_OFFSET, data);
    }

    public static void setInvalidateRemoteKey(long handle, int invalidateRemoteKey) {
        UNSAFE.putInt(handle + INV_REMOTE_KEY_OFFSET, invalidateRemoteKey);
    }

    public static void setRdmaRemoteAddress(long handle, long address) {
        UNSAFE.putLong(handle + RDMA_ADDRESS_OFFSET, address);
    }

    public static void setRdmaRemoteKey(long handle, int key) {
        UNSAFE.putInt(handle + RDMA_KEY_OFFSET, key);
    }
}
