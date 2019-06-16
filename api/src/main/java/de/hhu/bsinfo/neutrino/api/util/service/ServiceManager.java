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

    private final Map<Class<?>, ServiceContainer> services = new HashMap<>();
    private final DependencyManager dependencyManager = new DependencyManager();
    private final ServiceInjector injector;

    public ServiceManager() {
        injector = new ServiceInjector(this);
    }

    public void initialize() {
        loadServices();
        initializeServices();
    }

    private void loadServices() {
        for (var definition : ServiceLoader.load()) {
            var container = new ServiceContainer(definition);
            services.put(definition.getInterfaceClass(), container);
            dependencyManager.register(definition.getInterfaceClass(), definition.getImplementationClass());
        }
    }

    private void initializeServices() {
        for (var service : dependencyManager.getOrderedDependencies()) {
            var container = services.get(service);
            var config = container.newConfigInstance();
            var instance = container.newServiceInstance(config);
            injector.inject(instance);
            instance.onInit(config);
        }
    }

    @Override
    public <T> T get(Class<T> service) {
        return service.cast(services.get(service).getInstance());
    }

    public static class ServiceContainer {

        private final ServiceDefinition serviceDefinition;

        private Service<ServiceConfig> instance;

        ServiceContainer(final ServiceDefinition serviceDefinition) {
            this.serviceDefinition = serviceDefinition;
        }

        ServiceConfig newConfigInstance() {
            try {
                return serviceDefinition.getConfigClass().getConstructor().newInstance();
            } catch (final Exception e) {
                throw new ServiceInstantiationException("Creating config for {} failed", e, serviceDefinition.getConfigClass().getSimpleName());
            }
        }

        Service<ServiceConfig> newServiceInstance(final ServiceConfig config) {
            if (instance != null) {
                throw new ServiceInstantiationException("An instance of {} was already created: ", instance.getClass().getSimpleName());
            }

            instance = serviceDefinition.createInstance();
            instance.setConfig(config);
            return instance;
        }

        public Service<?> getInstance() {
            return instance;
        }
    }
}
