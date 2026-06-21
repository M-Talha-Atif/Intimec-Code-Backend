package com.aidentx.authservice.exception;

import com.aidentx.authservice.config.AppConstants;

/**
 * Exception thrown when the provided refresh token is invalid or expired.
 * <p>
 * This exception is typically thrown by authentication or token refresh logic when the
 * provided refresh token is missing, malformed, expired, revoked, or otherwise invalid.
 * It is handled globally to return a 401 Unauthorized or 400 Bad Request response.
 * </p>
 *
 * Example usage:
 * <pre>
 *   throw new InvalidRefreshTokenException(AppConstants.ERROR_INVALID_REFRESH_TOKEN);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class InvalidRefreshTokenException extends RuntimeException {

    /**
     * Constructs a new InvalidRefreshTokenException with a default message from AppConstants.
     */
    public InvalidRefreshTokenException() {
        super(AppConstants.ERROR_INVALID_REFRESH_TOKEN);
    }

    /**
     * Constructs a new InvalidRefreshTokenException with the specified detail message.
     *
     * @param message the detail message describing the invalid refresh token error
     */
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
