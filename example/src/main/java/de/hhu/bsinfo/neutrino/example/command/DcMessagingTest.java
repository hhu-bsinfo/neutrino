package de.hhu.bsinfo.neutrino.example.command;

import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "dc-msg-test",
        description = "Starts a simple InfiniBand messaging test with dynamic connections, using the neutrino core.%n",
        showDefaultValues = true,
        separator = " ")
public class DcMessagingTest implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcMessagingTest.class);

    private static final int QUEUE_SIZE = 100;

    @Override
    public Void call() throws Exception {
        int numDevices = Context.getDeviceCount();

        if(numDevices < 0) {
            LOGGER.error("No InfiniBand devices were found in your system");
            return null;
        }

        var context = Context.openDevice(0);

        if(context == null) {
            LOGGER.error("Unable to open to context");
            return null;
        }

        LOGGER.info("Opened context");

        if(!context.mlx5IsSupported()) {
            LOGGER.error("Your device does not support mlx5 direct verbs");
            return null;
        }

        var protectionDomain = context.allocateProtectionDomain();

        if(protectionDomain == null) {
            LOGGER.error("Unable to allocate protection domain");
            return null;
        }

        LOGGER.info("Allocated protection domain");

        var completionQueue = context.createExtendedCompletionQueue(new ExtendedCompletionQueue.InitialAttributes.Builder(QUEUE_SIZE).build());

        if(completionQueue == null) {
            LOGGER.error("Unable to create completion queue");
            return null;
        }

        LOGGER.info("Created completion queue");

        var attributes = new ExtendedQueuePair.InitialAttributes.Builder(QueuePair.Type.DRIVER, protectionDomain)
                .withSendCompletionQueue(completionQueue.toCompletionQueue())
                .withReceiveCompletionQueue(completionQueue.toCompletionQueue())
                .withMaxSendWorkRequests(QUEUE_SIZE)
                .withMaxReceiveWorkRequests(QUEUE_SIZE)
                .withMaxSendScatterGatherElements(1)
                .withMaxReceiveScatterGatherElements(1)
                .withSendOperationFlags(ExtendedQueuePair.SendOperationFlag.WITH_SEND)
                .build();

        var mlx5Attributes = new Mlx5ExtendedQueuePair.InitialAttributes.Builder()
                .withDcType(Mlx5ExtendedQueuePair.DcType.DCI)
                .withDcAccessKey(0)
                .build();

        var queuePair = context.mlx5CreateQueuePair(attributes, mlx5Attributes);

        if(queuePair == null) {
            LOGGER.error("Unable to create queue pair");
            return null;
        }

        LOGGER.info("Created completion queue");

        return null;
    }
}
