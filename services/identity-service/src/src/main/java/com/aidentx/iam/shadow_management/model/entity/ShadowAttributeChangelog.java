package com.aidentx.iam.shadow_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log of attribute-level changes on Shadow objects.
 * <p>
 * Each row records a single field change detected during a sync run.
 * When a connector delivers fresh attributes for an existing shadow,
 * the sync pipeline diffs old vs. new and writes one row per changed field.
 * <p>
 * This provides a complete, immutable audit trail for:
 * <ul>
 *   <li>Compliance reporting — "When did this attribute change and who triggered it?"</li>
 *   <li>Incident investigation — "What changed on this account right before the breach?"</li>
 *   <li>Drift detection — "Which attributes keep changing between sync runs?"</li>
 *   <li>Data quality analysis — "How often does the source system send conflicting data?"</li>
 * </ul>
 */
@Entity
@Table(name = "shadow_attribute_changelog", indexes = {
        @Index(name = "idx_shadow_changelog_shadow_id", columnList = "shadow_id"),
        @Index(name = "idx_shadow_changelog_tenant_sync", columnList = "tenant_id, sync_run_id"),
        @Index(name = "idx_shadow_changelog_changed_at", columnList = "changed_at"),
        @Index(name = "idx_shadow_changelog_field", columnList = "shadow_id, field_name")
})
public class ShadowAttributeChangelog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "shadow_id", nullable = false)
    private UUID shadowId;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotBlank
    @Column(name = "sync_run_id", nullable = false, length = 36)
    private String syncRunId;

    @NotBlank
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @NotBlank
    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * The type of change that occurred.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Long changedAt;

    public enum ChangeType {
        /** A new attribute appeared that didn't exist before */
        ADDED,
        /** An existing attribute's value changed */
        MODIFIED,
        /** An attribute that existed before is no longer present */
        REMOVED
    }

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = Instant.now().toEpochMilli();
        }
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getShadowId() {
        return shadowId;
    }

    public void setShadowId(UUID shadowId) {
        this.shadowId = shadowId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSyncRunId() {
        return syncRunId;
    }

    public void setSyncRunId(String syncRunId) {
        this.syncRunId = syncRunId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public Long getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Long changedAt) {
        this.changedAt = changedAt;
    }
}
