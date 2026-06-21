package com.aidentx.iam.exception;

/**
 * Exception thrown when an identity schema is not found.
 * <p>
 * This exception is typically thrown when attempting to perform operations
 * on a schema that doesn't exist in the database or when the schema ID
 * provided is invalid or refers to a non-existent schema.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class SchemaNotFoundException extends RuntimeException {

    /**
     * Constructs a new SchemaNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining why the schema was not found
     */
    public SchemaNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new SchemaNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the schema was not found
     * @param cause   the cause of the exception
     */
    public SchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
