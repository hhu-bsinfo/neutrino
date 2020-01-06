package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.example.util.WindowContext;
import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "window-test",
        description = "Starts a simple test to show the memory window capabilities of modern InfiniBand cards.%n",
        showDefaultValues = true,
        separator = " ")
public class WindowTest implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowTest.class);

    private static final int QUEUE_SIZE = 10;
    private static final int WINDOW_SIZE = 4;

    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final int DEFAULT_BUFFER_SIZE = 16;

    @CommandLine.Option(
            names = "--server",
            description = "Runs this instance in server mode.")
    private boolean isServer;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "The port the server will listen on.")
    private int port = DEFAULT_SERVER_PORT;

    @CommandLine.Option(
            names = { "-d", "--device" },
            description = "Sets the InfiniBand device to be used.")
    private int device = 0;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    @CommandLine.Option(
            names = {"-s", "--size"},
            description = "Sets the buffer size.")
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private int remoteWindowKey;

    private SendWorkRequest sendRemoteKeyRequest;
    private SendWorkRequest readRemoteWindowRequest;

    private ReceiveWorkRequest receiveRemoteKeyRequest;

    private ScatterGatherElement readRemoteWindowElement;

    private CompletionQueue.WorkCompletionArray completionArray;

    private WindowContext context;

    @Override
    public Void call() throws Exception {
        if(!isServer && serverAddress == null) {
            LOGGER.error("Please specify the server address");
            return null;
        }

        context = new WindowContext(device, QUEUE_SIZE, bufferSize, WINDOW_SIZE);

        for(int i = 0; i < bufferSize; i += WINDOW_SIZE) {
            context.getLocalBuffer().putInt(i, i / WINDOW_SIZE);
        }

        sendRemoteKeyRequest = new SendWorkRequest.Builder()
                .withOpCode(SendWorkRequest.OpCode.SEND_WITH_IMM)
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED)
                .build();

        receiveRemoteKeyRequest = new ReceiveWorkRequest.Builder().build();

        readRemoteWindowElement = new ScatterGatherElement(context.getReadBuffer().getHandle(), WINDOW_SIZE, context.getReadBuffer().getLocalKey());

        readRemoteWindowRequest = new SendWorkRequest.Builder()
                .withOpCode(SendWorkRequest.OpCode.RDMA_READ)
                .withScatterGatherElement(readRemoteWindowElement)
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED)
                .build();

        completionArray = new CompletionQueue.WorkCompletionArray(QUEUE_SIZE);

        if(isServer) {
            startServer();
        } else {
            startClient();
        }

        context.close();

        return null;
    }

    private void startServer() throws IOException, InterruptedException {
        var serverSocket = new ServerSocket(port);
        var socket = serverSocket.accept();

        context.connect(socket);

        LOGGER.info("Starting to read memory window from client");

        while(true) {
            receiveWindowKey();
            readRemoteWindow();

            Thread.sleep(1000);

            LOGGER.info("Remote window content: {}", context.getReadBuffer().getInt(0));
        }
    }

    private void startClient() throws IOException, InterruptedException {
        var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());

        context.connect(socket);

        LOGGER.info("Starting to cyclically rebind memory window");

        var first = true;

        while(true) {
            for(int i = first ? WINDOW_SIZE : 0; i < bufferSize; i+= WINDOW_SIZE) {
                sendWindowKey();

                Thread.sleep(1000);

                if(!context.getWindow().rebind(context.getLocalBuffer(), context.getQueuePair(), i, WINDOW_SIZE, AccessFlag.REMOTE_READ, AccessFlag.ZERO_BASED)) {
                    System.exit(1);
                }
            }

            first = false;
        }
    }

    private void sendWindowKey() {
        sendRemoteKeyRequest.setImmediateData(context.getWindow().getRemoteKey());

        context.getQueuePair().postSend(sendRemoteKeyRequest);

        while(poll() == 0) {}
    }

    private void receiveWindowKey() {
        context.getQueuePair().postReceive(receiveRemoteKeyRequest);

        while(poll() == 0) {}
    }

    private void readRemoteWindow() {
        readRemoteWindowRequest.rdma.setRemoteKey(remoteWindowKey);

        context.getQueuePair().postSend(readRemoteWindowRequest);

        while(poll() == 0) {}
    }

    private int poll() {
        var completionQueue = context.getCompletionQueue();

        completionQueue.poll(completionArray);

        for(int i = 0; i < completionArray.getLength(); i++) {
            WorkCompletion completion = completionArray.get(i);

            if(completion.getStatus() != WorkCompletion.Status.SUCCESS) {
                LOGGER.error("Work completion failed with error [{}]: {}", completion.getStatus(), completion.getStatusMessage());
                System.exit(1);
            }

            if(completion.getImmediateData() != 0) {
                remoteWindowKey = completion.getImmediateData();
            }
        }

        return completionArray.getLength();
    }
}
