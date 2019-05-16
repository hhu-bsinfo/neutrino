package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

public class StructDefinition {

    private static final ClassName STRUCT_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.struct", "Struct");
    private static final ClassName STRUCT_UTIL_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.util", "StructUtil");
    private static final ClassName STRUCT_INFO_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.struct", "StructInformation");

    public static TypeSpec generate(String structName, List<StructMember> members) {
        var infoFieldSpec = FieldSpec.builder(STRUCT_INFO_CLASS, "INFO")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.getInfo($S)", STRUCT_UTIL_CLASS, structName)
            .build();

        var sizeFieldSpec = FieldSpec.builder(int.class, "SIZE")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("INFO.structSize.get()")
            .build();

        MethodSpec primaryConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("super(SIZE)")
            .build();

        MethodSpec secondaryConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(long.class, "handle", Modifier.FINAL)
            .addStatement("super($N, SIZE)", "handle")
            .build();

        return TypeSpec.classBuilder(structName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(STRUCT_CLASS)
            .addField(infoFieldSpec)
            .addField(sizeFieldSpec)
            .addFields(members.stream().map(StructDefinition::toFieldSpec).collect(Collectors.toList()))
            .addMethod(primaryConstructor)
            .addMethod(secondaryConstructor)
            .build();
    }

    private static FieldSpec toFieldSpec(StructMember member) {
        var fieldType = ClassName.get("de.hhu.bsinfo.neutrino.data", "NativeLong");
        return FieldSpec.builder(fieldType, member.getName())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new NativeLong(getByteBuffer(), INFO.getOffset($S))", member.getName())
            .build();
    }
}
