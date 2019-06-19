package de.hhu.bsinfo.neutrino.api.message;

import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.NativeObject;
import io.reactivex.Observable;

@Expose
public interface MessageService {

    void send(Connection connection, NativeObject object);

}
