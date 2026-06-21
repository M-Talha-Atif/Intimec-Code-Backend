package com.aidentx.access_portal_service.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Exception for validation errors
 */
@Getter
public class ValidationException extends RuntimeException {

    private Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
