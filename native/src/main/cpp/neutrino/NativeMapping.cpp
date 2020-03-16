#include <neutrino/NativeMapping.hpp>
#include <infiniband/verbs.h>
#include <rdma/rdma_cma.h>
#include <sys/epoll.h>

#define GET_MEMBER_INFO(structName, memberName) NativeMapping::MemberInfo{#memberName, offsetof(structName, memberName)}


/***************************************************************************************/
/*                                                                                     */
/*                                Infiniband Verbs                                     */
/*                                                                                     */
/***************************************************************************************/

NativeMapping::MemberInfo ibv_device_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_device_attr, fw_ver),
    GET_MEMBER_INFO(ibv_device_attr, node_guid),
    GET_MEMBER_INFO(ibv_device_attr, sys_image_guid),
    GET_MEMBER_INFO(ibv_device_attr, max_mr_size),
    GET_MEMBER_INFO(ibv_device_attr, page_size_cap),
    GET_MEMBER_INFO(ibv_device_attr, vendor_id),
    GET_MEMBER_INFO(ibv_device_attr, vendor_part_id),
    GET_MEMBER_INFO(ibv_device_attr, hw_ver),
    GET_MEMBER_INFO(ibv_device_attr, max_qp),
    GET_MEMBER_INFO(ibv_device_attr, max_qp_wr),
    GET_MEMBER_INFO(ibv_device_attr, device_cap_flags),
    GET_MEMBER_INFO(ibv_device_attr, max_sge),
    GET_MEMBER_INFO(ibv_device_attr, max_sge_rd),
    GET_MEMBER_INFO(ibv_device_attr, max_cq),
    GET_MEMBER_INFO(ibv_device_attr, max_cqe),
    GET_MEMBER_INFO(ibv_device_attr, max_mr),
    GET_MEMBER_INFO(ibv_device_attr, max_pd),
    GET_MEMBER_INFO(ibv_device_attr, max_qp_rd_atom),
    GET_MEMBER_INFO(ibv_device_attr, max_ee_rd_atom),
    GET_MEMBER_INFO(ibv_device_attr, max_res_rd_atom),
    GET_MEMBER_INFO(ibv_device_attr, max_qp_init_rd_atom),
    GET_MEMBER_INFO(ibv_device_attr, max_ee_init_rd_atom),
    GET_MEMBER_INFO(ibv_device_attr, atomic_cap),
    GET_MEMBER_INFO(ibv_device_attr, max_ee),
    GET_MEMBER_INFO(ibv_device_attr, max_rdd),
    GET_MEMBER_INFO(ibv_device_attr, max_mw),
    GET_MEMBER_INFO(ibv_device_attr, max_raw_ipv6_qp),
    GET_MEMBER_INFO(ibv_device_attr, max_raw_ethy_qp),
    GET_MEMBER_INFO(ibv_device_attr, max_mcast_grp),
    GET_MEMBER_INFO(ibv_device_attr, max_mcast_qp_attach),
    GET_MEMBER_INFO(ibv_device_attr, max_total_mcast_qp_attach),
    GET_MEMBER_INFO(ibv_device_attr, max_ah),
    GET_MEMBER_INFO(ibv_device_attr, max_fmr),
    GET_MEMBER_INFO(ibv_device_attr, max_map_per_fmr),
    GET_MEMBER_INFO(ibv_device_attr, max_srq),
    GET_MEMBER_INFO(ibv_device_attr, max_srq_wr),
    GET_MEMBER_INFO(ibv_device_attr, max_srq_sge),
    GET_MEMBER_INFO(ibv_device_attr, max_pkeys),
    GET_MEMBER_INFO(ibv_device_attr, local_ca_ack_delay),
    GET_MEMBER_INFO(ibv_device_attr, phys_port_cnt)
};

