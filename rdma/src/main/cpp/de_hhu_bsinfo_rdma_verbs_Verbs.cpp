#include <de_hhu_bsinfo_rdma_verbs_Verbs.h>
#include <infiniband/verbs.h>
#include <stddef.h>
#include <vector>
#include <Util.hpp>

JNIEXPORT jint JNICALL Java_de_hhu_bsinfo_rdma_verbs_Verbs_getNumDevices (JNIEnv *, jclass) {
    int numDevices = 0;
    ibv_device **devices = ibv_get_device_list(&numDevices);
    if (devices != nullptr) {
        ibv_free_device_list(devices);
    }
    return numDevices;
}

JNIEXPORT void JNICALL Java_de_hhu_bsinfo_rdma_verbs_Verbs_openDevice (JNIEnv *, jclass, jint index, jlong resultHandle) {
    auto result = castHandle<Result>(resultHandle);
    int numDevices = 0;
    ibv_device **devices = ibv_get_device_list(&numDevices);
    if (devices == nullptr) {
        result->status = 1;
        return;
    }

    if (index >= numDevices) {
        ibv_free_device_list(devices);
        result->status = 1;
        return;
    }

    result->handle = reinterpret_cast<long>(ibv_open_device(devices[index]));
    result->status = 0;

    ibv_free_device_list(devices);
}

JNIEXPORT void JNICALL Java_de_hhu_bsinfo_rdma_verbs_Verbs_queryDevice (JNIEnv *env, jclass clazz, jlong contextHandle, jlong deviceHandle, jlong resultHandle) {
    auto context = castHandle<ibv_context>(contextHandle);
    auto device = castHandle<ibv_device_attr>(deviceHandle);
    auto result = castHandle<Result>(resultHandle);

    result->status = ibv_query_device(context, device);
    result->handle = 0;
}



