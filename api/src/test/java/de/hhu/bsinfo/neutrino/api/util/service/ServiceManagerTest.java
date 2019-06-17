package de.hhu.bsinfo.neutrino.api.util.service;

import de.hhu.bsinfo.neutrino.api.core.impl.CoreServiceImpl;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ServiceManagerTest {

    @Test
    public void get() {

        Class<?>[] seed = { CoreServiceImpl.class };

        var interfaceStream = Stream.iterate(seed,
                                             classes -> classes.length > 0,
                                             classes -> Arrays.stream(classes)
                                                        .flatMap(it -> Arrays.stream(it.getInterfaces()))
                                                        .filter(Objects::nonNull)
                                                        .toArray(Class<?>[]::new));
    }
}