package com.aidentx.authservice.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for password validation operations.
 * <p>
 * This class encapsulates the result of password validation, including
 * whether the password is valid and any error messages that should be
 * displayed to the user. It supports both single error messages and
 * multiple validation error messages.
 * </p>
 *
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-09-11
 */
public class PasswordValidationResponse {

    /**
     * Whether the password validation was successful.
     */
    private boolean valid;

    /**
     * Success or error message.
     */
    private String message;

    /**
     * List of detailed validation error messages.
     * Used when multiple validation rules fail.
     */
    private List<String> errors;

    /**
     * Default constructor.
     */
    public PasswordValidationResponse() {
        this.errors = new ArrayList<>();
    }

    /**
     * Constructor for simple success/failure with single message.
     *
     * @param valid whether validation passed
     * @param message the success or error message
     */
    public PasswordValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
        this.errors = new ArrayList<>();
    }

    /**
     * Constructor for validation failure with multiple error messages.
     *
     * @param valid whether validation passed (should be false)
     * @param message the main error message
     * @param errors list of detailed error messages
     */
    public PasswordValidationResponse(boolean valid, String message, List<String> errors) {
        this.valid = valid;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * Creates a successful validation response.
     *
     * @param message the success message
     * @return PasswordValidationResponse indicating success
     */
    public static PasswordValidationResponse success(String message) {
        return new PasswordValidationResponse(true, message);
    }

    /**
     * Creates a failed validation response with single error message.
     *
     * @param message the error message
     * @return PasswordValidationResponse indicating failure
     */
    public static PasswordValidationResponse failure(String message) {
        return new PasswordValidationResponse(false, message);
    }

    /**
     * Creates a failed validation response with multiple error messages.
     *
     * @param message the main error message
     * @param errors list of detailed error messages
     * @return PasswordValidationResponse indicating failure
     */
    public static PasswordValidationResponse failure(String message, List<String> errors) {
        return new PasswordValidationResponse(false, message, errors);
    }

    /**
     * Adds an error message to the validation response.
     *
     * @param error the error message to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false; // Ensure valid is false when errors are present
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "PasswordValidationResponse{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                ", errors=" + errors +
                '}';
    }
}
