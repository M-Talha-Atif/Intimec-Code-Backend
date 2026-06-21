package com.aidentx.authservice.exception;

import java.util.List;

/**
 * Exception thrown when password validation fails against password policy.
 * <p>
 * This exception carries a list of specific validation errors that occurred
 * during password validation, allowing the caller to provide detailed
 * feedback to the user.
 * </p>
 * 
 * @author Aidentx
 * @since 1.0
 */
public class PasswordValidationException extends RuntimeException {

    private final List<String> validationErrors;

    /**
     * Constructs a new PasswordValidationException with the specified validation errors.
     *
     * @param validationErrors the list of validation error messages
     */
    public PasswordValidationException(List<String> validationErrors) {
        super(buildErrorMessage(validationErrors));
        this.validationErrors = validationErrors;
    }

    /**
     * Gets the list of validation error messages.
     *
     * @return the validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Builds a combined error message from the validation errors.
     */
    private static String buildErrorMessage(List<String> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return "Password validation failed";
        }
        return "Password validation failed: " + String.join(", ", validationErrors);
    }
}
