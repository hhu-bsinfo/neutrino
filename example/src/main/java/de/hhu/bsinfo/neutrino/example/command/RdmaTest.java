package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.struct.field.NativeLinkedList;
import de.hhu.bsinfo.neutrino.example.util.RdmaContext;
import de.hhu.bsinfo.neutrino.example.util.Result;
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
        name = "rdma-test",
        description = "Starts a simple InfiniBand RDMA test, using the neutrino core.%n",
        showDefaultValues = true,
        separator = " ")
public class RdmaTest implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdmaTest.class);

    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final int DEFAULT_QUEUE_SIZE = 100;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int DEFAULT_OPERATION_COUNT = 1048576;

    private static final String SOCKET_CLOSE_SIGNAL = "close";

    private enum Mode {
        READ, WRITE
    }

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
            names = {"-s", "--size"},
            description = "Sets the buffer size.")
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    @CommandLine.Option(
            names = {"-c", "--connect"},
            description = "The server to connect to.")
    private InetSocketAddress serverAddress;

    @CommandLine.Option(
            names = {"-q", "--queue-size"},
            description = "The queue size to be used for the queue pair and completion queue.")
    private int queueSize = DEFAULT_QUEUE_SIZE;

    @CommandLine.Option(
            names = {"-n", "--count"},
            description = "The amount of RDMA operations to be performed.")
    private int operationCount = DEFAULT_OPERATION_COUNT;

    @CommandLine.Option(
            names = {"-m", "--mode"},
            description = "Set the rdma operation mode (read/write).")
    private Mode mode = Mode.WRITE;

    private ScatterGatherElement scatterGatherElement;
    private SendWorkRequest[] sendWorkRequests;

    private NativeLinkedList<SendWorkRequest> sendList = new NativeLinkedList<>();

    private CompletionQueue.WorkCompletionArray completionArray;

    private RdmaContext context;
    private Result result;

    @Override
    public Void call() throws Exception {
        if(!isServer && serverAddress == null) {
            LOGGER.error("Please specify the server address");
            return null;
        }

        completionArray = new CompletionQueue.WorkCompletionArray(queueSize);

        context = new RdmaContext(device, queueSize, bufferSize);

        scatterGatherElement = new ScatterGatherElement(context.getLocalBuffer().getHandle(), (int) context.getLocalBuffer().getNativeSize(), context.getLocalBuffer().getLocalKey());

        sendWorkRequests = new SendWorkRequest[queueSize];

        if(isServer) {
            startServer();
        } else {
            startClient();
        }

        context.close();

        if(isServer) {
            LOGGER.info(result.toString());
        }

        return null;
    }

    private void startServer() throws IOException {
        var serverSocket = new ServerSocket(port);
        var socket = serverSocket.accept();

        context.connect(socket);

        var sendBuilder = new SendWorkRequest.RdmaBuilder(
                mode == Mode.WRITE ? SendWorkRequest.OpCode.RDMA_WRITE : SendWorkRequest.OpCode.RDMA_READ,
                scatterGatherElement, context.getRemoteBufferInfo().getAddress(), context.getRemoteBufferInfo().getRemoteKey())
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED);

        for(int i = 0; i < queueSize; i++) {
            sendWorkRequests[i] = sendBuilder.build();
        }

        for(int i = 0; i < queueSize; i++) {
            sendWorkRequests[i] = sendBuilder.build();
        }

        LOGGER.info("Starting to " + mode.toString().toLowerCase() + " via RDMA");

        int operationsLeft = operationCount;
        int pendingCompletions = 0;

        long startTime = System.nanoTime();

        while(operationsLeft > 0) {
            int batchSize = queueSize - pendingCompletions;

            if(batchSize > operationsLeft) {
                batchSize = operationsLeft;
            }

            performOperations(batchSize);

            pendingCompletions += batchSize;
            operationsLeft -= batchSize;

            pendingCompletions -= poll();
        }

        while(pendingCompletions > 0) {
            pendingCompletions -= poll();
        }

        long time = System.nanoTime() - startTime;
        result = new Result(operationCount, bufferSize, time);

        LOGGER.info("Finished " + mode.toString().toLowerCase() + "ing via RDMA");

        socket.getOutputStream().write(SOCKET_CLOSE_SIGNAL.getBytes());

        LOGGER.info("Sent '" + SOCKET_CLOSE_SIGNAL + "' to client");

        socket.close();
    }

    private void startClient() throws IOException {
        var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());

        context.connect(socket);

        LOGGER.info("Waiting for server to finish RDMA operations");

        String serverString = new String(socket.getInputStream().readAllBytes());

        if(serverString.equals(SOCKET_CLOSE_SIGNAL)) {
            LOGGER.info("Received '" + serverString + "' from server");
        } else if(serverString.isEmpty()) {
            LOGGER.error("Lost connection to server");
        } else {
            LOGGER.error("Received invalid string '" + serverString + "' from server");
        }

        socket.close();
    }

    private void performOperations(int amount) throws IOException {
        if(amount == 0) {
            return;
        }

        sendList.clear();

        for(int i = 0; i < amount; i++) {
            sendList.add(sendWorkRequests[i]);
        }

        context.getQueuePair().postSend(sendList);
    }

    private int poll() throws IOException {
        var completionQueue = context.getCompletionQueue();

        completionQueue.poll(completionArray);

        for(int i = 0; i < completionArray.getLength(); i++) {
            WorkCompletion completion = completionArray.get(i);

            if(completion.getStatus() != WorkCompletion.Status.SUCCESS) {
                LOGGER.error("Work completion failed with error [{}]: {}", completion.getStatus(), completion.getStatusMessage());
                System.exit(1);
            }
        }

        return completionArray.getLength();
    }
}
