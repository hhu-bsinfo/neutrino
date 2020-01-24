/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
