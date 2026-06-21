package com.aidentx.iam.identity_management.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO for API error handling.
 *
 * @author aidentx
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;
    private String message;
    private int status;
    private String path;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private List<String> missingFields;
    private List<ValidationError> validationErrors;
    private List<FieldError> fieldErrors;

    /**
     * Default constructor.
     */
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with basic error information.
     * 
     * @param error The error type
     * @param message The error message
     * @param status The HTTP status code
     * @param path The request path
     */
    public ErrorResponse(String error, String message, int status, String path) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
    }

    /**
     * Constructor for field validation errors with detailed validation information.
     * 
     * @param error The error type
     * @param message The error message
     * @param status The HTTP status code
     * @param path The request path
     * @param missingFields List of missing required fields
     * @param validationErrors List of detailed validation errors
     */
    public ErrorResponse(String error, String message, int status, String path, 
                        List<String> missingFields, List<ValidationError> validationErrors) {
        this(error, message, status, path);
        this.missingFields = missingFields;
        this.validationErrors = validationErrors;
    }

    /**
     * Constructor for field validation errors.
     * 
     * @param error The error type
     * @param message The error message
     * @param status The HTTP status code
     * @param path The request path
     * @param missingFields List of missing required fields
     */
    public ErrorResponse(String error, String message, int status, String path, List<String> missingFields) {
        this(error, message, status, path);
        this.missingFields = missingFields;
    }

    // Getters and Setters

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    /**
     * Inner class for validation error information.
     */
    public static class ValidationError {
        private String field;
        private String fieldDisplayName;
        private String errorType;
        private String errorMessage;

        public ValidationError() {}

        public ValidationError(String field, String fieldDisplayName, String errorType, String errorMessage) {
            this.field = field;
            this.fieldDisplayName = fieldDisplayName;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        // Getters and Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getFieldDisplayName() {
            return fieldDisplayName;
        }

        public void setFieldDisplayName(String fieldDisplayName) {
            this.fieldDisplayName = fieldDisplayName;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Inner class for detailed field error information.
     */
    public static class FieldError {
        private String field;
        private String displayName;
        private String message;
        private Object rejectedValue;

        public FieldError() {}

        public FieldError(String field, String displayName, String message) {
            this.field = field;
            this.displayName = displayName;
            this.message = message;
        }

        public FieldError(String field, String displayName, String message, Object rejectedValue) {
            this(field, displayName, message);
            this.rejectedValue = rejectedValue;
        }

        // Getters and Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}
