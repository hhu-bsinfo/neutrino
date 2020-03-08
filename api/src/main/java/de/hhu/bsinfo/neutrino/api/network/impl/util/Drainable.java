package de.hhu.bsinfo.neutrino.api.network.impl.util;

import java.util.function.Consumer;

public interface Drainable<T> {

    int drain(Consumer<T> consumer);

    int drain(Consumer<T> consumer, int limit);
}
