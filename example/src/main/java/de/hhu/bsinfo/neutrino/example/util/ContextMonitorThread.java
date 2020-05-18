package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.verbs.AsyncEvent;
import de.hhu.bsinfo.neutrino.verbs.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ContextMonitorThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextMonitorThread.class);

    private final Context context;
    private volatile boolean isRunning;

    public ContextMonitorThread(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        isRunning = true;

        try {
            while(isRunning) {
                AsyncEvent event = context.getAsyncEvent();

                if(event != null) {
                    LOGGER.debug("An AsyncEvent of type {} occurred", event.getEventType());
                    event.acknowledge();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected error", e);
        }
    }

    public void finish() {
        isRunning = false;
    }
}
