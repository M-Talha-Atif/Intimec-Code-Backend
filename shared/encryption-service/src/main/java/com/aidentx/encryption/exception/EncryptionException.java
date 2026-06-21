package com.aidentx.encryption.exception;

/**
 * Exception thrown when an error occurs during encryption operations
 * such as encrypting data or handling encryption keys in the encryption system.
 */
public class EncryptionException extends RuntimeException {
    /**
     * Constructs a new EncryptionException with the specified detail message.
     * @param message the detail message
     */
    public EncryptionException(String message) {
        super(message);
    }

    /**
     * Constructs a new EncryptionException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}