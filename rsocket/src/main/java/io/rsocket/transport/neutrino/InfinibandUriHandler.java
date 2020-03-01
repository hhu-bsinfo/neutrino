package io.rsocket.transport.neutrino;

import de.hhu.bsinfo.neutrino.api.util.QueuePairAddress;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.neutrino.client.InfinibandClientTransport;
import io.rsocket.transport.neutrino.server.InfinibandServerTransport;
import io.rsocket.uri.UriHandler;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link UriHandler} that creates {@link InfinibandClientTransport}s and {@link
 * InfinibandServerTransport}s.
 */
public final class InfinibandUriHandler implements UriHandler {

    private static final String SCHEME = "ib";

    @Override
    public Optional<ClientTransport> buildClient(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        if (!SCHEME.equals(uri.getScheme())) {
            return Optional.empty();
        }

        short localId = Short.parseShort(uri.getHost());
        byte portNumber = (byte) uri.getPort();
        int queuePairNumber = Integer.parseInt(uri.getPath().substring(1));

        QueuePairAddress remote = QueuePairAddress.builder()
                .localId(localId)
                .queuePairNumber(queuePairNumber)
                .portNumber(portNumber)
                .build();

        // TODO(krakowski)
        //  Find a way to inject neutrino's service provider into this class.
        return Optional.empty();
//        return Optional.of(InfinibandClientTransport.create(remote));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Optional<ServerTransport> buildServer(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        if (!SCHEME.equals(uri.getScheme())) {
            return Optional.empty();
        }

        return Optional.empty();
//        return Optional.of(InfinibandServerTransport.create(uri.getHost(), uri.getPort()));
    }
}
