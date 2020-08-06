package de.hhu.bsinfo.neutrino.verbs.panama;

import static org.linux.rdma.ibverbs_h.*;

import de.hhu.bsinfo.neutrino.verbs.panama.util.Struct;
import jdk.incubator.foreign.MemoryAddress;

public final class QueuePairCapabilities extends Struct {

    public QueuePairCapabilities() {
        super(ibv_qp_cap.allocate());
    }

    public QueuePairCapabilities(MemoryAddress address) {
        super(address, ibv_qp_cap.$LAYOUT());
    }

    public int getMaxSendWorkRequests() {
        return ibv_qp_cap.max_send_wr$get(segment());
    }

    public int getMaxReceiveWorkRequests() {
        return ibv_qp_cap.max_recv_wr$get(segment());
    }

    public int getMaxSendScatterGatherElements() {
        return ibv_qp_cap.max_send_sge$get(segment());
    }

    public int getMaxReceiveScatterGatherElements() {
        return ibv_qp_cap.max_recv_sge$get(segment());
    }

    public int getMaxInlineData() {
        return ibv_qp_cap.max_inline_data$get(segment());
    }

    public void setMaxSendWorkRequests(final int value) {
        ibv_qp_cap.max_send_wr$set(segment(), value);
    }

    public void setMaxReceiveWorkRequests(final int value) {
        ibv_qp_cap.max_recv_wr$set(segment(), value);
    }

    public void setMaxSendScatterGatherElements(final int value) {
        ibv_qp_cap.max_send_sge$set(segment(), value);
    }

    public void setMaxReceiveScatterGatherElements(final int value) {
        ibv_qp_cap.max_recv_sge$set(segment(), value);
    }

    public void setMaxInlineData(final int value) {
        ibv_qp_cap.max_inline_data$set(segment(), value);
    }
}