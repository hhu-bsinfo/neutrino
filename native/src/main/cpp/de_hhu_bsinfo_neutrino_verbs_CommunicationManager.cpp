#include <de_hhu_bsinfo_neutrino_verbs_CommunicationManager.h>
#include <neutrino/NativeCall.hpp>
#include <rdma/rdma_verbs.h>

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    getAddressInfo0
 * Signature: (Ljava/lang/String;Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_getAddressInfo0 (JNIEnv *env, jclass clazz, jstring host, jstring port, jlong hints, jlong result) {
    auto hostChars = env->GetStringUTFChars(host, nullptr);
    auto portChars = env->GetStringUTFChars(port, nullptr);

    rdma_addrinfo *handle;
    auto status = rdma_getaddrinfo(
            hostChars,
            portChars,
            NativeCall::castHandle<rdma_addrinfo>(hints),
            &handle
    );

    env->ReleaseStringUTFChars(host, hostChars);
    env->ReleaseStringUTFChars(port, portChars);

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, handle);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    freeAddressInfo0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_freeAddressInfo0 (JNIEnv *env, jclass clazz, jlong addressInfo, jlong result) {
    rdma_freeaddrinfo(NativeCall::castHandle<rdma_addrinfo>(addressInfo));
    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), 0, nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    createEndpoint0
 * Signature: (JJJJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_createEndpoint0 (JNIEnv *env, jclass clazz, jlong addressInfo, jlong protectionDomain, jlong attributes, jlong result) {
    rdma_cm_id *handle;
    auto status = rdma_create_ep(
            &handle,
            NativeCall::castHandle<rdma_addrinfo>(addressInfo),
            NativeCall::castHandle<ibv_pd>(protectionDomain),
            NativeCall::castHandle<ibv_qp_init_attr>(attributes)
    );

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, handle);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    destroyEndpoint0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_destroyEndpoint0 (JNIEnv *env, jclass clazz, jlong endpoint, jlong result) {
    rdma_destroy_ep(NativeCall::castHandle<rdma_cm_id>(endpoint));
    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), 0, nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    listen0
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_listen0 (JNIEnv *env, jclass clazz, jlong endpoint, jint backlog, jlong result) {
    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), rdma_listen(NativeCall::castHandle<rdma_cm_id>(endpoint), backlog), nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    getRequest0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_getRequest0 (JNIEnv *env, jclass clazz, jlong serverEndpoint, jlong result) {
    auto id = NativeCall::castHandle<rdma_cm_id>(serverEndpoint);

    rdma_cm_id *handle;
    auto status = rdma_get_request(id, &handle);

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, handle);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    accept0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_accept0 (JNIEnv *env, jclass clazz, jlong endpoint, jlong parameter, jlong result) {
    auto status = rdma_accept(
            NativeCall::castHandle<rdma_cm_id>(endpoint),
            NativeCall::castHandle<rdma_conn_param>(parameter)
    );

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    reject0
 * Signature: (JJB)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_reject0 (JNIEnv *env, jclass clazz, jlong endpoint, jlong privateData, jbyte dataLength, jlong result) {
    auto status = rdma_reject(
            NativeCall::castHandle<rdma_cm_id>(endpoint),
            NativeCall::castHandle<void>(privateData),
            dataLength
    );

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    connect0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_connect0 (JNIEnv *env, jclass clazz, jlong endpoint, jlong parameters, jlong result) {
    auto status = rdma_connect(
            NativeCall::castHandle<rdma_cm_id>(endpoint),
            NativeCall::castHandle<rdma_conn_param>(parameters)
    );

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, nullptr);
}

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    disconnect0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_disconnect0 (JNIEnv *env, jclass clazz, jlong endpoint, jlong result) {
    auto status = rdma_disconnect(NativeCall::castHandle<rdma_cm_id>(endpoint));

    NativeCall::setResult(NativeCall::castHandle<NativeCall::Result>(result), status, nullptr);
}

