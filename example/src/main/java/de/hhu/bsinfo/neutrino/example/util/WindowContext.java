package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.buffer.RegisteredBuffer;
import de.hhu.bsinfo.neutrino.buffer.RegisteredBufferWindow;
import de.hhu.bsinfo.neutrino.verbs.AccessFlag;
import de.hhu.bsinfo.neutrino.verbs.CompletionQueue;
import de.hhu.bsinfo.neutrino.verbs.WorkCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class WindowContext extends RdmaContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContext.class);

    private final long windowSize;

    private RegisteredBufferWindow window;
    private RegisteredBuffer readBuffer;

    public WindowContext(int deviceNumber, int queueSize, long bufferSize, long windowSize) throws IOException {
        super(deviceNumber, queueSize, bufferSize);

        if(bufferSize % windowSize != 0) {
            LOGGER.error("Buffer size must be a multiple of window size [{}]", windowSize);
            System.exit(1);
        }

        this.windowSize = windowSize;
        readBuffer = getProtectionDomain().allocateMemory(windowSize, AccessFlag.LOCAL_WRITE);
    }

    @Override
    public void connect(Socket socket) throws IOException {
        super.connect(socket);

        window = getLocalBuffer().allocateAndBindMemoryWindow(getQueuePair(), 0, windowSize, AccessFlag.REMOTE_READ, AccessFlag.ZERO_BASED);

        if(window == null) {
            System.exit(1);
        }

        LOGGER.info("Requested memory window bind");

        var completions = new CompletionQueue.WorkCompletionArray(1);

        while(completions.getLength() == 0) {
            getCompletionQueue().poll(completions);
        }

        var completion = completions.get(0);

        if(completion.getStatus() != WorkCompletion.Status.SUCCESS) {
            LOGGER.error("Work completion failed with error [{}]", completion.getStatus());
            System.exit(1);
        }

        LOGGER.info("Successfully bound memory window");
    }

    public RegisteredBufferWindow getWindow() {
        return window;
    }

    public RegisteredBuffer getReadBuffer() {
        return readBuffer;
    }

    @Override
    public void close() {
        window.close();
        readBuffer.close();

        super.close();
    }
}
