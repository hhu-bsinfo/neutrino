package de.hhu.bsinfo.neutrino.api.util.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoader.class);
    private static final String SERVICE_DEFINITIONS = "META-INF/services/definitions.yml";

    private static final String SERVICES_KEY = "services";
    private static final String INTERFACE_KEY = "interface";
    private static final String IMPLEMENTATION_KEY = "implementation";

    private ServiceLoader() {}

    public static List<ServiceDefinition> load() {
        var yaml = new Yaml();
        try (var input = ServiceLoader.class.getClassLoader().getResourceAsStream(SERVICE_DEFINITIONS)) {
            List<Map<String, String>> content = yaml.load(input);
            return content.stream()
                    .map(ServiceLoader::parse)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new LoaderException("Could not load service definitions", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ServiceDefinition parse(final Map<String, String> definition) {
        if (!definition.containsKey(INTERFACE_KEY) || !definition.containsKey(IMPLEMENTATION_KEY)) {
            throw new LoaderException("Invalid definition provided {}", definition);
        }

        Class<?> interfaceClass;
        Class<?> implementationClass;

        try {
            interfaceClass = Class.forName(definition.get(INTERFACE_KEY));
            implementationClass = Class.forName(definition.get(IMPLEMENTATION_KEY));
        } catch (ClassNotFoundException e) {
            throw new LoaderException("Service definition contains unknown class(es)", e);
        }

        if (!Modifier.isInterface(interfaceClass.getModifiers())) {
            throw new LoaderException("{} must be an interface", interfaceClass.getName());
        }

        if (!interfaceClass.isAssignableFrom(implementationClass)) {
            throw new LoaderException("{} must implement {}", implementationClass.getName(), interfaceClass.getName());
        }

        if (!Service.class.isAssignableFrom(implementationClass)) {
            throw new LoaderException("{} must extend {}", implementationClass.getName(), Service.class.getName());
        }

        return new ServiceDefinition(interfaceClass, (Class<? extends Service<ServiceConfig>>) implementationClass);
    }
}
