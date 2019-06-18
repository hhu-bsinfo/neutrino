package de.hhu.bsinfo.neutrino.api.subscribe.impl;

import de.hhu.bsinfo.neutrino.api.util.FormattedRuntimeException;

public class IllegalSubscriberException extends FormattedRuntimeException {

    public IllegalSubscriberException(String format, Object... args) {
        super(format, args);
    }

    public IllegalSubscriberException(String format, Throwable cause, Object... args) {
        super(format, cause, args);
    }

    public IllegalSubscriberException(String format, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object... args) {
        super(format, cause, enableSuppression, writableStackTrace, args);
    }
}
