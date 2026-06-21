package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists the relationship between an identity and an access role.
 *
 * <p>This entity is source-aware: the same identity can hold the same role from
 * different assignment sources, such as a dynamic role rule and a manual grant.
 * The {@code sourceKey} makes uniqueness deterministic even when the source has
 * no rule ID.</p>
 *
 * <p>Assignments are soft-revoked by changing {@code status} to {@code REVOKED}
 * and setting {@code revokedAt}. If the same source grants the same role again,
 * the existing row is reactivated instead of creating a duplicate.</p>
 */
@Entity
@Table(name = "identity_access_role_assignments", indexes = {
        @Index(name = "idx_ira_tenant_identity_status", columnList = "tenant_id, identity_id, status"),
        @Index(name = "idx_ira_tenant_role_status", columnList = "tenant_id, role_id, status"),
        @Index(name = "idx_ira_tenant_rule_status", columnList = "tenant_id, assignment_rule_id, status"),
        @Index(name = "idx_ira_tenant_source", columnList = "tenant_id, source_type, source_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_identity_role_source",
                columnNames = {"tenant_id", "identity_id", "role_id", "source_type", "source_key"})
})
public class IdentityRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** Optional dynamic access-role assignment rule that produced this assignment. */
    @Column(name = "assignment_rule_id")
    private UUID assignmentRuleId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType = SourceType.MANUAL;

    /**
     * Non-null source discriminator used in the unique key.
     *
     * <p>For rule-based assignments this should be the rule UUID. For manual or
     * API assignments it can be a stable actor/request key, or {@code MANUAL}
     * / {@code API} when no finer source exists.</p>
     */
    @NotBlank
    @Column(name = "source_key", nullable = false, length = 120)
    private String sourceKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "assigned_at", nullable = false)
    private Long assignedAt;

    @Column(name = "revoked_at")
    private Long revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    /** User ID of the actor that assigned this role, when known. */
    @Column(name = "assigned_by_user_id", length = 255)
    private String assignedByUserId;

    /** Human-readable username/name of the actor that assigned this role, when known. */
    @Column(name = "assigned_by_user_name", length = 255)
    private String assignedByUserName;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        if (assignedAt == null) {
            assignedAt = now;
        }
        if (sourceKey == null || sourceKey.isBlank()) {
            sourceKey = buildDefaultSourceKey(sourceType, assignmentRuleId);
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        if (sourceKey == null || sourceKey.isBlank()) {
            sourceKey = buildDefaultSourceKey(sourceType, assignmentRuleId);
        }
        updatedAt = Instant.now().toEpochMilli();
    }

    /** Reactivates a previously revoked assignment for the same source. */
    public void reactivate() {
        this.status = Status.ACTIVE;
        this.revokedAt = null;
        this.assignedAt = Instant.now().toEpochMilli();
    }

    /** Soft-revokes the assignment. */
    public void revoke() {
        this.status = Status.REVOKED;
        this.revokedAt = Instant.now().toEpochMilli();
    }

    /** Assignment lifecycle status. */
    public enum Status {
        ACTIVE,
        REVOKED
    }

    /** Source that caused an identity to receive a role. */
    public enum SourceType {
        RULE,
        MANUAL,
        API,
        IMPORT,
        SYNC
    }

    public static String buildDefaultSourceKey(SourceType sourceType, UUID assignmentRuleId) {
        if (assignmentRuleId != null) {
            return assignmentRuleId.toString();
        }
        SourceType effectiveSource = sourceType != null ? sourceType : SourceType.MANUAL;
        return effectiveSource.name();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getAssignmentRuleId() { return assignmentRuleId; }
    public void setAssignmentRuleId(UUID assignmentRuleId) { this.assignmentRuleId = assignmentRuleId; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Long getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Long assignedAt) { this.assignedAt = assignedAt; }

    public Long getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Long revokedAt) { this.revokedAt = revokedAt; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getAssignedByUserId() { return assignedByUserId; }
    public void setAssignedByUserId(String assignedByUserId) { this.assignedByUserId = assignedByUserId; }

    public String getAssignedByUserName() { return assignedByUserName; }
    public void setAssignedByUserName(String assignedByUserName) { this.assignedByUserName = assignedByUserName; }
}

