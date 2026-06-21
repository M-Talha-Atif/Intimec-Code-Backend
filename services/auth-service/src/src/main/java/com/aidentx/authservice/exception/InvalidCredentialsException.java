package com.aidentx.authservice.exception;

/**
 * Exception thrown when user credentials (username or password) are invalid.
 * <p>
 * This exception is typically thrown by authentication logic when a login attempt fails
 * due to incorrect credentials. It is handled globally to return a 401 Unauthorized response
 * to the client.
 * </p>
 *
 * Example usage:
 * <pre>
 *   throw new InvalidCredentialsException(AppConstants.WRONG_USERNAME_OR_PASSWORD);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class InvalidCredentialsException extends RuntimeException {
    /**
     * Constructs a new InvalidCredentialsException with the specified detail message.
     *
     * @param message the detail message describing the invalid credentials error
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
