package de.hhu.bsinfo.neutrino.api.device;

public interface InfinibandDeviceConfig {

    /**
     * The selected device's device number.
     */
    int getDeviceNumber();

    /**
     * The selected device's port number.
     */
    byte getPortNumber();
}
