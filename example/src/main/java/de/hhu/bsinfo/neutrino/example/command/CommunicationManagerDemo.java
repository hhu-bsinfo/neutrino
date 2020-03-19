package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.verbs.AddressInfo;
import de.hhu.bsinfo.neutrino.verbs.CommunicationManager;
import de.hhu.bsinfo.neutrino.verbs.PortSpace;
import de.hhu.bsinfo.neutrino.verbs.QueuePair;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.net.InetSocketAddress;

@Slf4j
@CommandLine.Command(
        name = "cm",
        description = "Demonstrates the RDMA Communication Manager.%n",
        showDefaultValues = true,
        separator = " ")
public class CommunicationManagerDemo implements Runnable {

    private static final int DEFAULT_SERVER_PORT = 2998;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "The port the server will listen on.")
    private int port = DEFAULT_SERVER_PORT;

    @Override
    public void run() {
        if (isServer) {
            runServer();
        } else {
            runClient();
        }
    }

    private void runServer() {

        // Create hints
        var hints = new AddressInfo();
        hints.setFlags(AddressInfo.Flag.PASSIVE);
        hints.setPortSpace(PortSpace.TCP);

        // Get local address information
        var localAddress = new InetSocketAddress(port);
        var addressInfo = CommunicationManager.getAddressInfo(localAddress, hints);

        // Create initial attributes used for new queue pairs
        var attributes = new QueuePair.InitialAttributes();
        attributes.capabilities.setMaxSendWorkRequests(1);
        attributes.capabilities.setMaxReceiveWorkRequests(1);
        attributes.capabilities.setMaxInlineData(16);
        attributes.setSignalLevel(1);

        // Create server endpoint
        var serverEndpoint = CommunicationManager.createEndpoint(addressInfo, null, attributes);

        // Set endpoint into listening mode
        serverEndpoint.listen();

        // Wait for client connection
        var client = serverEndpoint.getRequest();

        serverEndpoint.accept(client, null);

        log.info("Accepted client connection");
    }

    private void runClient() {

        // Create hints
        var hints = new AddressInfo();
        hints.setPortSpace(PortSpace.TCP);

        // Get server address information
        var addressInfo = CommunicationManager.getAddressInfo(serverAddress, hints);

        // Create initial attributes used for new queue pairs
        var attributes = new QueuePair.InitialAttributes();
        attributes.capabilities.setMaxSendWorkRequests(1);
        attributes.capabilities.setMaxReceiveWorkRequests(1);
        attributes.capabilities.setMaxInlineData(16);
        attributes.setSignalLevel(1);

        // Create server endpoint
        var clientEndpoint = CommunicationManager.createEndpoint(addressInfo, null, attributes);

        clientEndpoint.connect(null);

        log.info("Connected to server");
    }
}
