package io.dockstore.client.cli;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gluu
 * @since 24/03/17
 */
public final class ExceptionHelper {
    public static final int GENERIC_ERROR = 1; // General error, not yet described by an error type
    public static final int IO_ERROR = 3; // IO throws an exception
    public static final int API_ERROR = 6; // API throws an exception
    public static final int CLIENT_ERROR = 4; // Client does something wrong (ex. input validation)
    public static final int COMMAND_ERROR = 10; // Command is not successful, but not due to errors
    public static final AtomicBoolean DEBUG = new AtomicBoolean(false);
    static final int CONNECTION_ERROR = 150; // Connection exception
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHelper.class);

    private ExceptionHelper() {
    }

    /**
     * Logs the error message and then exits
     * @param message   The error message
     * @param exitCode  The code to exit with
     */
    public static void errorMessage(String message, int exitCode) {
        err(message);
        System.exit(exitCode);
    }

    private static void err(String format, Object... args) {
        LOG.error(String.format(format, args));
    }
}
