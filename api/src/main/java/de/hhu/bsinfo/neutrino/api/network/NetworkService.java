package de.hhu.bsinfo.neutrino.api.network;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.util.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.verbs.Mtu;
import org.agrona.DirectBuffer;

import java.io.IOException;

public interface NetworkService {

    InfinibandDevice getDevice();

    InfinibandChannel connect(Negotiator negotiator, NetworkHandler handler, Mtu mtu) throws IOException;

    void send(InfinibandChannel channel, int id, DirectBuffer buffer, int offset, int length);

    void read(InfinibandChannel channel, int id, RemoteHandle handle, RegisteredBuffer buffer, int offset, int length);

    void write(InfinibandChannel channel, int id, RegisteredBuffer buffer, int offset, int length, RemoteHandle handle);
}
