package com.aidentx.encryption.exception;

/**
 * Exception thrown when an error occurs during key management operations
 * such as key generation, storage, retrieval, or rotation in the encryption system.
 */
public class KeyManagementException extends RuntimeException {
    /**
     * Constructs a new KeyManagementException with the specified detail message.
     * @param message the detail message
     */
    public KeyManagementException(String message) {
        super(message);
    }

    /**
     * Constructs a new KeyManagementException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public KeyManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}