NativeMapping::StructInfo ibv_device_attr_struct_info {
    sizeof(ibv_device_attr),
    sizeof(ibv_device_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_device_attr_member_infos
};

NativeMapping::MemberInfo ibv_port_attr_member_infos[] {
    GET_MEMBER_INFO(ibv_port_attr, state),
    GET_MEMBER_INFO(ibv_port_attr, max_mtu),
    GET_MEMBER_INFO(ibv_port_attr, active_mtu),
    GET_MEMBER_INFO(ibv_port_attr, gid_tbl_len),
    GET_MEMBER_INFO(ibv_port_attr, port_cap_flags),
    GET_MEMBER_INFO(ibv_port_attr, max_msg_sz),
    GET_MEMBER_INFO(ibv_port_attr, bad_pkey_cntr),
    GET_MEMBER_INFO(ibv_port_attr, qkey_viol_cntr),
    GET_MEMBER_INFO(ibv_port_attr, pkey_tbl_len),
    GET_MEMBER_INFO(ibv_port_attr, lid),
    GET_MEMBER_INFO(ibv_port_attr, sm_lid),
    GET_MEMBER_INFO(ibv_port_attr, lmc),
    GET_MEMBER_INFO(ibv_port_attr, max_vl_num),
    GET_MEMBER_INFO(ibv_port_attr, sm_sl),
    GET_MEMBER_INFO(ibv_port_attr, subnet_timeout),
    GET_MEMBER_INFO(ibv_port_attr, init_type_reply),
    GET_MEMBER_INFO(ibv_port_attr, active_width),
    GET_MEMBER_INFO(ibv_port_attr, active_speed),
    GET_MEMBER_INFO(ibv_port_attr, phys_state),
    GET_MEMBER_INFO(ibv_port_attr, link_layer),
    GET_MEMBER_INFO(ibv_port_attr, flags),
    GET_MEMBER_INFO(ibv_port_attr, port_cap_flags2),
};

NativeMapping::StructInfo ibv_port_attr_struct_info {
    sizeof(ibv_port_attr),
    sizeof(ibv_port_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_port_attr_member_infos
};

NativeMapping::MemberInfo ibv_async_event_member_infos[] = {
    GET_MEMBER_INFO(ibv_async_event, element.cq),
    GET_MEMBER_INFO(ibv_async_event, element.qp),
    GET_MEMBER_INFO(ibv_async_event, element.srq),
    GET_MEMBER_INFO(ibv_async_event, element.wq),
    GET_MEMBER_INFO(ibv_async_event, element.port_num),
    GET_MEMBER_INFO(ibv_async_event, event_type)
};

NativeMapping::StructInfo ibv_async_event_struct_info {
    sizeof(ibv_async_event),
    sizeof(ibv_async_event_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_async_event_member_infos
};

NativeMapping::MemberInfo ibv_comp_channel_member_infos[] = {
    GET_MEMBER_INFO(ibv_comp_channel, context),
    GET_MEMBER_INFO(ibv_comp_channel, fd),
    GET_MEMBER_INFO(ibv_comp_channel, refcnt)
};

NativeMapping::StructInfo ibv_comp_channel_struct_info {
    sizeof(ibv_comp_channel),
    sizeof(ibv_comp_channel_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_comp_channel_member_infos
};

NativeMapping::MemberInfo ibv_cq_member_infos[] {
    GET_MEMBER_INFO(ibv_cq, context),
    GET_MEMBER_INFO(ibv_cq, channel),
    GET_MEMBER_INFO(ibv_cq, cq_context),
    GET_MEMBER_INFO(ibv_cq, handle),
    GET_MEMBER_INFO(ibv_cq, cqe),
    GET_MEMBER_INFO(ibv_cq, mutex),
    GET_MEMBER_INFO(ibv_cq, cond),
    GET_MEMBER_INFO(ibv_cq, comp_events_completed),
    GET_MEMBER_INFO(ibv_cq, async_events_completed),
};

NativeMapping::StructInfo ibv_cq_struct_info {
    sizeof(ibv_cq),
    sizeof(ibv_cq_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_cq_member_infos
};

NativeMapping::MemberInfo ibv_wc_member_infos[] = {
    GET_MEMBER_INFO(ibv_wc, wr_id),
    GET_MEMBER_INFO(ibv_wc, status),
    GET_MEMBER_INFO(ibv_wc, opcode),
    GET_MEMBER_INFO(ibv_wc, vendor_err),
    GET_MEMBER_INFO(ibv_wc, byte_len),
    GET_MEMBER_INFO(ibv_wc, imm_data),
    GET_MEMBER_INFO(ibv_wc, invalidated_rkey),
    GET_MEMBER_INFO(ibv_wc, qp_num),
    GET_MEMBER_INFO(ibv_wc, src_qp),
    GET_MEMBER_INFO(ibv_wc, wc_flags),
    GET_MEMBER_INFO(ibv_wc, pkey_index),
    GET_MEMBER_INFO(ibv_wc, slid),
    GET_MEMBER_INFO(ibv_wc, sl),
    GET_MEMBER_INFO(ibv_wc, dlid_path_bits)
};

NativeMapping::StructInfo ibv_wc_struct_info {
    sizeof(ibv_wc),
    sizeof(ibv_wc_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_wc_member_infos
};

NativeMapping::MemberInfo ibv_sge_member_infos[] = {
    GET_MEMBER_INFO(ibv_sge, addr),
    GET_MEMBER_INFO(ibv_sge, length),
    GET_MEMBER_INFO(ibv_sge, lkey)
};

NativeMapping::StructInfo ibv_sge_struct_info {
    sizeof(ibv_sge),
    sizeof(ibv_sge_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_sge_member_infos
};

NativeMapping::MemberInfo ibv_send_wr_member_infos[] = {
    GET_MEMBER_INFO(ibv_send_wr, wr_id),
    GET_MEMBER_INFO(ibv_send_wr, next),
    GET_MEMBER_INFO(ibv_send_wr, sg_list),
    GET_MEMBER_INFO(ibv_send_wr, num_sge),
    GET_MEMBER_INFO(ibv_send_wr, opcode),
    GET_MEMBER_INFO(ibv_send_wr, send_flags),
    GET_MEMBER_INFO(ibv_send_wr, imm_data),
    GET_MEMBER_INFO(ibv_send_wr, invalidate_rkey),
    GET_MEMBER_INFO(ibv_send_wr, wr.rdma.remote_addr),
    GET_MEMBER_INFO(ibv_send_wr, wr.rdma.rkey),
    GET_MEMBER_INFO(ibv_send_wr, wr.atomic.remote_addr),
    GET_MEMBER_INFO(ibv_send_wr, wr.atomic.compare_add),
    GET_MEMBER_INFO(ibv_send_wr, wr.atomic.swap),
    GET_MEMBER_INFO(ibv_send_wr, wr.atomic.rkey),
    GET_MEMBER_INFO(ibv_send_wr, wr.ud.ah),
    GET_MEMBER_INFO(ibv_send_wr, wr.ud.remote_qpn),
    GET_MEMBER_INFO(ibv_send_wr, wr.ud.remote_qkey),
    GET_MEMBER_INFO(ibv_send_wr, qp_type.xrc.remote_srqn),
    GET_MEMBER_INFO(ibv_send_wr, bind_mw.mw),
    GET_MEMBER_INFO(ibv_send_wr, bind_mw.rkey),
    GET_MEMBER_INFO(ibv_send_wr, bind_mw.bind_info),
    GET_MEMBER_INFO(ibv_send_wr, tso.hdr),
    GET_MEMBER_INFO(ibv_send_wr, tso.hdr_sz),
    GET_MEMBER_INFO(ibv_send_wr, tso.mss)
};

NativeMapping::StructInfo ibv_send_wr_struct_info {
    sizeof(ibv_send_wr),
    sizeof(ibv_send_wr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_send_wr_member_infos
};

NativeMapping::MemberInfo ibv_recv_wr_member_infos[] = {
    GET_MEMBER_INFO(ibv_recv_wr, wr_id),
    GET_MEMBER_INFO(ibv_recv_wr, next),
    GET_MEMBER_INFO(ibv_recv_wr, sg_list),
    GET_MEMBER_INFO(ibv_recv_wr, num_sge)
};

NativeMapping::StructInfo ibv_recv_wr_struct_info {
    sizeof(ibv_recv_wr),
    sizeof(ibv_recv_wr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_recv_wr_member_infos
};

NativeMapping::MemberInfo ibv_srq_init_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_srq_init_attr, srq_context),
    GET_MEMBER_INFO(ibv_srq_init_attr, attr)
};

NativeMapping::StructInfo ibv_srq_init_attr_struct_info {
    sizeof(ibv_srq_init_attr),
    sizeof(ibv_srq_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_srq_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_srq_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_srq_attr, max_wr),
    GET_MEMBER_INFO(ibv_srq_attr, max_sge),
    GET_MEMBER_INFO(ibv_srq_attr, srq_limit)
};

NativeMapping::StructInfo ibv_srq_attr_struct_info {
    sizeof(ibv_srq_attr),
    sizeof(ibv_srq_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_srq_attr_member_infos
};

NativeMapping::MemberInfo ibv_qp_init_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_qp_init_attr, qp_context),
    GET_MEMBER_INFO(ibv_qp_init_attr, send_cq),
    GET_MEMBER_INFO(ibv_qp_init_attr, recv_cq),
    GET_MEMBER_INFO(ibv_qp_init_attr, srq),
    GET_MEMBER_INFO(ibv_qp_init_attr, cap),
    GET_MEMBER_INFO(ibv_qp_init_attr, qp_type),
    GET_MEMBER_INFO(ibv_qp_init_attr, sq_sig_all)
};

NativeMapping::StructInfo ibv_qp_init_attr_struct_info {
    sizeof(ibv_qp_init_attr),
    sizeof(ibv_qp_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_qp_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_srq_member_infos[] = {
    GET_MEMBER_INFO(ibv_srq, context),
    GET_MEMBER_INFO(ibv_srq, srq_context),
    GET_MEMBER_INFO(ibv_srq, pd),
    GET_MEMBER_INFO(ibv_srq, handle),
    GET_MEMBER_INFO(ibv_srq, mutex),
    GET_MEMBER_INFO(ibv_srq, cond),
    GET_MEMBER_INFO(ibv_srq, events_completed)
};

NativeMapping::StructInfo ibv_srq_struct_info {
    sizeof(ibv_srq),
    sizeof(ibv_srq_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_srq_member_infos
};

NativeMapping::MemberInfo ibv_ah_member_infos[] = {
    GET_MEMBER_INFO(ibv_ah, context),
    GET_MEMBER_INFO(ibv_ah, pd),
    GET_MEMBER_INFO(ibv_ah, handle)
};

NativeMapping::StructInfo ibv_ah_struct_info {
    sizeof(ibv_ah),
    sizeof(ibv_ah_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_ah_member_infos
};

NativeMapping::MemberInfo ibv_ah_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_ah_attr, grh),
    GET_MEMBER_INFO(ibv_ah_attr, dlid),
    GET_MEMBER_INFO(ibv_ah_attr, sl),
    GET_MEMBER_INFO(ibv_ah_attr, src_path_bits),
    GET_MEMBER_INFO(ibv_ah_attr, static_rate),
    GET_MEMBER_INFO(ibv_ah_attr, is_global),
    GET_MEMBER_INFO(ibv_ah_attr, port_num)
};

NativeMapping::StructInfo ibv_ah_attr_struct_info {
    sizeof(ibv_ah_attr),
    sizeof(ibv_ah_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_ah_attr_member_infos
};


NativeMapping::MemberInfo ibv_global_route_member_infos[] = {
    GET_MEMBER_INFO(ibv_global_route, dgid),
    GET_MEMBER_INFO(ibv_global_route, flow_label),
    GET_MEMBER_INFO(ibv_global_route, sgid_index),
    GET_MEMBER_INFO(ibv_global_route, hop_limit),
    GET_MEMBER_INFO(ibv_global_route, traffic_class)
};

NativeMapping::StructInfo ibv_global_route_struct_info {
    sizeof(ibv_global_route),
    sizeof(ibv_global_route_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_global_route_member_infos
};

NativeMapping::MemberInfo ibv_qp_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_qp_attr, qp_state),
    GET_MEMBER_INFO(ibv_qp_attr, cur_qp_state),
    GET_MEMBER_INFO(ibv_qp_attr, path_mtu),
    GET_MEMBER_INFO(ibv_qp_attr, path_mig_state),
    GET_MEMBER_INFO(ibv_qp_attr, qkey),
    GET_MEMBER_INFO(ibv_qp_attr, rq_psn),
    GET_MEMBER_INFO(ibv_qp_attr, sq_psn),
    GET_MEMBER_INFO(ibv_qp_attr, dest_qp_num),
    GET_MEMBER_INFO(ibv_qp_attr, qp_access_flags),
    GET_MEMBER_INFO(ibv_qp_attr, cap),
    GET_MEMBER_INFO(ibv_qp_attr, ah_attr),
    GET_MEMBER_INFO(ibv_qp_attr, alt_ah_attr),
    GET_MEMBER_INFO(ibv_qp_attr, pkey_index),
    GET_MEMBER_INFO(ibv_qp_attr, alt_pkey_index),
    GET_MEMBER_INFO(ibv_qp_attr, en_sqd_async_notify),
    GET_MEMBER_INFO(ibv_qp_attr, sq_draining),
    GET_MEMBER_INFO(ibv_qp_attr, max_rd_atomic),
    GET_MEMBER_INFO(ibv_qp_attr, max_dest_rd_atomic),
    GET_MEMBER_INFO(ibv_qp_attr, min_rnr_timer),
    GET_MEMBER_INFO(ibv_qp_attr, port_num),
    GET_MEMBER_INFO(ibv_qp_attr, timeout),
    GET_MEMBER_INFO(ibv_qp_attr, retry_cnt),
    GET_MEMBER_INFO(ibv_qp_attr, rnr_retry),
    GET_MEMBER_INFO(ibv_qp_attr, alt_port_num),
    GET_MEMBER_INFO(ibv_qp_attr, alt_timeout),
    GET_MEMBER_INFO(ibv_qp_attr, rate_limit)
};

NativeMapping::StructInfo ibv_qp_attr_struct_info {
    sizeof(ibv_qp_attr),
    sizeof(ibv_qp_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_qp_attr_member_infos
};

NativeMapping::MemberInfo ibv_qp_cap_member_infos[] = {
    GET_MEMBER_INFO(ibv_qp_cap, max_send_wr),
    GET_MEMBER_INFO(ibv_qp_cap, max_recv_wr),
    GET_MEMBER_INFO(ibv_qp_cap, max_send_sge),
    GET_MEMBER_INFO(ibv_qp_cap, max_recv_sge),
    GET_MEMBER_INFO(ibv_qp_cap, max_inline_data)
};

NativeMapping::StructInfo ibv_qp_cap_struct_info {
    sizeof(ibv_qp_cap),
    sizeof(ibv_qp_cap_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_qp_cap_member_infos
};

NativeMapping::MemberInfo ibv_qp_member_infos[] = {
    GET_MEMBER_INFO(ibv_qp, context),
    GET_MEMBER_INFO(ibv_qp, qp_context),
    GET_MEMBER_INFO(ibv_qp, pd),
    GET_MEMBER_INFO(ibv_qp, send_cq),
    GET_MEMBER_INFO(ibv_qp, recv_cq),
    GET_MEMBER_INFO(ibv_qp, srq),
    GET_MEMBER_INFO(ibv_qp, handle),
    GET_MEMBER_INFO(ibv_qp, qp_num),
    GET_MEMBER_INFO(ibv_qp, state),
    GET_MEMBER_INFO(ibv_qp, qp_type),
    GET_MEMBER_INFO(ibv_qp, mutex),
    GET_MEMBER_INFO(ibv_qp, cond),
    GET_MEMBER_INFO(ibv_qp, events_completed)
};

NativeMapping::StructInfo ibv_qp_struct_info {
    sizeof(ibv_qp),
    sizeof(ibv_qp_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_qp_member_infos
};

NativeMapping::MemberInfo ibv_dm_member_infos[] = {
    GET_MEMBER_INFO(ibv_dm, context),
    GET_MEMBER_INFO(ibv_dm, comp_mask)
};

NativeMapping::StructInfo ibv_dm_struct_info {
    sizeof(ibv_dm),
    sizeof(ibv_dm_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_dm_member_infos
};

NativeMapping::MemberInfo ibv_alloc_dm_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_alloc_dm_attr, length),
    GET_MEMBER_INFO(ibv_alloc_dm_attr, log_align_req),
    GET_MEMBER_INFO(ibv_alloc_dm_attr, comp_mask)
};

NativeMapping::StructInfo ibv_alloc_dm_attr_struct_info {
    sizeof(ibv_alloc_dm_attr),
    sizeof(ibv_alloc_dm_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_alloc_dm_attr_member_infos
};

NativeMapping::MemberInfo ibv_mr_member_infos[] = {
    GET_MEMBER_INFO(ibv_mr, context),
    GET_MEMBER_INFO(ibv_mr, pd),
    GET_MEMBER_INFO(ibv_mr, addr),
    GET_MEMBER_INFO(ibv_mr, length),
    GET_MEMBER_INFO(ibv_mr, handle),
    GET_MEMBER_INFO(ibv_mr, lkey),
    GET_MEMBER_INFO(ibv_mr, rkey)
};

NativeMapping::StructInfo ibv_mr_struct_info {
    sizeof(ibv_mr),
    sizeof(ibv_mr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_mr_member_infos
};

NativeMapping::MemberInfo ibv_mw_member_infos[] = {
    GET_MEMBER_INFO(ibv_mw, context),
    GET_MEMBER_INFO(ibv_mw, pd),
    GET_MEMBER_INFO(ibv_mw, rkey),
    GET_MEMBER_INFO(ibv_mw, handle),
    GET_MEMBER_INFO(ibv_mw, type)
};

NativeMapping::StructInfo ibv_mw_struct_info {
    sizeof(ibv_mw),
    sizeof(ibv_mw_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_mw_member_infos
};

NativeMapping::MemberInfo ibv_mw_bind_member_infos[] = {
    GET_MEMBER_INFO(ibv_mw_bind, wr_id),
    GET_MEMBER_INFO(ibv_mw_bind, send_flags),
    GET_MEMBER_INFO(ibv_mw_bind, bind_info)
};

NativeMapping::StructInfo ibv_mw_bind_struct_info {
    sizeof(ibv_mw_bind),
    sizeof(ibv_mw_bind_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_mw_bind_member_infos
};

NativeMapping::MemberInfo ibv_mw_bind_info_member_infos[] = {
    GET_MEMBER_INFO(ibv_mw_bind_info, mr),
    GET_MEMBER_INFO(ibv_mw_bind_info, addr),
    GET_MEMBER_INFO(ibv_mw_bind_info, length),
    GET_MEMBER_INFO(ibv_mw_bind_info, mw_access_flags)
};

NativeMapping::StructInfo ibv_mw_bind_info_struct_info {
    sizeof(ibv_mw_bind_info),
    sizeof(ibv_mw_bind_info_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_mw_bind_info_member_infos
};

NativeMapping::MemberInfo ibv_pd_member_infos[] = {
    GET_MEMBER_INFO(ibv_pd, context),
    GET_MEMBER_INFO(ibv_pd, handle)
};

NativeMapping::StructInfo ibv_pd_struct_info {
    sizeof(ibv_pd),
    sizeof(ibv_pd_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_pd_member_infos
};

NativeMapping::MemberInfo ibv_td_member_infos[] = {
    GET_MEMBER_INFO(ibv_td, context)
};

NativeMapping::StructInfo ibv_td_struct_info {
    sizeof(ibv_td),
    sizeof(ibv_td_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_td_member_infos
};

NativeMapping::MemberInfo ibv_td_init_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_td_init_attr, comp_mask)
};

NativeMapping::StructInfo ibv_td_init_attr_struct_info {
    sizeof(ibv_td_init_attr),
    sizeof(ibv_td_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_td_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_parent_domain_init_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_parent_domain_init_attr, pd),
    GET_MEMBER_INFO(ibv_parent_domain_init_attr, td),
    GET_MEMBER_INFO(ibv_parent_domain_init_attr, comp_mask)
};

NativeMapping::StructInfo ibv_parent_domain_init_attr_struct_info {
    sizeof(ibv_parent_domain_init_attr),
    sizeof(ibv_parent_domain_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_parent_domain_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_device_attr_ex_member_infos[] = {
    GET_MEMBER_INFO(ibv_device_attr_ex, orig_attr),
    GET_MEMBER_INFO(ibv_device_attr_ex, comp_mask),
    GET_MEMBER_INFO(ibv_device_attr_ex, odp_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, completion_timestamp_mask),
    GET_MEMBER_INFO(ibv_device_attr_ex, hca_core_clock),
    GET_MEMBER_INFO(ibv_device_attr_ex, device_cap_flags_ex),
    GET_MEMBER_INFO(ibv_device_attr_ex, tso_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, rss_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, max_wq_type_rq),
    GET_MEMBER_INFO(ibv_device_attr_ex, packet_pacing_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, raw_packet_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, tm_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, cq_mod_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, max_dm_size),
    GET_MEMBER_INFO(ibv_device_attr_ex, pci_atomic_caps),
    GET_MEMBER_INFO(ibv_device_attr_ex, xrc_odp_caps)
};

NativeMapping::StructInfo ibv_device_attr_ex_struct_info {
    sizeof(ibv_device_attr_ex),
    sizeof(ibv_device_attr_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_device_attr_ex_member_infos
};

NativeMapping::MemberInfo ibv_odp_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_odp_caps, general_caps),
    GET_MEMBER_INFO(ibv_odp_caps, per_transport_caps.rc_odp_caps),
    GET_MEMBER_INFO(ibv_odp_caps, per_transport_caps.uc_odp_caps),
    GET_MEMBER_INFO(ibv_odp_caps, per_transport_caps.ud_odp_caps)
};

NativeMapping::StructInfo ibv_odp_caps_struct_info {
    sizeof(ibv_odp_caps),
    sizeof(ibv_odp_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_odp_caps_member_infos
};

NativeMapping::MemberInfo ibv_tso_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_tso_caps, max_tso),
    GET_MEMBER_INFO(ibv_tso_caps, supported_qpts)
};

NativeMapping::StructInfo ibv_tso_caps_struct_info {
    sizeof(ibv_tso_caps),
    sizeof(ibv_tso_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_tso_caps_member_infos
};

NativeMapping::MemberInfo ibv_rss_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_rss_caps, supported_qpts),
    GET_MEMBER_INFO(ibv_rss_caps, max_rwq_indirection_tables),
    GET_MEMBER_INFO(ibv_rss_caps, max_rwq_indirection_table_size),
    GET_MEMBER_INFO(ibv_rss_caps, rx_hash_fields_mask),
    GET_MEMBER_INFO(ibv_rss_caps, rx_hash_function)
};

NativeMapping::StructInfo ibv_rss_caps_struct_info {
    sizeof(ibv_rss_caps),
    sizeof(ibv_rss_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_rss_caps_member_infos
};

NativeMapping::MemberInfo ibv_packet_pacing_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_packet_pacing_caps, qp_rate_limit_min),
    GET_MEMBER_INFO(ibv_packet_pacing_caps, qp_rate_limit_max),
    GET_MEMBER_INFO(ibv_packet_pacing_caps, supported_qpts)
};

NativeMapping::StructInfo ibv_packet_pacing_caps_struct_info {
    sizeof(ibv_packet_pacing_caps),
    sizeof(ibv_packet_pacing_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_packet_pacing_caps_member_infos
};

NativeMapping::MemberInfo ibv_tm_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_tm_caps, max_rndv_hdr_size),
    GET_MEMBER_INFO(ibv_tm_caps, max_num_tags),
    GET_MEMBER_INFO(ibv_tm_caps, flags),
    GET_MEMBER_INFO(ibv_tm_caps, max_ops),
    GET_MEMBER_INFO(ibv_tm_caps, max_sge)
};

NativeMapping::StructInfo ibv_tm_caps_struct_info {
    sizeof(ibv_tm_caps),
    sizeof(ibv_tm_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_tm_caps_member_infos
};

NativeMapping::MemberInfo ibv_cq_moderation_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_cq_moderation_caps, max_cq_count),
    GET_MEMBER_INFO(ibv_cq_moderation_caps, max_cq_period)
};

NativeMapping::StructInfo ibv_cq_moderation_caps_struct_info {
    sizeof(ibv_cq_moderation_caps),
    sizeof(ibv_cq_moderation_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_cq_moderation_caps_member_infos
};

NativeMapping::MemberInfo ibv_pci_atomic_caps_member_infos[] = {
    GET_MEMBER_INFO(ibv_pci_atomic_caps, fetch_add),
    GET_MEMBER_INFO(ibv_pci_atomic_caps, swap),
    GET_MEMBER_INFO(ibv_pci_atomic_caps, compare_swap)
};

NativeMapping::StructInfo ibv_pci_atomic_caps_struct_info {
    sizeof(ibv_pci_atomic_caps),
    sizeof(ibv_pci_atomic_caps_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_pci_atomic_caps_member_infos
};

NativeMapping::MemberInfo ibv_query_device_ex_input_member_infos[] = {
    GET_MEMBER_INFO(ibv_query_device_ex_input, comp_mask)
};

NativeMapping::StructInfo ibv_query_device_ex_input_struct_info {
    sizeof(ibv_query_device_ex_input),
    sizeof(ibv_query_device_ex_input_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_query_device_ex_input_member_infos
};

NativeMapping::MemberInfo ibv_xrcd_member_infos[] = {
    GET_MEMBER_INFO(ibv_xrcd, context)
};

NativeMapping::StructInfo ibv_xrcd_struct_info {
    sizeof(ibv_xrcd),
    sizeof(ibv_xrcd_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_xrcd_member_infos
};

NativeMapping::MemberInfo ibv_xrcd_init_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_xrcd_init_attr, comp_mask),
    GET_MEMBER_INFO(ibv_xrcd_init_attr, fd),
    GET_MEMBER_INFO(ibv_xrcd_init_attr, oflags)
};

NativeMapping::StructInfo ibv_xrcd_init_attr_struct_info {
    sizeof(ibv_xrcd_init_attr),
    sizeof(ibv_xrcd_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_xrcd_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_cq_ex_member_infos[] = {
    GET_MEMBER_INFO(ibv_cq_ex, context),
    GET_MEMBER_INFO(ibv_cq_ex, channel),
    GET_MEMBER_INFO(ibv_cq_ex, cq_context),
    GET_MEMBER_INFO(ibv_cq_ex, handle),
    GET_MEMBER_INFO(ibv_cq_ex, cqe),
    GET_MEMBER_INFO(ibv_cq_ex, mutex),
    GET_MEMBER_INFO(ibv_cq_ex, cond),
    GET_MEMBER_INFO(ibv_cq_ex, comp_events_completed),
    GET_MEMBER_INFO(ibv_cq_ex, async_events_completed),
    GET_MEMBER_INFO(ibv_cq_ex, comp_mask),
    GET_MEMBER_INFO(ibv_cq_ex, status),
    GET_MEMBER_INFO(ibv_cq_ex, wr_id)
};

NativeMapping::StructInfo ibv_cq_ex_struct_info {
    sizeof(ibv_cq_ex),
    sizeof(ibv_cq_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_cq_ex_member_infos
};

NativeMapping::MemberInfo ibv_srq_init_attr_ex_member_infos[] = {
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, srq_context),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, attr),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, comp_mask),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, srq_type),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, pd),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, xrcd),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, cq),
    GET_MEMBER_INFO(ibv_srq_init_attr_ex, tm_cap)
};

NativeMapping::StructInfo ibv_srq_init_attr_ex_struct_info {
    sizeof(ibv_srq_init_attr_ex),
    sizeof(ibv_srq_init_attr_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_srq_init_attr_ex_member_infos
};

NativeMapping::MemberInfo ibv_tm_cap_member_infos[] = {
    GET_MEMBER_INFO(ibv_tm_cap, max_num_tags),
    GET_MEMBER_INFO(ibv_tm_cap, max_ops)
};

NativeMapping::StructInfo ibv_tm_cap_struct_info {
    sizeof(ibv_tm_cap),
    sizeof(ibv_tm_cap_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_tm_cap_member_infos
};

NativeMapping::MemberInfo ibv_cq_init_attr_ex_member_infos[] = {
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, cqe),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, cq_context),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, channel),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, comp_vector),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, wc_flags),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, comp_mask),
    GET_MEMBER_INFO(ibv_cq_init_attr_ex, flags)
};

NativeMapping::StructInfo ibv_cq_init_attr_ex_struct_info {
    sizeof(ibv_cq_init_attr_ex),
    sizeof(ibv_cq_init_attr_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_cq_init_attr_ex_member_infos
};

NativeMapping::MemberInfo ibv_poll_cq_attr_member_infos[] = {
    GET_MEMBER_INFO(ibv_poll_cq_attr, comp_mask)
};

NativeMapping::StructInfo ibv_poll_cq_attr_struct_info {
    sizeof(ibv_poll_cq_attr),
    sizeof(ibv_poll_cq_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
    ibv_poll_cq_attr_member_infos
};

NativeMapping::MemberInfo ibv_wq_member_infos[] = {
        GET_MEMBER_INFO(ibv_wq, context),
        GET_MEMBER_INFO(ibv_wq, wq_context),
        GET_MEMBER_INFO(ibv_wq, pd),
        GET_MEMBER_INFO(ibv_wq, cq),
        GET_MEMBER_INFO(ibv_wq, wq_num),
        GET_MEMBER_INFO(ibv_wq, handle),
        GET_MEMBER_INFO(ibv_wq, state),
        GET_MEMBER_INFO(ibv_wq, wq_type),
        GET_MEMBER_INFO(ibv_wq, mutex),
        GET_MEMBER_INFO(ibv_wq, cond),
        GET_MEMBER_INFO(ibv_wq, events_completed),
        GET_MEMBER_INFO(ibv_wq, comp_mask)
};

NativeMapping::StructInfo ibv_wq_struct_info {
        sizeof(ibv_wq),
        sizeof(ibv_wq_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_wq_member_infos
};

NativeMapping::MemberInfo ibv_wq_init_attr_member_infos[] = {
        GET_MEMBER_INFO(ibv_wq_init_attr, wq_context),
        GET_MEMBER_INFO(ibv_wq_init_attr, wq_type),
        GET_MEMBER_INFO(ibv_wq_init_attr, max_wr),
        GET_MEMBER_INFO(ibv_wq_init_attr, max_sge),
        GET_MEMBER_INFO(ibv_wq_init_attr, pd),
        GET_MEMBER_INFO(ibv_wq_init_attr, cq),
        GET_MEMBER_INFO(ibv_wq_init_attr, comp_mask),
        GET_MEMBER_INFO(ibv_wq_init_attr, create_flags)
};

NativeMapping::StructInfo ibv_wq_init_attr_struct_info {
        sizeof(ibv_wq_init_attr),
        sizeof(ibv_wq_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_wq_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_wq_attr_member_infos[] = {
        GET_MEMBER_INFO(ibv_wq_attr, attr_mask),
        GET_MEMBER_INFO(ibv_wq_attr, wq_state),
        GET_MEMBER_INFO(ibv_wq_attr, curr_wq_state),
        GET_MEMBER_INFO(ibv_wq_attr, flags),
        GET_MEMBER_INFO(ibv_wq_attr, flags_mask)
};

NativeMapping::StructInfo ibv_wq_attr_struct_info {
        sizeof(ibv_wq_attr),
        sizeof(ibv_wq_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_wq_attr_member_infos
};

NativeMapping::MemberInfo ibv_rwq_ind_table_member_infos[] = {
        GET_MEMBER_INFO(ibv_rwq_ind_table, context),
        GET_MEMBER_INFO(ibv_rwq_ind_table, ind_tbl_handle),
        GET_MEMBER_INFO(ibv_rwq_ind_table, ind_tbl_num),
        GET_MEMBER_INFO(ibv_rwq_ind_table, comp_mask)
};

NativeMapping::StructInfo ibv_rwq_ind_table_struct_info {
        sizeof(ibv_rwq_ind_table),
        sizeof(ibv_rwq_ind_table_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_rwq_ind_table_member_infos
};

NativeMapping::MemberInfo ibv_rwq_ind_table_init_attr_member_infos[] = {
        GET_MEMBER_INFO(ibv_rwq_ind_table_init_attr, log_ind_tbl_size),
        GET_MEMBER_INFO(ibv_rwq_ind_table_init_attr, ind_tbl),
        GET_MEMBER_INFO(ibv_rwq_ind_table_init_attr, comp_mask)
};

NativeMapping::StructInfo ibv_rwq_ind_table_init_attr_struct_info {
        sizeof(ibv_rwq_ind_table_init_attr),
        sizeof(ibv_rwq_ind_table_init_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_rwq_ind_table_init_attr_member_infos
};

NativeMapping::MemberInfo ibv_qp_ex_member_infos[] = {
        GET_MEMBER_INFO(ibv_qp_ex, qp_base),
        GET_MEMBER_INFO(ibv_qp_ex, comp_mask),
        GET_MEMBER_INFO(ibv_qp_ex, wr_id),
        GET_MEMBER_INFO(ibv_qp_ex, wr_flags)
};

NativeMapping::StructInfo ibv_qp_ex_struct_info {
        sizeof(ibv_qp_ex),
        sizeof(ibv_qp_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_qp_ex_member_infos
};

NativeMapping::MemberInfo ibv_qp_init_attr_ex_member_infos[] = {
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, qp_context),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, send_cq),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, recv_cq),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, srq),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, cap),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, qp_type),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, sq_sig_all),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, comp_mask),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, pd),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, xrcd),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, create_flags),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, max_tso_header),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, rwq_ind_tbl),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, rx_hash_conf),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, source_qpn),
        GET_MEMBER_INFO(ibv_qp_init_attr_ex, send_ops_flags)
};

