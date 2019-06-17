package de.hhu.bsinfo.neutrino.api.connection;

import de.hhu.bsinfo.neutrino.api.connection.impl.manager.Connection;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.net.InetSocketAddress;

public interface ConnectionService {

    Single<Connection> connect(InetSocketAddress remote);

    Observable<Connection> listen(InetSocketAddress bindAddress);
}
