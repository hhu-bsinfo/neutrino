package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class StructDefinition {

    private static final ClassName STRUCT_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.struct", "Struct");
    private static final ClassName STRUCT_UTIL_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.util", "StructUtil");
    private static final ClassName STRUCT_INFO_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.struct", "StructInformation");
    private static final ClassName ANNOTATION_LINK_NATIVE = ClassName.get("de.hhu.bsinfo.neutrino.util", "LinkNative");

    public static TypeSpec generate(String structName, List<StructMember> members) {
        MethodSpec primaryConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .build();

        var annotation = AnnotationSpec.builder(ANNOTATION_LINK_NATIVE)
                .addMember("value", CodeBlock.of("\"" + structName + "\""))
                .build();

        return TypeSpec.classBuilder(structName)
            .addAnnotation(annotation)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(STRUCT_CLASS)
            .addFields(members.stream().map(StructDefinition::toFieldSpec).collect(Collectors.toList()))
            .addMethod(primaryConstructor)
            .addMethods(members.stream().map(StructDefinition::createGetterMethod).collect(Collectors.toList()))
            .addMethods(members.stream().map(StructDefinition::createSetterMethod).collect(Collectors.toList()))
            .build();
    }

    private static FieldSpec toFieldSpec(StructMember member) {
        var typeInfo = MemberMappings.resolve(member);
        return FieldSpec.builder(typeInfo.getWrapperType(), member.getName())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$L($S)", typeInfo.getInitMethod(), member.getName())
            .build();
    }

    private static MethodSpec createGetterMethod(StructMember member) {
        var typeInfo = MemberMappings.resolve(member);
        return MethodSpec.methodBuilder(String.format("get%s", capitalize(member.getName())))
            .addModifiers(Modifier.PUBLIC)
            .returns(typeInfo.getActualType())
            .addStatement("return $L.get()", member.getName())
            .build();
    }

    private static MethodSpec createSetterMethod(StructMember member) {
        var typeInfo = MemberMappings.resolve(member);
        return MethodSpec.methodBuilder(String.format("set%s", capitalize(member.getName())))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(typeInfo.getActualType(), "value", Modifier.FINAL)
            .addStatement("$L.set(value)", member.getName())
            .build();
    }

    private static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase(Locale.getDefault()) + input.substring(1);
    }
}
