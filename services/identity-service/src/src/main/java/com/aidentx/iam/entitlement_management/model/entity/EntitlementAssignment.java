package com.aidentx.iam.entitlement_management.model.entity;

import com.aidentx.iam.entitlement_management.config.constants.EntitlementAssignmentSourceConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the assignment of an Entitlement to a Shadow (account).
 * This is the fundamental "who has what" data in IGA — it records which
 * accounts possess which entitlements on a given resource.
 *
 * <p>Populated during import by parsing membership attributes (e.g., AD memberOf,
 * SAP role assignments) from account shadows, or from connector group membership data.
 *
 * <p>Uniqueness is source-scoped: an identity/account can hold separate DIRECT,
 * DYNAMIC, and ROLE_DERIVED assignments for the same entitlement. DIRECT represents
 * manual/import grants, DYNAMIC represents entitlement-rule-created grants, and
 * ROLE_DERIVED represents access-role-created grants.
 */
@Entity
@Table(name = "entitlement_assignments", indexes = {
        @Index(name = "idx_ea_shadow_entitlement", columnList = "shadow_id, entitlement_id, tenant_id"),
        @Index(name = "idx_ea_shadow_entitlement_source", columnList = "shadow_id, entitlement_id, tenant_id, source"),
        @Index(name = "idx_ea_identity_entitlement", columnList = "identity_id, entitlement_id, tenant_id, source"),
        @Index(name = "idx_ea_entitlement_id", columnList = "entitlement_id, tenant_id"),
        @Index(name = "idx_ea_resource_id", columnList = "resource_id, tenant_id"),
        @Index(name = "idx_ea_access_role", columnList = "tenant_id, access_role_id"),
        @Index(name = "idx_ea_last_sync_run", columnList = "resource_id, tenant_id, last_sync_run_id")
})
public class EntitlementAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * The account shadow that holds this entitlement.
     * Null when the identity has no account yet and the assignment is pending.
     */
    @Column(name = "shadow_id")
    private UUID shadowId;

    /**
     * Identity that should receive this entitlement.
     * Populated for pending assignments and also for platform-created assignments
     * when the identity is known.
     */
    @Column(name = "identity_id")
    private UUID identityId;

    /**
     * The entitlement being assigned.
     */
    @NotNull
    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    /**
     * The resource (application) this assignment belongs to.
     * Denormalized for efficient per-resource queries.
     */
    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * How this assignment was discovered.
     * DIRECT = assigned outside entitlement rules, including manual/UI and file/import flows.
     * DYNAMIC = assigned by a dynamic rule and controlled by rule reconciliation.
     * ROLE_DERIVED = assigned because the identity holds an access role.
     */
    @NotBlank
    @Column(name = "source", nullable = false, length = 20)
    private String source = EntitlementAssignmentSourceConstants.SOURCE_DIRECT;

    /** Assignment lifecycle status. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    /**
     * Tracks which sync run last confirmed this assignment.
     * Assignments not seen in the current run may have been revoked on the target system.
     */
    @Column(name = "last_sync_run_id", length = 36)
    private String lastSyncRunId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Long assignedAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    /** User ID of the actor that assigned this direct entitlement, when known. */
    @Column(name = "assigned_by_user_id", length = 255)
    private String assignedByUserId;

    /** Human-readable username/name of the actor that assigned this direct entitlement, when known. */
    @Column(name = "assigned_by_user_name", length = 255)
    private String assignedByUserName;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        this.assignedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * The rule that created this assignment.
     * NULL for DIRECT assignments.
     * Set when source = DYNAMIC.
     */
    @Column(name = "rule_id")
    private UUID ruleId;

    /**
     * Access role that caused this entitlement assignment when it is role-derived.
     * NULL for import/manual direct entitlement rows and for legacy rows created
     * before role provenance was captured.
     */
    @Column(name = "access_role_id")
    private UUID accessRoleId;

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public UUID getAccessRoleId() {
        return accessRoleId;
    }

    public void setAccessRoleId(UUID accessRoleId) {
        this.accessRoleId = accessRoleId;
    }

    public enum AssignmentStatus {
        ASSIGNED,
        PENDING
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getShadowId() {
        return shadowId;
    }

    public void setShadowId(UUID shadowId) {
        this.shadowId = shadowId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public UUID getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(UUID entitlementId) {
        this.entitlementId = entitlementId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public String getLastSyncRunId() {
        return lastSyncRunId;
    }

    public void setLastSyncRunId(String lastSyncRunId) {
        this.lastSyncRunId = lastSyncRunId;
    }

    public Long getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Long assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(String assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public String getAssignedByUserName() {
        return assignedByUserName;
    }

    public void setAssignedByUserName(String assignedByUserName) {
        this.assignedByUserName = assignedByUserName;
    }
}
