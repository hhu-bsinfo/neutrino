package de.hhu.bsinfo.neutrino.api.util;

public final class Assert {

    private Assert() {}

    public static <T> T assertNotNull(T object, String format, Object... args) {
        if (object == null) {
            throw new InitializationException(format, args);
        }

        return object;
    }
}
