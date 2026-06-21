package com.aidentx.core.log_management.exception;

/**
 * Exception thrown when attempting to pin a log that doesn't exist in OpenSearch.
 */
public class LogNotFoundException extends RuntimeException {

    /**
     * Creates a new LogNotFoundException with the specified message.
     *
     * @param message the error message
     */
    public LogNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new LogNotFoundException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public LogNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
