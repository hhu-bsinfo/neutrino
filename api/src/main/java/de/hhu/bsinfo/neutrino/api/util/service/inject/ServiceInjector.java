package de.hhu.bsinfo.neutrino.api.util.service.inject;

import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceInjector implements Injector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInjector.class);

    private final ServiceProvider provider;

    public ServiceInjector(final ServiceProvider provider) {
        this.provider = provider;
    }

    @Override
    public void inject(final Object target) {
        for (Field injectableField : getInjectableFields(target.getClass())) {
            if (Modifier.isFinal(injectableField.getModifiers())){
                throw new InjectionException("Injecting final fields is not supported");
            }

            var type = injectableField.getType();
            var service = provider.get(type);

            if (service == null) {
                LOGGER.warn("Could not find dependency {}", type.getName());
                continue;
            }

            if (!type.isAssignableFrom(service.getClass())) {
                LOGGER.warn("dependency {} is not compatible with field type {}",
                        service.getClass().getName(), type.getName());
                continue;
            }

            try {
                injectableField.setAccessible(true);
                injectableField.set(target, service);
            } catch (IllegalAccessException e) {
                throw new InjectionException("Could not inject {} within {}", e,
                        injectableField.getName(), service.getClass().getName());
            }
        }
    }

    private static List<Field> getInjectableFields(final Class<?> target) {
        return Arrays.stream(target.getDeclaredFields())
                .filter(field -> field.getAnnotation(Inject.class) != null)
                .collect(Collectors.toList());
    }
}
