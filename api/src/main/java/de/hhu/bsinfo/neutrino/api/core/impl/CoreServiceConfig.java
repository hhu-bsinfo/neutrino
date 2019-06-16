package de.hhu.bsinfo.neutrino.api.core.impl;

import de.hhu.bsinfo.neutrino.api.util.service.ServiceConfig;

@SuppressWarnings("FieldMayBeFinal")
public class CoreServiceConfig extends ServiceConfig {

    private int deviceNumber = 0;

    private int portNumber = 1;

    public int getDeviceNumber() {
        return deviceNumber;
    }

    public int getPortNumber() {
        return portNumber;
    }
}