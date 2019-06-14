package de.hhu.bsinfo.neutrino.api.util.service;

import de.hhu.bsinfo.neutrino.api.util.FormattedRuntimeException;

public class ServiceInstantiationException extends FormattedRuntimeException {

    public ServiceInstantiationException(String format, Object... args) {
        super(format, args);
    }

    public ServiceInstantiationException(String format, Throwable cause, Object... args) {
        super(format, cause, args);
    }

    public ServiceInstantiationException(String format, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object... args) {
        super(format, cause, enableSuppression, writableStackTrace, args);
    }
}
