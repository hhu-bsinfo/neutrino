#ifndef RDMA_REFLECTIONUTILITY_HPP
#define RDMA_REFLECTIONUTILITY_HPP

#include <unordered_map>
#include <cstdint>
#include <infiniband/verbs.h>

class NativeMapping {

public:

    struct MemberInfo {
        char name[32];
        int offset;
    } __attribute__ ((packed));

    struct StructInfo {
        int structSize;
        int memberCount;
        MemberInfo *memberInfos;
    } __attribute__ ((packed));

    NativeMapping() = delete;

    static StructInfo* getStructInfo(const std::string& identifier);

private:

    static std::unordered_map<std::string, NativeMapping::StructInfo*> structInfos;

};

#endif //RDMA_REFLECTIONUTILITY_HPP
