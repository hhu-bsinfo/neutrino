package de.hhu.bsinfo.neutrino.api.message;

import de.hhu.bsinfo.neutrino.api.util.NullOptions;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceOptions;

public abstract class MessageService<T extends ServiceOptions> extends Service<T> {

    public abstract void testMethod();
}
