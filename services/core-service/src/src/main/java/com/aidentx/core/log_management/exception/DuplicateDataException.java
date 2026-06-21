package com.aidentx.core.log_management.exception;

/**
 * Exception thrown when attempting to create a pinned log with duplicate data.
 * This prevents the creation of pinned logs with identical JSON content.
 */
public class DuplicateDataException extends RuntimeException {
    
    /**
     * Constructs a new DuplicateDataException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate data scenario
     */
    public DuplicateDataException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new DuplicateDataException with the specified detail message and cause.
     *
     * @param message the detail message explaining the duplicate data scenario
     * @param cause   the cause of the exception
     */
    public DuplicateDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
