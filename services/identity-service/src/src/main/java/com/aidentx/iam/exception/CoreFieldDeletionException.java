package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to delete a core field from an identity schema.
 * <p>
 * Core fields are protected system fields that cannot be removed from a schema
 * as they are essential for the schema's operation and data integrity.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class CoreFieldDeletionException extends RuntimeException {

    /**
     * Constructs a new CoreFieldDeletionException with the specified detail message.
     *
     * @param message the detail message explaining why the core field cannot be deleted
     */
    public CoreFieldDeletionException(String message) {
        super(message);
    }

    /**
     * Constructs a new CoreFieldDeletionException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the core field cannot be deleted
     * @param cause the cause of this exception
     */
    public CoreFieldDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CoreFieldDeletionException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public CoreFieldDeletionException(Throwable cause) {
        super(cause);
    }
}
