/*
 * Copyright (C) 2019 Heinrich-Heine-Universitaet Duesseldorf, Institute of Computer Science,
 * Department Operating Systems
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package de.hhu.bsinfo.neutrino.api.util.service;

import de.hhu.bsinfo.neutrino.api.util.service.inject.ServiceInjector;
import de.hhu.bsinfo.neutrino.api.util.service.resolve.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ServiceManager implements ServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);

    private final Map<Class<? extends Service<?>>, ServiceContainer> services = new HashMap<>();
    private final DependencyManager<Service<?>> dependencyManager = new DependencyManager<>();
    private final ServiceInjector injector;

    public ServiceManager() {
        injector = new ServiceInjector(this);
    }

    public void initialize() {
        for (var definition : ServiceLoader.load()) {
            var container = new ServiceContainer(definition);
            services.put(definition.getInterfaceClass(), container);
            dependencyManager.register(definition.getInterfaceClass(), definition.getImplementationClass());
        }

        for (var service : dependencyManager.getOrderedDependencies()) {
            var container = services.get(service);
            var instance = container.newInstance(container.newOptionsInstance());
            injector.inject(instance);
            instance.onInit();
        }
    }

    @Override
    public <T extends Service<?>> T get(Class<T> service) {
        return service.cast(services.get(service).getInstance());
    }

    public static class ServiceContainer {

        private final ServiceDefinition serviceDefinition;
        private Service<?> instance;

        ServiceContainer(final ServiceDefinition serviceDefinition) {
            this.serviceDefinition = serviceDefinition;
        }

        ServiceOptions newOptionsInstance() {
            try {
                return serviceDefinition.getOptionsClass().getConstructor().newInstance();
            } catch (final Exception e) {
                throw new ServiceInstantiationException("Creating options for {} failed", e, serviceDefinition.getOptionsClass().getSimpleName());
            }
        }

        Service<?> newInstance(final ServiceOptions options) {
            if (instance != null) {
                throw new ServiceInstantiationException("An instance of {} was already created: ", instance.getClass().getSimpleName());
            }

            instance = serviceDefinition.createInstance();
            instance.setOptions(options);
            return instance;
        }

        public Service<?> getInstance() {
            return instance;
        }
    }
}
