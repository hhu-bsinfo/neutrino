package de.hhu.bsinfo.neutrino.api;

import de.hhu.bsinfo.neutrino.api.device.InfinibandDevice;
import de.hhu.bsinfo.neutrino.api.network.NetworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Neutrino {

    private final InfinibandDevice device;
    private final NetworkService networkService;

    public Neutrino(InfinibandDevice device, NetworkService networkService) {
        this.device = device;
        this.networkService = networkService;
    }
}
