package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.ClassName;
import java.util.HashMap;
import java.util.Map;

public class MemberMappings {

    private static final ClassName NATIVE_BYTE_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeByte");
    private static final ClassName NATIVE_SHORT_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeShort");
    private static final ClassName NATIVE_INTEGER_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeInteger");
    private static final ClassName NATIVE_LONG_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeLong");
    private static final ClassName NATIVE_STRING_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeString");


    private static final Map<String, TypeInfo> TYPE_MAPPINGS = new HashMap<>();

    static {
        TYPE_MAPPINGS.put("byte", new TypeInfo(NATIVE_BYTE_CLASS, byte.class, "byteField"));
        TYPE_MAPPINGS.put("uint8_t", new TypeInfo(NATIVE_BYTE_CLASS, byte.class, "byteField"));
        TYPE_MAPPINGS.put("char", new TypeInfo(NATIVE_BYTE_CLASS, byte.class, "byteField"));

        TYPE_MAPPINGS.put("short", new TypeInfo(NATIVE_SHORT_CLASS, short.class, "shortField"));
        TYPE_MAPPINGS.put("uint16_t", new TypeInfo(NATIVE_SHORT_CLASS, short.class, "shortField"));
        TYPE_MAPPINGS.put("__be16", new TypeInfo(NATIVE_SHORT_CLASS, short.class, "shortField"));

        TYPE_MAPPINGS.put("int", new TypeInfo(NATIVE_INTEGER_CLASS, int.class, "integerField"));
        TYPE_MAPPINGS.put("uint32_t", new TypeInfo(NATIVE_INTEGER_CLASS, int.class, "integerField"));
        TYPE_MAPPINGS.put("__be32", new TypeInfo(NATIVE_INTEGER_CLASS, int.class, "integerField"));

        TYPE_MAPPINGS.put("long", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
        TYPE_MAPPINGS.put("uint64_t", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
        TYPE_MAPPINGS.put("__be64", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
        TYPE_MAPPINGS.put("size_t", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
        TYPE_MAPPINGS.put("socklen_t", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));

        TYPE_MAPPINGS.put("pthread_mutex_t", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
        TYPE_MAPPINGS.put("pthread_cond_t", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));

        TYPE_MAPPINGS.put("ibv_gid", new TypeInfo(NATIVE_LONG_CLASS, long.class, "longField"));
    }

    public static TypeInfo resolve(StructMember member) {
        if (member.isPointer()) {
            return TYPE_MAPPINGS.get("long");
        }

        if (member.isEnum()) {
            return TYPE_MAPPINGS.get("int");
        }

        if (member.isStruct()) {
            return TYPE_MAPPINGS.get("long");
        }

        if ("char".equals(member.getType()) && member.isPointer()) {
            return TYPE_MAPPINGS.get("long");
        }

        if (TYPE_MAPPINGS.containsKey(member.getType())) {
            return TYPE_MAPPINGS.get(member.getType());
        }

        throw new IllegalArgumentException("No mapping found for " + member.getType());
    }

}
