package com.aidentx.iam.organization_management.model.entity;

import com.aidentx.iam.organization_management.model.enums.OrgEventType;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only event log for an {@link OrgUnit}. Backs the frontend
 * {@code OrgChangeHistory} array.
 */
@Entity
@Table(name = "org_unit_history", indexes = {
        @Index(name = "idx_org_history_unit_time", columnList = "tenant_id,org_unit_id,occurred_at")
})
public class OrgUnitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /** Not a strict FK — must survive deletion of the org unit. */
    @NotNull
    @Column(name = "org_unit_id", nullable = false)
    private UUID orgUnitId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private OrgEventType eventType;

    @Column(name = "actor_identity_id")
    private UUID actorIdentityId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "action_label", length = 255)
    private String actionLabel;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Type(JsonBinaryType.class)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private JsonNode beforeState;

    @Type(JsonBinaryType.class)
    @Column(name = "after_state", columnDefinition = "jsonb")
    private JsonNode afterState;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(UUID orgUnitId) { this.orgUnitId = orgUnitId; }

    public OrgEventType getEventType() { return eventType; }
    public void setEventType(OrgEventType eventType) { this.eventType = eventType; }

    public UUID getActorIdentityId() { return actorIdentityId; }
    public void setActorIdentityId(UUID actorIdentityId) { this.actorIdentityId = actorIdentityId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public JsonNode getBeforeState() { return beforeState; }
    public void setBeforeState(JsonNode beforeState) { this.beforeState = beforeState; }

    public JsonNode getAfterState() { return afterState; }
    public void setAfterState(JsonNode afterState) { this.afterState = afterState; }
}
