package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class StructDefinition {

    private static final ClassName STRUCT_CLASS = ClassName.get("de.hhu.bsinfo.neutrino.verbs.panama", "Struct");
    private static final ClassName MEMORYADDRESS_CLASS = ClassName.get("jdk.incubator.foreign", "MemoryAddress");

    public static TypeSpec generate(String structName, List<StructMember> members) {
        MethodSpec primaryConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addCode("super(C$L::allocate);\n", structName)
            .build();

        MethodSpec secondaryConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(MEMORYADDRESS_CLASS, "address")
                .addCode("super(C$L.$$LAYOUT, address);\n", structName)
                .build();

        return TypeSpec.classBuilder(structName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(STRUCT_CLASS)
            .addMethod(primaryConstructor)
            .addMethod(secondaryConstructor)
            .addMethods(members.stream().map(member -> createGetterMethod(structName, member)).collect(Collectors.toList()))
            .addMethods(members.stream().map(member -> createSetterMethod(structName, member)).collect(Collectors.toList()))
            .build();
    }

    private static MethodSpec createGetterMethod(String structName, StructMember member) {
        var typeInfo = MemberMappings.resolve(member);
        return MethodSpec.methodBuilder(String.format("get%s", capitalize(member.getName())))
            .addModifiers(Modifier.PUBLIC)
            .returns(typeInfo.getActualType())
            .addStatement("return C$L.$L$$get(memoryAddress())", structName, member.getName())
            .build();
    }

    private static MethodSpec createSetterMethod(String structName, StructMember member) {
        var typeInfo = MemberMappings.resolve(member);
        return MethodSpec.methodBuilder(String.format("set%s", capitalize(member.getName())))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(typeInfo.getActualType(), "value", Modifier.FINAL)
            .addStatement("C$L.$L$$set(value)", structName, member.getName())
            .build();
    }

    private static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase(Locale.getDefault()) + input.substring(1);
    }
}
