package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to perform operations on an inactive schema.
 * <p>
 * This exception is thrown when trying to add fields or perform modifications
 * on a schema that has been deactivated or is not currently active for use.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class InactiveSchemaException extends RuntimeException {

    /**
     * Constructs a new InactiveSchemaException with the specified detail message.
     *
     * @param message the detail message explaining the inactive schema scenario
     */
    public InactiveSchemaException(String message) {
        super(message);
    }

    /**
     * Constructs a new InactiveSchemaException with the specified detail message and cause.
     *
     * @param message the detail message explaining the inactive schema scenario
     * @param cause   the cause of the exception
     */
    public InactiveSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
