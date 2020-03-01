package de.hhu.bsinfo.neutrino.code;

import java.util.List;
import java.util.stream.Collectors;

public class NativeMapGenerator {

    public static String generate(String structName, List<StructMember> members) {

        String memberInfosBody = members.stream()
            .map(member -> String.format("GET_MEMBER_INFO(%s, %s)", structName, member.getName()))
            .collect(Collectors.joining(",\n\t", "\t", ""));

        String memberInfos = String.format("NativeMapping::MemberInfo %s_member_infos[] = {\n%s\n};", structName, memberInfosBody);

        String structInfo = String.format("NativeMapping::StructInfo %s_struct_info {\n\tsizeof(%s),\n\tsizeof(%s_member_infos) / sizeof(NativeMapping::MemberInfo),\n\t%s_member_infos\n};", structName, structName, structName, structName);

        return memberInfos + "\n\n" + structInfo + "\n";
    }

}
