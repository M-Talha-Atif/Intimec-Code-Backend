package com.aidentx.iam.exception;

import com.aidentx.iam.config.AppConstants;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when a unique field constraint is violated during identity creation or update.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>A field marked as {@code isUnique=true} in the schema already has the same value for another identity</li>
 *   <li>The uniqueness constraint is enforced per tenant and per schema</li>
 *   <li>The validation occurs during identity creation or update operations</li>
 * </ul>
 * 
 * <p>The exception provides detailed information about:</p>
 * <ul>
 *   <li>Which fields violated the uniqueness constraint</li>
 *   <li>The actual values that caused the violation</li>
 *   <li>Display names for user-friendly error messages</li>
 *   <li>The tenant context where the violation occurred</li>
 * </ul>
 * 
 * <p>Example usage scenarios:</p>
 * <pre>
 * // Single field violation
 * throw new UniqueFieldViolationException("email", "Email Address", "user@example.com", "tenant123");
 * 
 * // Multiple field violations
 * List&lt;String&gt; fields = Arrays.asList("email", "phone");
 * Map&lt;String, String&gt; displayNames = Map.of("email", "Email Address", "phone", "Phone Number");
 * Map&lt;String, Object&gt; values = Map.of("email", "user@example.com", "phone", "+1234567890");
 * throw new UniqueFieldViolationException(fields, displayNames, values, "tenant123");
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 * @see SchemaFieldClassificationService
 * @see GlobalExceptionHandler
 */
public class UniqueFieldViolationException extends RuntimeException {
    
    /**
     * List of field keys that violated the uniqueness constraint.
     */
    private final List<String> violatingFields;
    
    /**
     * Map of field keys to their user-friendly display names.
     */
    private final Map<String, String> fieldDisplayNames;
    
    /**
     * Map of field keys to their actual values that caused the violation.
     */
    private final Map<String, Object> fieldValues;
    
    /**
     * The tenant ID context where the violation occurred.
     */
    private final String tenantId;
    
    /**
     * Constructor for single field violation.
     * 
     * @param fieldKey The key/identifier of the field that violated the uniqueness constraint
     * @param fieldDisplayName The user-friendly display name of the field (can be null, defaults to fieldKey)
     * @param fieldValue The actual value that caused the uniqueness violation
     * @param tenantId The tenant ID context where the violation occurred
     * 
     * @throws IllegalArgumentException if fieldKey, fieldValue, or tenantId is null
     */
    public UniqueFieldViolationException(String fieldKey, String fieldDisplayName, Object fieldValue, String tenantId) {
        super(String.format(AppConstants.ERROR_MESSAGE_UNIQUE_FIELD_SINGLE_FORMAT, 
              fieldDisplayName != null ? fieldDisplayName : fieldKey, fieldValue));
        
        if (fieldKey == null || fieldValue == null || tenantId == null) {
            throw new IllegalArgumentException(AppConstants.EXCEPTION_MESSAGE_NULL_PARAMETERS);
        }
        
        this.violatingFields = List.of(fieldKey);
        this.fieldDisplayNames = Map.of(fieldKey, fieldDisplayName != null ? fieldDisplayName : fieldKey);
        this.fieldValues = Map.of(fieldKey, fieldValue);
        this.tenantId = tenantId;
    }
    
