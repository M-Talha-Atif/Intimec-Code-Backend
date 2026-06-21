package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to set a schema as default when another
 * default schema already exists for the same tenant.
 * <p>
 * This exception ensures that only one default schema exists per tenant,
 * maintaining the integrity of the default schema constraint.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class DuplicateDefaultSchemaException extends RuntimeException {

    /**
     * Constructs a new DuplicateDefaultSchemaException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate default schema scenario
     */
    public DuplicateDefaultSchemaException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateDefaultSchemaException with the specified detail message and cause.
     *
     * @param message the detail message explaining the duplicate default schema scenario
     * @param cause   the cause of the exception
     */
    public DuplicateDefaultSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
