package com.aidentx.core.audit_management.model.entity;

import com.aidentx.core.utils.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a detailed audit log entry capturing all relevant information about
 * actions performed within the system. It includes identities of the actor and target object,
 * event metadata, risk classification, timestamps, state snapshots, and governance data.
 * Complex nested objects are stored as JSONB for flexibility and extensibility.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    /**
     * Unique identifier of this audit log entry.
     */
    @Id
    @jakarta.persistence.GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Unique identifier of the actor who performed the action.
     */
    @Column(name = "actor_id", nullable = true)
    private UUID actorId;

    /**
     * The type of actor performing the action (e.g., user, system).
     */
    @Column(name = "actor_type", nullable = true)
    private String actorType;

    /**
     * The name of the actor performing the action (e.g., username, system name).
     */
    @Column(name = "actor_name", nullable = true)
    private String actorName;

    /**
     * Tenant identifier for multi-tenant support. This value is extracted from the JWT token
     * via the metadata field in incoming Kafka audit events.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Unique identifier of the object targeted by the action.
     */
    @Column(name = "object_id", nullable = true)
    private UUID objectId;

    /**
     * Type of the object targeted by the action (e.g., document, account).
     */
    @Column(name = "object_type")
    private String objectType;

    /**
     * Name or identifier of the event being logged.
     */
    @Column(nullable = false)
    private String event;

    /**
     * Status of the event such as SUCCESS or FAILURE.
     */
    @Column(nullable = false)
    private String status;

    /**
     * Optional risk assessment associated with the event.
     */
    @Column(nullable = true)
    private String risk;

    /**
     * Timestamp indicating when the event occurred.
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * JSONB column containing detailed contextual information about the event,
     * serialized from {@link LogDetails}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode details;

    /**
     * JSONB column capturing the state of the system or object prior to the event.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb", nullable = true)
    private JsonNode stateBefore;

    /**
     * JSONB column capturing the state of the system or object after the event.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb", nullable = true)
    private JsonNode stateAfter;

    /**
     * JSONB column holding additional metadata associated with the event.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb", nullable = true)
    private JsonNode metadata;

    /**
     * Optional string providing error details if the event failed.
     */
    @Column(name = "error_details",nullable = true)
    private String errorDetails;


    /**
     * JSONB column storing governance-related information. This can be any JSON structure for compliance, ownership, or traceability.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode governance;

    /**
     * JSONB column storing information about the target object of the event. This can be any JSON structure (e.g., id, type, name, etc).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode targetObject;

    /**
     * Default constructor generating a timestamp set to now.
     * The tenantId must be set separately from the JWT token context via metadata.
     */
    public AuditLog() {
        this.timestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public JsonNode getStateBefore() {
        return stateBefore;
    }

    public void setStateBefore(JsonNode stateBefore) {
        this.stateBefore = stateBefore;
    }

    public JsonNode getStateAfter() {
        return stateAfter;
    }

    public void setStateAfter(JsonNode stateAfter) {
        this.stateAfter = stateAfter;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public JsonNode getGovernance() {
        return governance;
    }

    public void setGovernance(JsonNode governance) {
        this.governance = governance;
    }

    public JsonNode getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(JsonNode targetObject) {
        this.targetObject = targetObject;
    }
}