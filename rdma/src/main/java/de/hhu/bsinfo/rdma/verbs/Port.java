package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.NativeByte;
import de.hhu.bsinfo.rdma.data.NativeInteger;
import de.hhu.bsinfo.rdma.data.NativeShort;
import de.hhu.bsinfo.rdma.data.Struct;
import de.hhu.bsinfo.rdma.data.StructInformation;
import de.hhu.bsinfo.rdma.util.StructUtil;

public class Port extends Struct {

    private static final StructInformation info = StructUtil.getPortAttributes();

    private static final int SIZE = info.structSize.get();

    private final NativeInteger state = new NativeInteger(getByteBuffer(), info.getOffset("state"));
    private final NativeInteger maxMtu = new NativeInteger(getByteBuffer(), info.getOffset("max_mtu"));
    private final NativeInteger activeMtu = new NativeInteger(getByteBuffer(), info.getOffset("active_mtu"));
    private final NativeInteger gidTableLength = new NativeInteger(getByteBuffer(), info.getOffset("gid_tbl_len"));
    private final NativeInteger portCapabilities = new NativeInteger(getByteBuffer(), info.getOffset("port_cap_flags"));
    private final NativeInteger maxMessageSize = new NativeInteger(getByteBuffer(), info.getOffset("max_msg_sz"));
    private final NativeInteger badPkeyCounter = new NativeInteger(getByteBuffer(), info.getOffset("bad_pkey_cntr"));
    private final NativeInteger qkeyViolationCounter = new NativeInteger(getByteBuffer(), info.getOffset("qkey_viol_cntr"));
    private final NativeShort pkeyTableLength = new NativeShort(getByteBuffer(), info.getOffset("pkey_tbl_len"));
    private final NativeShort localId = new NativeShort(getByteBuffer(), info.getOffset("lid"));
    private final NativeShort subnetManagerLocalId = new NativeShort(getByteBuffer(), info.getOffset("sm_lid"));
    private final NativeByte localIdMask = new NativeByte(getByteBuffer(), info.getOffset("lmc"));
    private final NativeByte maxVirtualLaneCount = new NativeByte(getByteBuffer(), info.getOffset("max_vl_num"));
    private final NativeByte subnetManagerServiceLevel = new NativeByte(getByteBuffer(), info.getOffset("sm_sl"));
    private final NativeByte subnetTimeout = new NativeByte(getByteBuffer(), info.getOffset("subnet_timeout"));
    private final NativeByte initTypeReply = new NativeByte(getByteBuffer(), info.getOffset("init_type_reply"));
    private final NativeByte activeWidth = new NativeByte(getByteBuffer(), info.getOffset("active_width"));
    private final NativeByte activeSpeed = new NativeByte(getByteBuffer(), info.getOffset("active_speed"));
    private final NativeByte physicalState = new NativeByte(getByteBuffer(), info.getOffset("phys_state"));
    private final NativeByte linkLayer = new NativeByte(getByteBuffer(), info.getOffset("link_layer"));
    private final NativeByte flags = new NativeByte(getByteBuffer(), info.getOffset("flags"));
    private final NativeShort portCapabilites2 = new NativeShort(getByteBuffer(), info.getOffset("port_cap_flags2"));

    protected Port() {
        super(SIZE);
    }

    protected Port(long handle) {
        super(handle, SIZE);
    }

    public int getState() {
        return state.get();
    }

    public int getMaxMtu() {
        return maxMtu.get();
    }

    public int getActiveMtu() {
        return activeMtu.get();
    }

    public int getGidTableLength() {
        return gidTableLength.get();
    }

    public int getPortCapabilities() {
        return portCapabilities.get();
    }

    public int getMaxMessageSize() {
        return maxMessageSize.get();
    }

    public int getBadPkeyCounter() {
        return badPkeyCounter.get();
    }

    public int getQkeyViolationCounter() {
        return qkeyViolationCounter.get();
    }

    public short getPkeyTableLength() {
        return pkeyTableLength.get();
    }

    public short getLocalId() {
        return localId.get();
    }

    public short getSubnetManagerLocalId() {
        return subnetManagerLocalId.get();
    }

    public byte getLocalIdMask() {
        return localIdMask.get();
    }

    public byte getMaxVirtualLaneCount() {
        return maxVirtualLaneCount.get();
    }

    public byte getSubnetManagerServiceLevel() {
        return subnetManagerServiceLevel.get();
    }

    public byte getSubnetTimeout() {
        return subnetTimeout.get();
    }

    public byte getInitTypeReply() {
        return initTypeReply.get();
    }

    public byte getActiveWidth() {
        return activeWidth.get();
    }

    public byte getActiveSpeed() {
        return activeSpeed.get();
    }

    public byte getPhysicalState() {
        return physicalState.get();
    }

    public byte getLinkLayer() {
        return linkLayer.get();
    }

    public byte getFlags() {
        return flags.get();
    }

    public short getPortCapabilites2() {
        return portCapabilites2.get();
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
