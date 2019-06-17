package de.hhu.bsinfo.neutrino.api.util.service.resolve;

import de.hhu.bsinfo.neutrino.api.util.service.ServiceManager;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DependencyManager {

    private final DependencyGraph<Class<?>> graph = new DependencyGraph<>();

    public void register(final Class<?> interfaceClass, final Class<?> implementationClass) {
        graph.add(interfaceClass, getDependencies(implementationClass).toArray(new Class[0]));
    }

    public List<Class<?>> getOrderedDependencies() {
        return graph.values().stream()
                .flatMap(dependency -> graph.resolve(dependency).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<Class<?>> getDependencies(final Class<?> target) {
        return Arrays.stream(target.getDeclaredFields())
                .filter(field -> field.getAnnotation(Inject.class) != null)
                .map(Field::getType)
                .map(ServiceManager::findServiceInterface)
                .collect(Collectors.toList());
    }
}
