package de.hhu.bsinfo.neutrino.api.network.impl.subscriber;

import de.hhu.bsinfo.neutrino.api.network.impl.util.Drainable;
import reactor.core.publisher.BaseSubscriber;

public abstract class DrainableSubscriber<T, S> extends BaseSubscriber<T> implements Drainable<S> {}