NativeMapping::StructInfo ibv_qp_init_attr_ex_struct_info {
        sizeof(ibv_qp_init_attr_ex),
        sizeof(ibv_qp_init_attr_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_qp_init_attr_ex_member_infos
};


NativeMapping::MemberInfo ibv_rx_hash_conf_member_infos[] = {
        GET_MEMBER_INFO(ibv_rx_hash_conf, rx_hash_function),
        GET_MEMBER_INFO(ibv_rx_hash_conf, rx_hash_key_len),
        GET_MEMBER_INFO(ibv_rx_hash_conf, rx_hash_key),
        GET_MEMBER_INFO(ibv_rx_hash_conf, rx_hash_fields_mask)
};

NativeMapping::StructInfo ibv_rx_hash_conf_struct_info {
        sizeof(ibv_rx_hash_conf),
        sizeof(ibv_rx_hash_conf_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_rx_hash_conf_member_infos
};

NativeMapping::MemberInfo ibv_data_buf_member_infos[] = {
        GET_MEMBER_INFO(ibv_data_buf, addr),
        GET_MEMBER_INFO(ibv_data_buf, length)
};

NativeMapping::StructInfo ibv_data_buf_struct_info {
        sizeof(ibv_data_buf),
        sizeof(ibv_data_buf_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_data_buf_member_infos
};

NativeMapping::MemberInfo ibv_qp_open_attr_member_infos[] = {
        GET_MEMBER_INFO(ibv_qp_open_attr, comp_mask),
        GET_MEMBER_INFO(ibv_qp_open_attr, qp_num),
        GET_MEMBER_INFO(ibv_qp_open_attr, xrcd),
        GET_MEMBER_INFO(ibv_qp_open_attr, qp_context),
        GET_MEMBER_INFO(ibv_qp_open_attr, qp_type)
};

NativeMapping::StructInfo ibv_qp_open_attr_struct_info {
        sizeof(ibv_qp_open_attr),
        sizeof(ibv_qp_open_attr_member_infos) / sizeof(NativeMapping::MemberInfo),
        ibv_qp_open_attr_member_infos
};

// Epoll

NativeMapping::MemberInfo epoll_data_t_member_infos[] = {
        GET_MEMBER_INFO(epoll_data_t, ptr),
        GET_MEMBER_INFO(epoll_data_t, fd),
        GET_MEMBER_INFO(epoll_data_t, u32),
        GET_MEMBER_INFO(epoll_data_t, u64),
};

NativeMapping::StructInfo epoll_data_t_struct_infos {
        sizeof(epoll_data_t),
        sizeof(epoll_data_t_member_infos) / sizeof(NativeMapping::MemberInfo),
        epoll_data_t_member_infos
};

NativeMapping::MemberInfo epoll_event_member_infos[] = {
        GET_MEMBER_INFO(epoll_event, events),
        GET_MEMBER_INFO(epoll_event, data)
};

NativeMapping::StructInfo epoll_event_struct_infos {
        sizeof(epoll_event),
        sizeof(epoll_event_member_infos) / sizeof(NativeMapping::MemberInfo),
        epoll_event_member_infos
};

/***************************************************************************************/
/*                                                                                     */
/*                                  Benchmark                                          */
/*                                                                                     */
/***************************************************************************************/

struct complex_t {
    double real;
    double imaginary;
};

NativeMapping::MemberInfo complex_t_member_infos[] = {
        GET_MEMBER_INFO(complex_t, real),
        GET_MEMBER_INFO(complex_t, imaginary)
};

NativeMapping::StructInfo complex_t_struct_infos {
        sizeof(complex_t),
        sizeof(complex_t_member_infos) / sizeof(NativeMapping::MemberInfo),
        complex_t_member_infos
};

/***************************************************************************************/
/*                                                                                     */
/*                          RDMA Communication Manager                                 */
/*                                                                                     */
/***************************************************************************************/

NativeMapping::MemberInfo rdma_route_member_infos[] = {
	GET_MEMBER_INFO(rdma_route, addr),
	GET_MEMBER_INFO(rdma_route, path_rec),
	GET_MEMBER_INFO(rdma_route, num_paths)
};

NativeMapping::StructInfo rdma_route_struct_info {
	sizeof(rdma_route),
	sizeof(rdma_route_member_infos) / sizeof(NativeMapping::MemberInfo),
	rdma_route_member_infos
};

NativeMapping::MemberInfo rdma_ud_param_member_infos[] = {
        GET_MEMBER_INFO(rdma_ud_param, private_data),
        GET_MEMBER_INFO(rdma_ud_param, private_data_len),
        GET_MEMBER_INFO(rdma_ud_param, ah_attr),
        GET_MEMBER_INFO(rdma_ud_param, qp_num),
        GET_MEMBER_INFO(rdma_ud_param, qkey)
};

NativeMapping::StructInfo rdma_ud_param_struct_info {
        sizeof(rdma_ud_param),
        sizeof(rdma_ud_param_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_ud_param_member_infos
};

NativeMapping::MemberInfo rdma_conn_param_member_infos[] = {
        GET_MEMBER_INFO(rdma_conn_param, private_data),
        GET_MEMBER_INFO(rdma_conn_param, private_data_len),
        GET_MEMBER_INFO(rdma_conn_param, responder_resources),
        GET_MEMBER_INFO(rdma_conn_param, initiator_depth),
        GET_MEMBER_INFO(rdma_conn_param, flow_control),
        GET_MEMBER_INFO(rdma_conn_param, retry_count),
        GET_MEMBER_INFO(rdma_conn_param, rnr_retry_count),
        GET_MEMBER_INFO(rdma_conn_param, srq),
        GET_MEMBER_INFO(rdma_conn_param, qp_num)
};

NativeMapping::StructInfo rdma_conn_param_struct_info {
        sizeof(rdma_conn_param),
        sizeof(rdma_conn_param_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_conn_param_member_infos
};

NativeMapping::MemberInfo rdma_cm_join_mc_attr_ex_member_infos[] = {
        GET_MEMBER_INFO(rdma_cm_join_mc_attr_ex, comp_mask),
        GET_MEMBER_INFO(rdma_cm_join_mc_attr_ex, join_flags),
        GET_MEMBER_INFO(rdma_cm_join_mc_attr_ex, addr)
};

NativeMapping::StructInfo rdma_cm_join_mc_attr_ex_struct_info {
        sizeof(rdma_cm_join_mc_attr_ex),
        sizeof(rdma_cm_join_mc_attr_ex_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_cm_join_mc_attr_ex_member_infos
};

NativeMapping::MemberInfo rdma_cm_id_member_infos[] = {
        GET_MEMBER_INFO(rdma_cm_id, verbs),
        GET_MEMBER_INFO(rdma_cm_id, channel),
        GET_MEMBER_INFO(rdma_cm_id, context),
        GET_MEMBER_INFO(rdma_cm_id, qp),
        GET_MEMBER_INFO(rdma_cm_id, route),
        GET_MEMBER_INFO(rdma_cm_id, ps),
        GET_MEMBER_INFO(rdma_cm_id, port_num),
        GET_MEMBER_INFO(rdma_cm_id, event),
        GET_MEMBER_INFO(rdma_cm_id, send_cq_channel),
        GET_MEMBER_INFO(rdma_cm_id, send_cq),
        GET_MEMBER_INFO(rdma_cm_id, recv_cq_channel),
        GET_MEMBER_INFO(rdma_cm_id, recv_cq),
        GET_MEMBER_INFO(rdma_cm_id, srq),
        GET_MEMBER_INFO(rdma_cm_id, pd),
        GET_MEMBER_INFO(rdma_cm_id, qp_type)
};

NativeMapping::StructInfo rdma_cm_id_struct_info {
        sizeof(rdma_cm_id),
        sizeof(rdma_cm_id_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_cm_id_member_infos
};

NativeMapping::MemberInfo rdma_ib_addr_member_infos[] = {
        GET_MEMBER_INFO(rdma_ib_addr, sgid),
        GET_MEMBER_INFO(rdma_ib_addr, dgid),
        GET_MEMBER_INFO(rdma_ib_addr, pkey)
};

NativeMapping::StructInfo rdma_ib_addr_struct_info {
        sizeof(rdma_ib_addr),
        sizeof(rdma_ib_addr_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_ib_addr_member_infos
};

NativeMapping::MemberInfo rdma_cm_event_member_infos[] = {
        GET_MEMBER_INFO(rdma_cm_event, id),
        GET_MEMBER_INFO(rdma_cm_event, listen_id),
        GET_MEMBER_INFO(rdma_cm_event, event),
        GET_MEMBER_INFO(rdma_cm_event, status),
        GET_MEMBER_INFO(rdma_cm_event, param.conn),
        GET_MEMBER_INFO(rdma_cm_event, param.ud)
};

NativeMapping::StructInfo rdma_cm_event_struct_info {
        sizeof(rdma_cm_event),
        sizeof(rdma_cm_event_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_cm_event_member_infos
};

NativeMapping::MemberInfo rdma_addrinfo_member_infos[] = {
        GET_MEMBER_INFO(rdma_addrinfo, ai_flags),
        GET_MEMBER_INFO(rdma_addrinfo, ai_family),
        GET_MEMBER_INFO(rdma_addrinfo, ai_qp_type),
        GET_MEMBER_INFO(rdma_addrinfo, ai_port_space),
        GET_MEMBER_INFO(rdma_addrinfo, ai_src_len),
        GET_MEMBER_INFO(rdma_addrinfo, ai_dst_len),
        GET_MEMBER_INFO(rdma_addrinfo, ai_src_addr),
        GET_MEMBER_INFO(rdma_addrinfo, ai_dst_addr),
        GET_MEMBER_INFO(rdma_addrinfo, ai_src_canonname),
        GET_MEMBER_INFO(rdma_addrinfo, ai_dst_canonname),
        GET_MEMBER_INFO(rdma_addrinfo, ai_route_len),
        GET_MEMBER_INFO(rdma_addrinfo, ai_route),
        GET_MEMBER_INFO(rdma_addrinfo, ai_connect_len),
        GET_MEMBER_INFO(rdma_addrinfo, ai_connect),
        GET_MEMBER_INFO(rdma_addrinfo, ai_next)
};

NativeMapping::StructInfo rdma_addrinfo_struct_info {
        sizeof(rdma_addrinfo),
        sizeof(rdma_addrinfo_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_addrinfo_member_infos
};

NativeMapping::MemberInfo rdma_event_channel_member_infos[] = {
        GET_MEMBER_INFO(rdma_event_channel, fd)
};

NativeMapping::StructInfo rdma_event_channel_struct_info {
        sizeof(rdma_event_channel),
        sizeof(rdma_event_channel_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_event_channel_member_infos
};

NativeMapping::MemberInfo rdma_addr_member_infos[] = {
        GET_MEMBER_INFO(rdma_addr, src_addr),
        GET_MEMBER_INFO(rdma_addr, src_sin),
        GET_MEMBER_INFO(rdma_addr, src_sin6),
        GET_MEMBER_INFO(rdma_addr, src_storage),
        GET_MEMBER_INFO(rdma_addr, dst_addr),
        GET_MEMBER_INFO(rdma_addr, dst_sin),
        GET_MEMBER_INFO(rdma_addr, dst_sin6),
        GET_MEMBER_INFO(rdma_addr, dst_storage),
        GET_MEMBER_INFO(rdma_addr, addr.ibaddr)
};

NativeMapping::StructInfo rdma_addr_struct_info {
        sizeof(rdma_addr),
        sizeof(rdma_addr_member_infos) / sizeof(NativeMapping::MemberInfo),
        rdma_addr_member_infos
};

/***************************************************************************************/
/*                                                                                     */
/*                            Struct Information                                       */
/*                                                                                     */
/***************************************************************************************/


std::unordered_map<std::string, NativeMapping::StructInfo*> NativeMapping::structInfos {

    // libibverbs
    {"ibv_device_attr", &ibv_device_attr_struct_info},
    {"ibv_port_attr", &ibv_port_attr_struct_info},
    {"ibv_async_event", &ibv_async_event_struct_info},
    {"ibv_comp_channel", &ibv_comp_channel_struct_info},
    {"ibv_cq", &ibv_cq_struct_info},
    {"ibv_wc", &ibv_wc_struct_info},
    {"ibv_sge", &ibv_sge_struct_info},
    {"ibv_send_wr", &ibv_send_wr_struct_info},
    {"ibv_recv_wr", &ibv_recv_wr_struct_info},
    {"ibv_srq_init_attr", &ibv_srq_init_attr_struct_info},
    {"ibv_srq_attr", &ibv_srq_attr_struct_info},
    {"ibv_srq", &ibv_srq_struct_info},
    {"ibv_ah", &ibv_ah_struct_info},
    {"ibv_ah_attr", &ibv_ah_attr_struct_info},
    {"ibv_global_route", &ibv_global_route_struct_info},
    {"ibv_qp_init_attr", &ibv_qp_init_attr_struct_info},
    {"ibv_qp_attr", &ibv_qp_attr_struct_info},
    {"ibv_qp_cap", &ibv_qp_cap_struct_info},
    {"ibv_qp", &ibv_qp_struct_info},
    {"ibv_dm", &ibv_dm_struct_info},
    {"ibv_alloc_dm_attr", &ibv_alloc_dm_attr_struct_info},
    {"ibv_mr", &ibv_mr_struct_info},
    {"ibv_mw", &ibv_mw_struct_info},
    {"ibv_mw_bind", &ibv_mw_bind_struct_info},
    {"ibv_mw_bind_info", &ibv_mw_bind_info_struct_info},
    {"ibv_pd", &ibv_pd_struct_info},
    {"ibv_td", &ibv_td_struct_info},
    {"ibv_td_init_attr", &ibv_td_init_attr_struct_info},
    {"ibv_parent_domain_init_attr", &ibv_parent_domain_init_attr_struct_info},
    {"ibv_device_attr_ex", &ibv_device_attr_ex_struct_info},
    {"ibv_odp_caps", &ibv_odp_caps_struct_info},
    {"ibv_tso_caps", &ibv_tso_caps_struct_info},
    {"ibv_rss_caps", &ibv_rss_caps_struct_info},
    {"ibv_packet_pacing_caps", &ibv_packet_pacing_caps_struct_info},
    {"ibv_tm_caps", &ibv_tm_caps_struct_info},
    {"ibv_cq_moderation_caps", &ibv_cq_moderation_caps_struct_info},
    {"ibv_pci_atomic_caps", &ibv_pci_atomic_caps_struct_info},
    {"ibv_query_device_ex_input", &ibv_query_device_ex_input_struct_info},
    {"ibv_xrcd", &ibv_xrcd_struct_info},
    {"ibv_xrcd_init_attr", &ibv_xrcd_init_attr_struct_info},
    {"ibv_srq_init_attr_ex", &ibv_srq_init_attr_ex_struct_info},
    {"ibv_tm_cap", &ibv_tm_cap_struct_info},
    {"ibv_cq_ex", &ibv_cq_ex_struct_info},
    {"ibv_cq_init_attr_ex", &ibv_cq_init_attr_ex_struct_info},
    {"ibv_poll_cq_attr", &ibv_poll_cq_attr_struct_info},
    {"ibv_wq", &ibv_wq_struct_info},
    {"ibv_wq_init_attr", &ibv_wq_init_attr_struct_info},
    {"ibv_wq_attr", &ibv_wq_attr_struct_info},
    {"ibv_rwq_ind_table", &ibv_rwq_ind_table_struct_info},
    {"ibv_rwq_ind_table_init_attr", &ibv_rwq_ind_table_init_attr_struct_info},
    {"ibv_qp_ex", &ibv_qp_ex_struct_info},
    {"ibv_qp_init_attr_ex", &ibv_qp_init_attr_ex_struct_info},
    {"ibv_rx_hash_conf", &ibv_rx_hash_conf_struct_info},
    {"ibv_data_buf", &ibv_data_buf_struct_info},
    {"ibv_open_qp_attr", &ibv_qp_open_attr_struct_info},

    // epoll
    {"epoll_data_t", &epoll_data_t_struct_infos},
    {"epoll_event", &epoll_event_struct_infos},

    // librdmacm
    {"rdma_route", &rdma_route_struct_info},
    {"rdma_ud_param", &rdma_ud_param_struct_info},
    {"rdma_conn_param", &rdma_conn_param_struct_info},
    {"rdma_cm_join_mc_attr_ex", &rdma_cm_join_mc_attr_ex_struct_info},
    {"rdma_cm_id", &rdma_cm_id_struct_info},
    {"rdma_ib_addr", &rdma_ib_addr_struct_info},
    {"rdma_cm_event", &rdma_cm_event_struct_info},
    {"rdma_addrinfo", &rdma_addrinfo_struct_info},
    {"rdma_event_channel", &rdma_event_channel_struct_info},
    {"rdma_addr", &rdma_addr_struct_info},

    // benchmark
    {"complex_t", &complex_t_struct_infos}
};

NativeMapping::StructInfo *NativeMapping::getStructInfo(const std::string& identifier) {
    if (structInfos.find(identifier) == structInfos.end()) {
        return nullptr;
    }

    return structInfos[identifier];
}
