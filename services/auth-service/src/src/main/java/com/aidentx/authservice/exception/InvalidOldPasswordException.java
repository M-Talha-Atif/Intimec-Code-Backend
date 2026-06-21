package com.aidentx.authservice.exception;

/**
 * Exception thrown when the old password provided during a password update is incorrect.
 * <p>
 * This exception is typically thrown by password update logic when the user provides
 * an incorrect current password. It is handled globally to return a 400 Bad Request response
 * with structured error details to the client.
 * </p>
 *
 * Example usage:
 * <pre>
 *   throw new InvalidOldPasswordException(AppConstants.INVALID_OLD_PASSWORD_MSG);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class InvalidOldPasswordException extends RuntimeException {
    /**
     * Constructs a new InvalidOldPasswordException with the specified detail message.
     *
     * @param message the detail message describing the invalid old password error
     */
    public InvalidOldPasswordException(String message) {
        super(message);
    }
}
