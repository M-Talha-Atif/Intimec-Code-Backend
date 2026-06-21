package com.aidentx.configuration.exception.artifact;

/**
 * Base exception for all artifact service related errors.
 * <p>
 * This serves as the parent class for all artifact service exceptions,
 * providing a common interface for error handling across the artifact service.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ArtifactServiceException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new artifact service exception with the specified message.
     *
     * @param message the detail message
     */
    public ArtifactServiceException(String message) {
        super(message);
        this.errorCode = "ARTIFACT_SERVICE_ERROR";
    }

    /**
     * Constructs a new artifact service exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ArtifactServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ARTIFACT_SERVICE_ERROR";
    }

    /**
     * Constructs a new artifact service exception with the specified message and error code.
     *
     * @param message   the detail message
     * @param errorCode the specific error code
     */
    public ArtifactServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new artifact service exception with the specified message, cause, and error code.
     *
     * @param message   the detail message
     * @param cause     the cause of the exception
     * @param errorCode the specific error code
     */
    public ArtifactServiceException(String message, Throwable cause, String errorCode) {
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