    /**
     * Constructor for multiple field violations.
     * 
     * @param violatingFields List of field keys that violated the uniqueness constraint
     * @param fieldDisplayNames Map of field keys to their user-friendly display names
     * @param fieldValues Map of field keys to their actual values that caused the violation
     * @param tenantId The tenant ID context where the violation occurred
     * 
     * @throws IllegalArgumentException if any required parameter is null or if collections are empty
     * @throws IllegalArgumentException if fieldDisplayNames or fieldValues don't contain entries for all violatingFields
     */
    public UniqueFieldViolationException(List<String> violatingFields, 
                                       Map<String, String> fieldDisplayNames,
                                       Map<String, Object> fieldValues,
                                       String tenantId) {
        super(buildMessage(violatingFields, fieldDisplayNames, fieldValues));
        
        if (violatingFields == null || fieldDisplayNames == null || fieldValues == null || tenantId == null) {
            throw new IllegalArgumentException(AppConstants.EXCEPTION_MESSAGE_ALL_PARAMETERS_NULL);
        }
        if (violatingFields.isEmpty()) {
            throw new IllegalArgumentException(AppConstants.EXCEPTION_MESSAGE_EMPTY_VIOLATING_FIELDS);
        }
        
        // Validate that all violating fields have corresponding display names and values
        for (String field : violatingFields) {
            if (!fieldDisplayNames.containsKey(field)) {
                throw new IllegalArgumentException(AppConstants.EXCEPTION_MESSAGE_MISSING_DISPLAY_NAME + field);
            }
            if (!fieldValues.containsKey(field)) {
                throw new IllegalArgumentException(AppConstants.EXCEPTION_MESSAGE_MISSING_VALUE + field);
            }
        }
        
        this.violatingFields = violatingFields;
        this.fieldDisplayNames = fieldDisplayNames;
        this.fieldValues = fieldValues;
        this.tenantId = tenantId;
    }
    
    /**
     * Builds a user-friendly error message based on the number of violating fields.
     * 
     * @param violatingFields List of field keys that violated the uniqueness constraint
     * @param fieldDisplayNames Map of field keys to their user-friendly display names
     * @param fieldValues Map of field keys to their actual values that caused the violation
     * @return A formatted error message appropriate for the number of violations
     */
    
    private static String buildMessage(List<String> violatingFields, 
                                     Map<String, String> fieldDisplayNames,
                                     Map<String, Object> fieldValues) {
        if (violatingFields.size() == 1) {
            String fieldKey = violatingFields.get(0);
            String displayName = fieldDisplayNames.getOrDefault(fieldKey, fieldKey);
            Object value = fieldValues.get(fieldKey);
            return String.format(AppConstants.ERROR_MESSAGE_UNIQUE_FIELD_SINGLE_FORMAT, 
                               displayName, value);
        } else {
            StringBuilder sb = new StringBuilder(AppConstants.ERROR_MESSAGE_UNIQUE_FIELDS_MULTIPLE_PREFIX);
            for (int i = 0; i < violatingFields.size(); i++) {
                String fieldKey = violatingFields.get(i);
                String displayName = fieldDisplayNames.getOrDefault(fieldKey, fieldKey);
                Object value = fieldValues.get(fieldKey);
                sb.append(String.format(AppConstants.ERROR_MESSAGE_UNIQUE_FIELD_VALUE_FORMAT, displayName, value));
                if (i < violatingFields.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * Gets user-friendly error message for API responses.
     * 
     * <p>This method returns the same message as {@link #getMessage()}, but is provided
     * for semantic clarity when the message is intended for end-user consumption.</p>
     * 
     * @return A user-friendly error message describing the uniqueness violation(s)
     */
    public String getUserFriendlyMessage() {
        return getMessage();
    }
    
    /**
     * Gets the list of field keys that violated the uniqueness constraint.
     * 
     * @return An immutable list of field keys that caused the uniqueness violation
     */
    public List<String> getViolatingFields() {
        return violatingFields;
    }
    
    /**
     * Alias for getViolatingFields() for compatibility with existing code.
     * 
     * @return An immutable list of field keys that caused the uniqueness violation
     * @deprecated Use {@link #getViolatingFields()} instead for better semantic clarity
     */
    @Deprecated
    public List<String> getViolatedFields() {
        return violatingFields;
    }
    
    /**
     * Gets the map of field keys to their user-friendly display names.
     * 
     * @return An immutable map of field keys to display names used in error messages
     */
    public Map<String, String> getFieldDisplayNames() {
        return fieldDisplayNames;
    }
    
    /**
     * Gets the map of field keys to their actual values that caused the violation.
     * 
     * @return An immutable map of field keys to the values that violated uniqueness constraints
     */
    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }
    
    /**
     * Gets the tenant ID context where the violation occurred.
     * 
     * @return The tenant ID where the uniqueness constraint was violated
     */
    public String getTenantId() {
        return tenantId;
    }
}
