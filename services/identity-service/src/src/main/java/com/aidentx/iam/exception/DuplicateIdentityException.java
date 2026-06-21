package com.aidentx.iam.exception;

/**
 * Exception thrown when attempting to create an identity with a username or email
 * that already exists in the system.
 *
 * @author aidentx
 * @since 1.0
 */
public class DuplicateIdentityException extends RuntimeException {

    /**
     * Constructor with a custom message.
     *
     * @param message the detail message
     */
    public DuplicateIdentityException(String message) {
        super(message);
    }

    /**
     * Constructor with a custom message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public DuplicateIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
