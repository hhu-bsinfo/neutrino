package de.hhu.bsinfo.neutrino.api.memory;

import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;

@Expose
public interface MemoryService {

    void read(RemoteHandle source, RegisteredBuffer target);

    void read(RemoteHandle source, long sourceOffset, RegisteredBuffer target, long targetOffset, long length);

    void write(RegisteredBuffer source, RemoteHandle target);

    void write(RegisteredBuffer source, long sourceOffset, RemoteHandle target, long targetOffset, long length);
}
