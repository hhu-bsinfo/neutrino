package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.api.core.impl.CoreServiceOptions;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.api.util.InitializationException;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceOptions;
import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CoreService<T extends ServiceOptions> extends Service<T> {

    public abstract Context getContext();

    public abstract Port getPort();
}
