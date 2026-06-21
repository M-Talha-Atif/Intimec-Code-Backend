package com.aidentx.iam.exception;

/**
 * Exception thrown when a requested schema field is not found.
 * <p>
 * This exception is used when a field operation cannot be completed
 * because the specified field does not exist in the system or in the
 * specified schema context.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class FieldNotFoundException extends RuntimeException {

    /**
     * Constructs a new FieldNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining why the field was not found
     */
    public FieldNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new FieldNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the field was not found
     * @param cause the cause of this exception
     */
    public FieldNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new FieldNotFoundException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public FieldNotFoundException(Throwable cause) {
        super(cause);
    }
}
