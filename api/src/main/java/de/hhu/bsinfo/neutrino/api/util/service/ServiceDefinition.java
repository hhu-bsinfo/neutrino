package de.hhu.bsinfo.neutrino.api.util.service;

import java.lang.reflect.ParameterizedType;

public class ServiceDefinition {

    private final Class<?> interfaceClass;
    private final Class<? extends Service<ServiceConfig>> implementationClass;

    public ServiceDefinition(Class<?> interfaceClass, Class<? extends Service<ServiceConfig>> implementationClass) {
        this.interfaceClass = interfaceClass;
        this.implementationClass = implementationClass;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public Class<? extends Service<?>> getImplementationClass() {
        return implementationClass;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ServiceConfig> getConfigClass() {
        ParameterizedType parameterizedType = (ParameterizedType) implementationClass.getGenericSuperclass();
        return (Class<? extends ServiceConfig>) parameterizedType.getActualTypeArguments()[0];
    }

    public Service<ServiceConfig> createInstance() {
        try {
            return implementationClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new ServiceInstantiationException("Creating instance of {} failed", e, implementationClass.getName());
        }
    }
}
