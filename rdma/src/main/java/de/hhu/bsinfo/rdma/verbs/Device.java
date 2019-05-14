package de.hhu.bsinfo.rdma.verbs;

import de.hhu.bsinfo.rdma.data.NativeByte;
import de.hhu.bsinfo.rdma.data.NativeInteger;
import de.hhu.bsinfo.rdma.data.NativeLong;
import de.hhu.bsinfo.rdma.data.NativeString;
import de.hhu.bsinfo.rdma.data.Struct;
import de.hhu.bsinfo.rdma.data.StructInformation;
import de.hhu.bsinfo.rdma.util.StructUtil;

public class Device extends Struct {

    private static final StructInformation info = StructUtil.getDeviceAttribtues();

    private static final int SIZE = info.structSize.get();

    public final NativeString firmwareVersion = new NativeString(getByteBuffer(), info.getOffset("fw_ver"), 64);
    public final NativeLong nodeGuid = new NativeLong(getByteBuffer(), info.getOffset("node_guid"));
    public final NativeLong systemImageGuid = new NativeLong(getByteBuffer(), info.getOffset("sys_image_guid"));
    public final NativeLong maxMemoryRegionSize = new NativeLong(getByteBuffer(), info.getOffset("max_mr_size"));
    public final NativeLong pageSizeCapabilities = new NativeLong(getByteBuffer(), info.getOffset("page_size_cap"));
    public final NativeInteger vendorId = new NativeInteger(getByteBuffer(), info.getOffset("vendor_id"));
    public final NativeInteger vendorPartId = new NativeInteger(getByteBuffer(), info.getOffset("vendor_part_id"));
    public final NativeInteger hardwareVersion = new NativeInteger(getByteBuffer(), info.getOffset("hw_ver"));
    public final NativeInteger maxQueuePairCount = new NativeInteger(getByteBuffer(), info.getOffset("max_qp"));
    public final NativeInteger maxQueuePairSize = new NativeInteger(getByteBuffer(), info.getOffset("max_qp_wr"));
    public final NativeInteger deviceCapabilities = new NativeInteger(getByteBuffer(), info.getOffset("device_cap_flags"));
    public final NativeInteger maxScatterGatherCount = new NativeInteger(getByteBuffer(), info.getOffset("max_sge"));
    public final NativeInteger maxRdScatterGatherCount = new NativeInteger(getByteBuffer(), info.getOffset("max_sge_rd"));
    public final NativeInteger maxCompletionQueueCount = new NativeInteger(getByteBuffer(), info.getOffset("max_cq"));
    public final NativeInteger maxCompletionQueueSize = new NativeInteger(getByteBuffer(), info.getOffset("max_cqe"));
    public final NativeInteger maxMemoryRegionCount = new NativeInteger(getByteBuffer(), info.getOffset("max_mr"));
    public final NativeInteger maxProtectionDomainCount = new NativeInteger(getByteBuffer(), info.getOffset("max_pd"));
    public final NativeInteger maxAddressHandles = new NativeInteger(getByteBuffer(), info.getOffset("max_ah"));
    public final NativeInteger maxSharedReceiveQueueCount = new NativeInteger(getByteBuffer(), info.getOffset("max_srq"));
    public final NativeInteger maxSharedReceiveQueueSize = new NativeInteger(getByteBuffer(), info.getOffset("max_srq_wr"));
    public final NativeInteger maxSharedReceiveQueueScatterGatherCount = new NativeInteger(getByteBuffer(), info.getOffset("max_srq_sge"));
    public final NativeByte physicalPortCount = new NativeByte(getByteBuffer(), info.getOffset("phys_port_cnt"));

    public Device() {
        super(SIZE);
    }

    public Device(long handle) {
        super(handle, SIZE);
    }

    @Override
    public String toString() {
        return "Device {" +
            "\n\tfirmwareVersion=" + firmwareVersion +
            ",\n\tnodeGuid=" + nodeGuid +
            ",\n\tsystemImageGuid=" + systemImageGuid +
            ",\n\tmaxMemoryRegionSize=" + maxMemoryRegionSize +
            ",\n\tpageSizeCapabilities=" + pageSizeCapabilities +
            ",\n\tvendorId=" + vendorId +
            ",\n\tvendorPartId=" + vendorPartId +
            ",\n\thardwareVersion=" + hardwareVersion +
            ",\n\tmaxQueuePairCount=" + maxQueuePairCount +
            ",\n\tmaxQueuePairSize=" + maxQueuePairSize +
            ",\n\tdeviceCapabilities=" + deviceCapabilities +
            ",\n\tmaxScatterGatherCount=" + maxScatterGatherCount +
            ",\n\tmaxRdScatterGatherCount=" + maxRdScatterGatherCount +
            ",\n\tmaxCompletionQueueCount=" + maxCompletionQueueCount +
            ",\n\tmaxCompletionQueueSize=" + maxCompletionQueueSize +
            ",\n\tmaxMemoryRegionCount=" + maxMemoryRegionCount +
            ",\n\tmaxProtectionDomainCount=" + maxProtectionDomainCount +
            ",\n\tmaxAddressHandles=" + maxAddressHandles +
            ",\n\tmaxSharedReceiveQueueCount=" + maxSharedReceiveQueueCount +
            ",\n\tmaxSharedReceiveQueueSize=" + maxSharedReceiveQueueSize +
            ",\n\tmaxSharedReceiveQueueScatterGatherCount="
            + maxSharedReceiveQueueScatterGatherCount +
            ",\n\tphysicalPortCount=" + physicalPortCount +
            "\n}";
    }
}
