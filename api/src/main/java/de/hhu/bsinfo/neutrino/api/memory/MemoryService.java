package de.hhu.bsinfo.neutrino.api.memory;

import de.hhu.bsinfo.neutrino.api.core.CoreService;
import de.hhu.bsinfo.neutrino.api.util.service.Service;
import de.hhu.bsinfo.neutrino.api.util.NullOptions;
import de.hhu.bsinfo.neutrino.api.util.service.ServiceOptions;

import javax.inject.Inject;

public abstract class MemoryService<T extends ServiceOptions> extends Service<T> {

}
