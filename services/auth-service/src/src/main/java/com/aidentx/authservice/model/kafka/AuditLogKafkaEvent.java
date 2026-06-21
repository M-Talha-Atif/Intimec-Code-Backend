package com.aidentx.authservice.model.kafka;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for sending audit log events to Kafka, matching the AuditLog entity in the auditlog-service.
 * Includes all fields required for full audit log persistence, including nested JSON fields.
 */
public class AuditLogKafkaEvent {
    private UUID id;
    private UUID actorId;
    private String actorType;
    private UUID objectId;
    private String objectType;
    private String event;
    private String status;
    private String risk;
    private Instant timestamp;
    private JsonNode details;
    private JsonNode stateBefore;
    private JsonNode stateAfter;
    private JsonNode metadata;
    private String errorDetails;
    private JsonNode governance;

    /**
     * Full constructor for all fields.
     */
    public AuditLogKafkaEvent(UUID id, UUID actorId, String actorType, UUID objectId, String objectType, String event,
                             String status, String risk, Instant timestamp, JsonNode details, JsonNode stateBefore,
                             JsonNode stateAfter, JsonNode metadata, String errorDetails, JsonNode governance) {
        this.id = id;
        this.actorId = actorId;
        this.actorType = actorType;
        this.objectId = objectId;
        this.objectType = objectType;
        this.event = event;
        this.status = status;
        this.risk = risk;
        this.timestamp = timestamp;
        this.details = details;
        this.stateBefore = stateBefore;
        this.stateAfter = stateAfter;
        this.metadata = metadata;
        this.errorDetails = errorDetails;
        this.governance = governance;
    }

    public AuditLogKafkaEvent() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public UUID getObjectId() { return objectId; }
    public void setObjectId(UUID objectId) { this.objectId = objectId; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRisk() { return risk; }
    public void setRisk(String risk) { this.risk = risk; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public JsonNode getDetails() { return details; }
    public void setDetails(JsonNode details) { this.details = details; }
    public JsonNode getStateBefore() { return stateBefore; }
    public void setStateBefore(JsonNode stateBefore) { this.stateBefore = stateBefore; }
    public JsonNode getStateAfter() { return stateAfter; }
    public void setStateAfter(JsonNode stateAfter) { this.stateAfter = stateAfter; }
    public JsonNode getMetadata() { return metadata; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    public JsonNode getGovernance() { return governance; }
    public void setGovernance(JsonNode governance) { this.governance = governance; }
}
