package de.hhu.bsinfo.neutrino.api.util;

import de.hhu.bsinfo.neutrino.api.network.Negotiator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Slf4j
public class DefaultNegotiator {

    public static Negotiator fromSocket(Socket socket) {
        return local -> {

            log.debug("Starting queue pair information exchange");

            // Create input and output streams to exchange queue pair information
            try (var out = new ObjectOutputStream(socket.getOutputStream());
                 var in = new ObjectInputStream(socket.getInputStream())) {

                log.debug("Sending local information");

                // Send local queue pair information
                out.writeObject(local);

                log.info("Receiving remote information");

                // Receive remote queue pair information
                return (QueuePairAddress) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                return null;
            }
        };
    }
}
