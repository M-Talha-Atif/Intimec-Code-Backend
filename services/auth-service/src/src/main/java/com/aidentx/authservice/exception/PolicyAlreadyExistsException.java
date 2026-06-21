package com.aidentx.authservice.exception;

/**
 * Exception thrown when attempting to create a policy that already exists for a tenant.
 * <p>
 * This is a runtime exception that indicates a violation of the business rule
 * that each tenant can have only one policy of a specific type.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class PolicyAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new PolicyAlreadyExistsException with the specified detail message.
     *
     * @param message the detail message
     */
    public PolicyAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new PolicyAlreadyExistsException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public PolicyAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
