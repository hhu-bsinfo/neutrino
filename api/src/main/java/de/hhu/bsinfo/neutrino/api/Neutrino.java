package de.hhu.bsinfo.neutrino.api;

import de.hhu.bsinfo.neutrino.api.core.InternalCoreService;
import de.hhu.bsinfo.neutrino.api.memory.MemoryService;
import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.api.util.InternalAccessException;
import de.hhu.bsinfo.neutrino.api.util.ServiceProvider;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceManager;
import de.hhu.bsinfo.neutrino.generated.BuildConfig;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Neutrino implements ServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neutrino.class);

    private final ServiceManager serviceManager = new ServiceManager();

    private Neutrino() {}

    public static Neutrino newInstance() {
        var neutrino = new Neutrino();
        neutrino.initialize();
        return neutrino;
    }

    private void initialize() {
        initializeServices();
    }

    private void initializeServices() {
        serviceManager.initialize();
    }

    @Override
    public <T> T getService(final Class<T> service) {
        if (!service.isAnnotationPresent(Expose.class)) {
            throw new InternalAccessException("{} is not exposed", service.getName());
        }

        return serviceManager.get(service);
    }

    public static void printBanner() {
        InputStream inputStream = Neutrino.class.getClassLoader().getResourceAsStream("banner.txt");

        if (inputStream == null) {
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String banner = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        System.out.print("\n");
        System.out.printf(banner, BuildConfig.VERSION, BuildConfig.BUILD_DATE, BuildConfig.GIT_BRANCH, BuildConfig.GIT_COMMIT);
        System.out.print("\n\n");
    }
}
