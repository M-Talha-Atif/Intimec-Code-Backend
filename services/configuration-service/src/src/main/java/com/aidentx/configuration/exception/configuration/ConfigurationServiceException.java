package com.aidentx.configuration.exception.configuration;

/**
 * Base exception for all configuration service related errors.
 * <p>
 * This serves as the parent class for all configuration service exceptions,
 * providing a common interface for error handling across the configuration service.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationServiceException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new configuration service exception with the specified message.
     *
     * @param message the detail message
     */
    public ConfigurationServiceException(String message) {
        super(message);
        this.errorCode = "CONFIG_SERVICE_ERROR";
    }

    /**
     * Constructs a new configuration service exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ConfigurationServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CONFIG_SERVICE_ERROR";
    }

    /**
     * Constructs a new configuration service exception with the specified message and error code.
     *
     * @param message   the detail message
     * @param errorCode the specific error code
     */
    public ConfigurationServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new configuration service exception with the specified message, cause, and error code.
     *
     * @param message   the detail message
     * @param cause     the cause of the exception
     * @param errorCode the specific error code
     */
    public ConfigurationServiceException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
