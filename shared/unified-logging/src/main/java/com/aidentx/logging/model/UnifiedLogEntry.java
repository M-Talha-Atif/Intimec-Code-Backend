package com.aidentx.logging.model;

// import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified structured log entry model for all aidentx microservices.
 * 
 * This class represents a standardized log entry format that provides
 * comprehensive information for monitoring, debugging, and operational
 * insights.
 * 
 * Example JSON output:
 * {
 * "id": "1",
 * "timestamp": "2024-01-15 14:23:45.123",
 * "level": "ERROR",
 * "source": "SAP Connector",
 * "category": "connector",
 * "objectType": "user",
 * "objectId": "john.doe@company.com",
 * "correlationId": "req-789-abc",
 * "message": "Failed to provision user account in SAP - Connection timeout
 * after 30 seconds",
 * "retryable": true,
 * "details": {
 * "operation": "CREATE_USER",
 * "targetSystem": "SAP_PROD",
 * "errorCode": "CONN_TIMEOUT",
 * "attemptNumber": 3,
 * "lastSuccessfulConnection": "2024-01-15 14:20:12",
 * "requestPayload": {
 * "username": "john.doe",
 * "firstName": "John",
 * "lastName": "Doe",
 * "department": "Engineering",
 * "manager": "jane.smith@company.com"
 * }
 * }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedLogEntry {

    /**
     * Unique identifier for this log entry
     */
    @JsonProperty("id")
    private String id;

    /**
     * Timestamp when the log entry was created
     * Format: yyyy-MM-dd HH:mm:ss.SSS
     */
    @JsonProperty("@timestamp")
    // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private long timestamp;

    /**
     * Log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
     */
    @JsonProperty("level")
    private String level;

    /**
     * Source component or service generating the log
     */
    @JsonProperty("source")
    private String source;

    @JsonProperty("microservice")
    private String microservice;

    /**
     * Category of the log entry (e.g., "connector", "authentication",
     * "configuration")
     */
    @JsonProperty("category")
    private String category;

    /**
     * Type of object being operated on (e.g., "user", "role", "configuration")
     */
    @JsonProperty("objectType")
    private String objectType;

    /**
     * Identifier of the specific object (e.g., user email, role name, config key)
     */
    @JsonProperty("objectId")
    private String objectId;

    /**
     * Correlation ID for tracking related operations across services
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Human-readable log message
     */
    @JsonProperty("message")
    private String message;

    /**
     * Indicates whether the operation that failed can be retried
     */
    @JsonProperty("retryable")
    private Boolean retryable;

    /**
     * Additional structured details as key-value pairs
     */
    @JsonProperty("details")
    private Map<String, Object> details;

    // Default constructor
    public UnifiedLogEntry() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(String microservice) {
        this.microservice = microservice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    /**
     * Overridden to provide a clean, human-readable summary for console logging.
     * This is used by Log4j2's PatternLayout (%msg).
     *
     * @return A formatted string representation of the log entry.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(message);

        // Append objectId if it exists
        if (objectId != null) {
            sb.append(" [").append(objectType != null ? objectType : "id").append(": ").append(objectId).append("]");
        }

        // Append key details if they exist
        if (details != null && !details.isEmpty()) {
            String detailsString = details.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", ", " {", "}"));
            sb.append(" - ").append(detailsString);
        }

        return sb.toString();
    }
}
