package de.hhu.bsinfo.neutrino.example.util;

import de.hhu.bsinfo.neutrino.verbs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidParameterException;

class BaseContext implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseContext.class);

    private final Context context;
    private final ProtectionDomain protectionDomain;

    BaseContext(int deviceNumber) throws IOException {
        int numDevices = Context.getDeviceCount();

        if(numDevices <= deviceNumber) {
            throw new InvalidParameterException("Invalid device number '" + deviceNumber + "'. Only " + numDevices + " InfiniBand " + (numDevices == 1 ? "device was" : "devices were") + " found in your system");
        }

        context = Context.openDevice(deviceNumber);
        if(context == null) {
            throw new IOException("Unable to open context");
        }

        LOGGER.info("Opened context for device {}", context.getDeviceName());

        protectionDomain = context.allocateProtectionDomain();
        if(protectionDomain == null) {
            throw new IOException("Unable to allocate protection domain");
        }

        LOGGER.info("Allocated protection domain");
    }

    Context getContext() {
        return context;
    }

    ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public void close() {
        protectionDomain.close();
        context.close();
    }
}
