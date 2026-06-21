package com.aidentx.authservice.exception;

/**
 * CustomException is a generic runtime exception for application-specific errors.
 * <p>
 * This exception can be thrown when a specific, meaningful error needs to be communicated
 * from the service or business logic layer, and is typically handled by a global exception handler.
 * </p>
 *
 * Example usage:
 * <pre>
 *   throw new CustomException(AppConstants.SOME_ERROR_MESSAGE);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class CustomException extends RuntimeException {
    /**
     * Constructs a new CustomException with the specified detail message.
     *
     * @param message the detail message describing the exception
     */
    public CustomException(String message) {
        super(message);
    }
}