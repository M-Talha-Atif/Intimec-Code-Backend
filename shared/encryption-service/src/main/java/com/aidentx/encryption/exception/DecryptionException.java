package com.aidentx.encryption.exception;

/**
 * Exception thrown when an error occurs during decryption operations
 * such as decrypting data or handling decryption keys in the encryption system.
 */
public class DecryptionException extends RuntimeException {
    /**
     * Constructs a new DecryptionException with the specified detail message.
     * @param message the detail message
     */
    public DecryptionException(String message) {
        super(message);
    }

    /**
     * Constructs a new DecryptionException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}