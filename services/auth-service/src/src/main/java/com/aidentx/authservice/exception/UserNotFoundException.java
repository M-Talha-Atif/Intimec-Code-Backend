package com.aidentx.authservice.exception;

/**
 * Exception thrown when a user is not found in the system.
 * Typically used in login, profile lookup, or authorization logic.
 * <p>
 * This exception is handled globally in {@link GlobalExceptionHandler}
 * and mapped to an HTTP 404 Not Found response.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new UserNotFoundException with a detailed message.
     *
     * @param message the detail message explaining which user was not found
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
