package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.struct.field.NativeLinkedList;
import de.hhu.bsinfo.neutrino.example.util.ConnectionContext;
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
        name = "msg-test",
        description = "Starts a simple InfiniBand messaging test, using the neutrino core.%n",
        showDefaultValues = true,
        separator = " ")
public class MessagingTest implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingTest.class);

    private static final int DEFAULT_SERVER_PORT = 2998;
    private static final int DEFAULT_QUEUE_SIZE = 100;
    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final int DEFAULT_MESSAGE_COUNT = 104857600;

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
            description = "Sets the message size.")
    private int messageSize = DEFAULT_MESSAGE_SIZE;

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
            description = "The amount of messages to be sent/received.")
    private int messageCount = DEFAULT_MESSAGE_COUNT;

    private ScatterGatherElement scatterGatherElement;
    private SendWorkRequest[] sendWorkRequests;
    private ReceiveWorkRequest[] receiveWorkRequests;

    private NativeLinkedList<SendWorkRequest> sendList = new NativeLinkedList<>();
    private NativeLinkedList<ReceiveWorkRequest> receiveList = new NativeLinkedList<>();

    private CompletionQueue.WorkCompletionArray completionArray;

    private ConnectionContext context;
    private Result result;

    @Override
    public Void call() throws Exception {
        if(!isServer && serverAddress == null) {
            LOGGER.error("Please specify the server address");
            return null;
        }

        completionArray = new CompletionQueue.WorkCompletionArray(queueSize);

        context = new ConnectionContext(device, queueSize, messageSize);

        scatterGatherElement = new ScatterGatherElement(context.getLocalBuffer().getHandle(), (int) context.getLocalBuffer().getNativeSize(), context.getLocalBuffer().getLocalKey());

        sendWorkRequests = new SendWorkRequest[queueSize];
        receiveWorkRequests = new ReceiveWorkRequest[queueSize];

        var sendBuilder = new SendWorkRequest.MessageBuilder(SendWorkRequest.OpCode.SEND, scatterGatherElement)
                .withSendFlags(SendWorkRequest.SendFlag.SIGNALED);

        var receiveBuilder = new ReceiveWorkRequest.Builder()
                .withScatterGatherElement(scatterGatherElement);

        for(int i = 0; i < queueSize; i++) {
            sendWorkRequests[i] = sendBuilder.build();
        }

        for(int i = 0; i < queueSize; i++) {
            receiveWorkRequests[i] = receiveBuilder.build();
        }

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

        socket.close();

        LOGGER.info("Starting to send messages");
        
        int messagesLeft = messageCount;
        int pendingCompletions = 0;
        
        long startTime = System.nanoTime();

        while(messagesLeft > 0) {
            int batchSize = queueSize - pendingCompletions;

            if(batchSize > messagesLeft) {
                batchSize = messagesLeft;
            }

            send(batchSize);

            pendingCompletions += batchSize;
            messagesLeft -= batchSize;

            pendingCompletions -= poll();
        }

        while(pendingCompletions > 0) {
            pendingCompletions -= poll();
        }
        
        long time = System.nanoTime() - startTime;
        result = new Result(messageCount, messageSize, time);

        LOGGER.info("Finished sending");
    }

    private void startClient() throws IOException {
        var socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());

        context.connect(socket);

        socket.close();
        
        LOGGER.info("Starting to receive messages");

        int messagesLeft = messageCount;
        int pendingCompletions = 0;

        while(messagesLeft > 0) {
            int batchSize = queueSize - pendingCompletions;

            if(batchSize > messagesLeft) {
                batchSize = messagesLeft;
            }

            receive(batchSize);

            pendingCompletions += batchSize;
            messagesLeft -= batchSize;

            pendingCompletions -= poll();
        }

        while(pendingCompletions > 0) {
            pendingCompletions -= poll();
        }
        
        LOGGER.info("Finished receiving");
    }

    private void send(int amount) throws IOException {
        if(amount == 0) {
            return;
        }

        sendList.clear();

        for(int i = 0; i < amount; i++) {
            sendList.add(sendWorkRequests[i]);
        }

        context.getQueuePair().postSend(sendList);
    }

    private void receive(int amount) throws IOException {
        if(amount == 0) {
            return;
        }

        receiveList.clear();

        for(int i = 0; i < amount; i++) {
            receiveList.add(receiveWorkRequests[i]);
        }

        context.getQueuePair().postReceive(receiveList);
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
