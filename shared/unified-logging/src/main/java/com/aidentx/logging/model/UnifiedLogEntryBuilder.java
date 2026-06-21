// src/main/java/comaidentx/logging/model/UnifiedLogEntryBuilder.java
package com.aidentx.logging.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for creating UnifiedLogEntry instances with a fluent API.
 *
 * This builder provides a convenient way to construct structured log entries
 * with all the necessary information for comprehensive logging.
 *
 */
public class UnifiedLogEntryBuilder {

    private final UnifiedLogEntry logEntry;

    private UnifiedLogEntryBuilder() {
        this.logEntry = new UnifiedLogEntry();
    }

    /**
     * Creates a new builder instance.
     * @return A new builder instance.
     */
    public static UnifiedLogEntryBuilder builder() {
        return new UnifiedLogEntryBuilder();
    }

    /**
     * Sets the unique identifier for the log entry.
     * @param id The unique identifier.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder id(String id) {
        logEntry.setId(id);
        return this;
    }

    /**
     * Sets the log level.
     * @param level The log level (e.g., "TRACE", "DEBUG", "INFO", "WARN", "ERROR").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder level(String level) {
        logEntry.setLevel(level);
        return this;
    }

    /**
     * Sets the source component or service.
     * @param source The source component name.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder source(String source) {
        logEntry.setSource(source);
        return this;
    }

    /**
     * Sets the microservice name.
     * @param microservice The name of the microservice generating the log.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder microservice(String microservice) {
        logEntry.setMicroservice(microservice);
        return this;
    }

    /**
     * Sets the category of the log entry.
     * @param category The log category (e.g., "connector", "authentication").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder category(String category) {
        logEntry.setCategory(category);
        return this;
    }

    /**
     * Sets the type of object being operated on.
     * @param objectType The object type (e.g., "user", "role").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder objectType(String objectType) {
        logEntry.setObjectType(objectType);
        return this;
    }

    /**
     * Sets the identifier of the specific object.
     * @param objectId The object identifier.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder objectId(String objectId) {
        logEntry.setObjectId(objectId);
        return this;
    }

    /**
     * Sets the correlation ID for tracking related operations.
     * @param correlationId The correlation identifier.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder correlationId(String correlationId) {
        logEntry.setCorrelationId(correlationId);
        return this;
    }

    /**
     * Sets the human-readable log message.
     * @param message The log message.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder message(String message) {
        logEntry.setMessage(message);
        return this;
    }

    /**
     * Sets whether the operation can be retried.
     * @param retryable True if retryable, false otherwise.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder retryable(Boolean retryable) {
        logEntry.setRetryable(retryable);
        return this;
    }
    
    // --- New Helper Methods for Common Details ---

    /**
     * Adds the 'operation' detail.
     * @param operation The name of the operation (e.g., "LOGIN", "CREATE_USER").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder operation(String operation) {
        return detail("operation", operation);
    }

    /**
     * Adds the 'clientIP' detail.
     * @param clientIp The client's IP address.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder clientIP(String clientIp) {
        return detail("clientIP", clientIp);
    }

    /**
     * Adds the 'userAgent' detail.
     * @param userAgent The client's User-Agent string.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder userAgent(String userAgent) {
        return detail("userAgent", userAgent);
    }

    /**
     * Adds the 'userId' detail.
     * @param userId The ID of the user involved in the operation.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder userId(String userId) {
        return detail("userId", userId);
    }

    /**
     * Adds the 'authMethod' detail.
     * @param authMethod The authentication method used (e.g., "DATABASE", "LDAP", "SAML").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder authMethod(String authMethod) {
        return detail("authMethod", authMethod);
    }

    /**
     * Adds the 'failureReason' detail for error logging.
     * @param reason A short, machine-readable reason for failure (e.g., "USER_NOT_FOUND").
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder failureReason(String reason) {
        return detail("failureReason", reason);
    }

    /**
     * Adds a detail key-value pair.
     * @param key The detail key.
     * @param value The detail value.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder detail(String key, Object value) {
        if (logEntry.getDetails() == null) {
            logEntry.setDetails(new HashMap<>());
        }
        logEntry.getDetails().put(key, value);
        return this;
    }

    /**
     * Sets all details at once from a map.
     * @param details A map of detail key-value pairs.
     * @return This builder instance.
     */
    public UnifiedLogEntryBuilder details(Map<String, Object> details) {
        logEntry.setDetails(details);
        return this;
    }

    /**
     * Builds and returns the UnifiedLogEntry instance.
     * @return The constructed UnifiedLogEntry.
     */
    public UnifiedLogEntry build() {
        return logEntry;
    }
}