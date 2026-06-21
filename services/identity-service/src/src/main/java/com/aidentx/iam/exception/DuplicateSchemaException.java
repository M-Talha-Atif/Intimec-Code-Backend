package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to create a schema with a schema code
 * that already exists within the same tenant.
 * <p>
 * This exception ensures the uniqueness of schema codes within a tenant,
 * preventing conflicts that could arise from duplicate schema identifiers.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class DuplicateSchemaException extends RuntimeException {

    /**
     * Constructs a new DuplicateSchemaException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate schema scenario
     */
    public DuplicateSchemaException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateSchemaException with the specified detail message and cause.
     *
     * @param message the detail message explaining the duplicate schema scenario
     * @param cause   the cause of the exception
     */
    public DuplicateSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
