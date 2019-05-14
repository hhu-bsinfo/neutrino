#include <jni.h>

#ifndef RDMA_UTIL_HPP
#define RDMA_UTIL_HPP

struct Result {
    int status;
    long handle;
} __attribute__ ((packed));

struct MemberInfo {
    char name[32];
    int offset;
} __attribute__ ((packed));

struct StructInfo {
    int structSize;
    int memberCount;
    MemberInfo *memberInfos;
} __attribute__ ((packed));

template<typename T> T* castHandle(jlong handle) {
    return reinterpret_cast<T*>(handle);
}

#endif //RDMA_UTIL_HPP
