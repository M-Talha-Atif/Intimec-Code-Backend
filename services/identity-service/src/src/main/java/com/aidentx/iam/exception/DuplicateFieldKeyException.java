package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to create a schema field with a system key
 * that already exists within the same schema.
 * <p>
 * This exception ensures the uniqueness of system keys within a schema,
 * preventing conflicts that could arise from duplicate field identifiers.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class DuplicateFieldKeyException extends RuntimeException {

    /**
     * Constructs a new DuplicateFieldKeyException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate key scenario
     */
    public DuplicateFieldKeyException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateFieldKeyException with the specified detail message and cause.
     *
     * @param message the detail message explaining the duplicate key scenario
     * @param cause   the cause of the exception
     */
    public DuplicateFieldKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
