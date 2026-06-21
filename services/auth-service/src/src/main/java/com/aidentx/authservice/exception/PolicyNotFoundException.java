package com.aidentx.authservice.exception;

/**
 * Exception thrown when attempting to access a policy that does not exist for a tenant.
 * <p>
 * This is a runtime exception that indicates the requested policy
 * could not be found for the specified tenant.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class PolicyNotFoundException extends RuntimeException {

    /**
     * Constructs a new PolicyNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public PolicyNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new PolicyNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public PolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
