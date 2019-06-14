package de.hhu.bsinfo.neutrino.api.util.service;

import java.lang.reflect.ParameterizedType;

public class ServiceDefinition {

    private final Class<? extends Service<?>> interfaceClass;
    private final Class<? extends Service<?>> implementationClass;

    public ServiceDefinition(Class<? extends Service<?>> interfaceClass, Class<? extends Service<?>> implementationClass) {
        this.interfaceClass = interfaceClass;
        this.implementationClass = implementationClass;
    }

    public Class<? extends Service<?>> getInterfaceClass() {
        return interfaceClass;
    }

    public Class<? extends Service<?>> getImplementationClass() {
        return implementationClass;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ServiceOptions> getOptionsClass() {
        ParameterizedType parameterizedType = (ParameterizedType) interfaceClass.getGenericSuperclass();
        return (Class<? extends ServiceOptions>) parameterizedType.getActualTypeArguments()[0];
    }

    public Service<?> createInstance() {
        try {
            return implementationClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new ServiceInstantiationException("Creating instance of {} failed", e, implementationClass.getName());
        }
    }
}
