package com.aidentx.llmservice.exception;

/**
 * Exception thrown when a requested LLM provider is not found or unavailable.
 */
public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message) {
        super(message);
    }

    public ProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
