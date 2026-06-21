package com.aidentx.authservice.exception;

import com.aidentx.authservice.config.AppConstants;

/**
 * Exception thrown when a password reset token is expired or invalid.
 * <p>
 * This exception is typically thrown by password reset logic when the provided token
 * is missing, expired, or otherwise invalid. It is handled globally to return a 400 Bad Request
 * or 401 Unauthorized response with structured error details.
 * </p>
 *
 * Example usage:
 * <pre>
 *   throw new PasswordResetTokenExpiredException(AppConstants.PASSWORD_RESET_TOKEN_EXPIRED);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class PasswordResetTokenExpiredException extends RuntimeException {
    /**
     * Constructs a new PasswordResetTokenExpiredException with a default message from AppConstants.
     */
    public PasswordResetTokenExpiredException() {
        super(AppConstants.PASSWORD_RESET_TOKEN_EXPIRED);
    }

    /**
     * Constructs a new PasswordResetTokenExpiredException with the specified detail message.
     *
     * @param message the detail message describing the expired or invalid token error
     */
    public PasswordResetTokenExpiredException(String message) {
        super(message);
    }
}
