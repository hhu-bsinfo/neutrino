package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.NativeByte;
import de.hhu.bsinfo.rdma.data.NativeInteger;
import de.hhu.bsinfo.rdma.data.NativeLong;
import de.hhu.bsinfo.rdma.data.NativeShort;
import de.hhu.bsinfo.rdma.data.Struct;
import de.hhu.bsinfo.rdma.data.StructInformation;
import de.hhu.bsinfo.rdma.util.StructUtil;

public class Port extends Struct {

    private static final StructInformation info = StructUtil.getPortAttributes();

    private static final int SIZE = info.structSize.get();

    public final NativeInteger state = new NativeInteger(getByteBuffer(), info.getOffset("state"));
    public final NativeInteger maxMtu = new NativeInteger(getByteBuffer(), info.getOffset("max_mtu"));
    public final NativeInteger activeMtu = new NativeInteger(getByteBuffer(), info.getOffset("active_mtu"));
    public final NativeInteger gidTableLength = new NativeInteger(getByteBuffer(), info.getOffset("gid_tbl_len"));
    public final NativeInteger portCapabilities = new NativeInteger(getByteBuffer(), info.getOffset("port_cap_flags"));
    public final NativeInteger maxMessageSize = new NativeInteger(getByteBuffer(), info.getOffset("max_msg_sz"));
    public final NativeInteger badPkeyCounter = new NativeInteger(getByteBuffer(), info.getOffset("bad_pkey_cntr"));
    public final NativeInteger qkeyViolationCounter = new NativeInteger(getByteBuffer(), info.getOffset("qkey_viol_cntr"));
    public final NativeShort pkeyTableLength = new NativeShort(getByteBuffer(), info.getOffset("pkey_tbl_len"));
    public final NativeShort localId = new NativeShort(getByteBuffer(), info.getOffset("lid"));
    public final NativeShort subnetManagerLocalId = new NativeShort(getByteBuffer(), info.getOffset("sm_lid"));
    public final NativeByte localIdMask = new NativeByte(getByteBuffer(), info.getOffset("lmc"));
    public final NativeByte maxVirtualLaneCount = new NativeByte(getByteBuffer(), info.getOffset("max_vl_num"));
    public final NativeByte subnetManagerServiceLevel = new NativeByte(getByteBuffer(), info.getOffset("sm_sl"));
    public final NativeByte subnetTimeout = new NativeByte(getByteBuffer(), info.getOffset("subnet_timeout"));
    public final NativeByte initTypeReply = new NativeByte(getByteBuffer(), info.getOffset("init_type_reply"));
    public final NativeByte activeWidth = new NativeByte(getByteBuffer(), info.getOffset("active_width"));
    public final NativeByte activeSpeed = new NativeByte(getByteBuffer(), info.getOffset("active_speed"));
    public final NativeByte physicalState = new NativeByte(getByteBuffer(), info.getOffset("phys_state"));
    public final NativeByte linkLayer = new NativeByte(getByteBuffer(), info.getOffset("link_layer"));
    public final NativeByte flags = new NativeByte(getByteBuffer(), info.getOffset("flags"));
    public final NativeShort portCapabilites2 = new NativeShort(getByteBuffer(), info.getOffset("port_cap_flags2"));

    protected Port() {
        super(SIZE);
    }

    protected Port(long handle) {
        super(handle, SIZE);
    }

    @Override
    public String toString() {
        return "Port {\n" +
            "\tstate=" + state +
            ",\n\tmaxMtu=" + maxMtu +
            ",\n\tactiveMtu=" + activeMtu +
            ",\n\tgidTableLength=" + gidTableLength +
            ",\n\tportCapabilities=" + portCapabilities +
            ",\n\tmaxMessageSize=" + maxMessageSize +
            ",\n\tbadPkeyCounter=" + badPkeyCounter +
            ",\n\tqkeyViolationCounter=" + qkeyViolationCounter +
            ",\n\tpkeyTableLength=" + pkeyTableLength +
            ",\n\tlocalId=" + localId +
            ",\n\tsubnetManagerLocalId=" + subnetManagerLocalId +
            ",\n\tlocalIdMask=" + localIdMask +
            ",\n\tmaxVirtualLaneCount=" + maxVirtualLaneCount +
            ",\n\tsubnetManagerServiceLevel=" + subnetManagerServiceLevel +
            ",\n\tsubnetTimeout=" + subnetTimeout +
            ",\n\tinitTypeReply=" + initTypeReply +
            ",\n\tactiveWidth=" + activeWidth +
            ",\n\tactiveSpeed=" + activeSpeed +
            ",\n\tphysicalState=" + physicalState +
            ",\n\tlinkLayer=" + linkLayer +
            ",\n\tflags=" + flags +
            ",\n\tportCapabilites2=" + portCapabilites2 +
            "\n}";
    }
}
