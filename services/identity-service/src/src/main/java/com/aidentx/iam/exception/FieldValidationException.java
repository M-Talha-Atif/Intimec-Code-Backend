package com.aidentx.iam.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom exception for field validation errors.
 * Provides detailed information about missing required fields.
 *
 * @author aidentx
 * @since 1.0
 */
public class FieldValidationException extends RuntimeException {

    private final List<String> missingRequiredFields;
    private final Map<String, String> fieldDisplayNames;

    /**
     * Constructor with missing required fields and their display names (auto-generates message).
     * 
     * @param missingRequiredFields List of missing required field system keys
     * @param fieldDisplayNames Map of field system keys to display names
     */
    public FieldValidationException(List<String> missingRequiredFields, Map<String, String> fieldDisplayNames) {
        super(generateMessage(missingRequiredFields, fieldDisplayNames));
        this.missingRequiredFields = missingRequiredFields;
        this.fieldDisplayNames = fieldDisplayNames;
    }

    /**
     * Constructor with missing required fields and their display names.
     * 
     * @param message The error message
     * @param missingRequiredFields List of missing required field system keys
     * @param fieldDisplayNames Map of field system keys to display names
     */
    public FieldValidationException(String message, List<String> missingRequiredFields, Map<String, String> fieldDisplayNames) {
        super(message);
        this.missingRequiredFields = missingRequiredFields;
        this.fieldDisplayNames = fieldDisplayNames;
    }

    /**
     * Constructor with simple message.
     * 
     * @param message The error message
     */
    public FieldValidationException(String message) {
        super(message);
        this.missingRequiredFields = null;
        this.fieldDisplayNames = null;
    }

    /**
     * Gets the list of missing required fields (alias for backward compatibility).
     * 
     * @return List of missing field system keys
     */
    public List<String> getMissingFields() {
        return missingRequiredFields;
    }

    /**
     * Gets the list of missing required fields.
     * 
     * @return List of missing field system keys
     */
    public List<String> getMissingRequiredFields() {
        return missingRequiredFields;
    }

    /**
     * Gets the list of missing field display names.
     * 
     * @return List of missing field display names
     */
    public List<String> getMissingFieldDisplayNames() {
        if (missingRequiredFields == null || fieldDisplayNames == null) {
            return missingRequiredFields != null ? missingRequiredFields : List.of();
        }
        
        return missingRequiredFields.stream()
                .map(fieldKey -> fieldDisplayNames.getOrDefault(fieldKey, fieldKey))
                .collect(Collectors.toList());
    }

    /**
     * Gets the field display names mapping.
     * 
     * @return Map of system keys to display names
     */
    public Map<String, String> getFieldDisplayNames() {
        return fieldDisplayNames;
    }

    /**
     * Generates a user-friendly error message with field display names.
     * 
     * @return Formatted error message
     */
    public String getUserFriendlyMessage() {
        if (missingRequiredFields == null || missingRequiredFields.isEmpty()) {
            return getMessage();
        }

        StringBuilder sb = new StringBuilder("The following required fields are missing: ");
        for (int i = 0; i < missingRequiredFields.size(); i++) {
            String fieldKey = missingRequiredFields.get(i);
            String displayName = fieldDisplayNames != null ? fieldDisplayNames.get(fieldKey) : fieldKey;
            
            sb.append("'").append(displayName != null ? displayName : fieldKey).append("'");
            
            if (i < missingRequiredFields.size() - 1) {
                sb.append(", ");
            }
        }
        
        return sb.toString();
    }

    /**
     * Generates a default error message from missing fields and display names.
     * 
     * @param missingFields List of missing field system keys
     * @param fieldDisplayNames Map of system keys to display names
     * @return Generated error message
     */
    private static String generateMessage(List<String> missingFields, Map<String, String> fieldDisplayNames) {
        if (missingFields == null || missingFields.isEmpty()) {
            return "Field validation failed";
        }

        StringBuilder sb = new StringBuilder("Required fields missing: ");
        for (int i = 0; i < missingFields.size(); i++) {
            String fieldKey = missingFields.get(i);
            String displayName = fieldDisplayNames != null ? fieldDisplayNames.get(fieldKey) : fieldKey;
            
            sb.append(displayName != null ? displayName : fieldKey);
            
            if (i < missingFields.size() - 1) {
                sb.append(", ");
            }
        }
        
        return sb.toString();
    }
}
