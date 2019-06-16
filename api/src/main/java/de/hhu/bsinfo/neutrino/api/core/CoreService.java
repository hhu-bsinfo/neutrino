package de.hhu.bsinfo.neutrino.api.core;

import de.hhu.bsinfo.neutrino.verbs.Context;
import de.hhu.bsinfo.neutrino.verbs.Port;

public interface CoreService {

    Context getContext();

    Port getPort();
}
