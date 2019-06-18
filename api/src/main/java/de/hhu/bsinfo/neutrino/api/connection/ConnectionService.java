package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.connection.impl.Connection;
import de.hhu.bsinfo.neutrino.api.util.Expose;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.net.InetSocketAddress;

@Expose
public interface ConnectionService {

    Single<Connection> connect(InetSocketAddress remote);

    Observable<Connection> listen(InetSocketAddress bindAddress);
}
