package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.ClassName;
import java.lang.reflect.Type;

public class TypeInfo {

    private final ClassName wrapperType;
    private final Type actualType;

    public TypeInfo(ClassName wrapperType, Type actualType) {
        this.wrapperType = wrapperType;
        this.actualType = actualType;
    }

    public ClassName getWrapperType() {
        return wrapperType;
    }

    public Type getActualType() {
        return actualType;
    }
}
