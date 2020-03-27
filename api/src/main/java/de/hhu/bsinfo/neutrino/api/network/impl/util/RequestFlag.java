package de.hhu.bsinfo.neutrino.api.network.impl.util;

import de.hhu.bsinfo.neutrino.util.flag.IntegerFlag;
import de.hhu.bsinfo.neutrino.util.flag.ShortFlag;
import lombok.val;

public enum RequestFlag implements ShortFlag {
    DIRECT((short) (1 << 0)),
    NO_DATA((short) (1 << 1));

    private final short value;

    RequestFlag(short value) {
        this.value = value;
    }

    @Override
    public short getValue() {
        return value;
    }
}
