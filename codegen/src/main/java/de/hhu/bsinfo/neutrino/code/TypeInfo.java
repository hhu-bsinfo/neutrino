package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.ClassName;
import java.lang.reflect.Type;

public class TypeInfo {

    private final ClassName wrapperType;
    private final Type actualType;
    private final String initMethod;

    public TypeInfo(ClassName wrapperType, Type actualType, String initMethod) {
        this.wrapperType = wrapperType;
        this.actualType = actualType;
        this.initMethod = initMethod;
    }

    public ClassName getWrapperType() {
        return wrapperType;
    }

    public Type getActualType() {
        return actualType;
    }

    public String getInitMethod() {
        return initMethod;
    }
}
