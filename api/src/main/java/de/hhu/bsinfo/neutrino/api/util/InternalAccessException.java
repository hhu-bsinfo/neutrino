package de.hhu.bsinfo.neutrino.api.util;

public class InternalAccessException extends FormattedRuntimeException {

    public InternalAccessException(String format, Object... args) {
        super(format, args);
    }

    public InternalAccessException(String format, Throwable cause, Object... args) {
        super(format, cause, args);
    }

    public InternalAccessException(String format, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object... args) {
        super(format, cause, enableSuppression, writableStackTrace, args);
    }
}
