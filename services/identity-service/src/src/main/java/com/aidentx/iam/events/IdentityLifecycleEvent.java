package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an identity is created, updated, or deleted.
 * Consumed by Auth service to manage user accounts.
 *
 * @author AidentX Development Team
 * @version 1.0
 */
public class IdentityLifecycleEvent {

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("eventType")
    private String eventType; // identity.created, identity.updated, identity.deleted

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("source")
    private String source = "identity-service";

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("identityId")
    private UUID identityId;

    @JsonProperty("identityType")
    private String identityType; // EMPLOYEE, NON_EMPLOYEE, NON_HUMAN

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("status")
    private String status; // ACTIVE, INACTIVE, SUSPENDED

    /**
     * Additional identity-specific data (e.g., employeeId, contractId, serviceName).
     */
    @JsonProperty("additionalData")
    private Map<String, Object> additionalData;

    /**
     * Metadata about the event (e.g., triggeredBy, source system).
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public IdentityLifecycleEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }

    public IdentityLifecycleEvent(String eventType, String tenantId, UUID identityId,
                                  String identityType, String username, String email) {
        this();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.identityId = identityId;
        this.identityType = identityType;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}