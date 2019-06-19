package de.hhu.bsinfo.neutrino.api.message;

import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.util.Expose;
import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.ValueFactory;
import io.reactivex.Observable;

@Expose
public interface MessageService {

    void send(Connection connection, NativeObject object);

    <T extends NativeObject> Observable<T> receive(Connection connection, ValueFactory<T> factory);
}
