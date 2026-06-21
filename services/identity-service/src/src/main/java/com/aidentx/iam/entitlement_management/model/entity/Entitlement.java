package com.aidentx.iam.entitlement_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a canonical Technical Entitlement in the IGA platform.
 * An Entitlement is a fine-grained unit of access as it exists on a target
 * system,
 * such as an Active Directory group, a Salesforce Permission Set, or a database
 * role.
 * This entity serves as the central catalog for all such entitlements.
 *
 * Entitlements are imported from connectors (e.g., __GROUP__ object class)
 * and linked to accounts via {@code EntitlementAssignment}.
 */
@Entity
@Table(name = "entitlements", indexes = {
        @Index(name = "idx_entitlement_resource_source_uid", columnList = "resource_id, source_uid, tenant_id", unique = true),
        @Index(name = "idx_entitlement_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_entitlement_last_sync_run", columnList = "resource_id, tenant_id, last_sync_run_id")
})
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "dgraph_uid")
    private String dgraphUid;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotBlank
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * The unique identifier of this entitlement ON THE SOURCE SYSTEM.
     * For AD, this could be the objectGUID or distinguishedName. For Salesforce,
     * the Profile ID.
     * This, combined with resourceId, forms a natural key.
     */
    @NotBlank
    @Column(name = "source_uid", nullable = false, columnDefinition = "TEXT")
    private String sourceUid;

    /**
     * The ConnID object class this entitlement was imported from.
     * Typically "__GROUP__" but can be custom (e.g., "organizationalRole").
     */
    @Column(name = "object_class", length = 100)
    private String objectClass;

    /**
     * The kind of entitlement (e.g., "entitlement", "group", "role").
     * Follows MidPoint kind/intent classification.
     */
    @Column(name = "kind", length = 50)
    private String kind;

    /**
     * All attributes of this entitlement from the source system.
     * Stored as JSONB for flexibility — AD group attributes, SAP role properties, etc.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    /**
     * Tracks which sync run last touched this entitlement.
     * Used for deletion detection — entitlements not seen in the current run
     * may have been removed from the source system.
     */
    @Column(name = "last_sync_run_id", length = 36)
    private String lastSyncRunId;

    // ===================================================================
    //  Governance Metadata — set by IGA admins, not by sync imports.
    //  These fields represent the organizational policies and risk
    //  classifications applied to entitlements after they are discovered
    //  from target systems. Standard IGA catalog fields per MidPoint/SailPoint.
    // ===================================================================

    /**
     * Lifecycle state of this entitlement in the IGA catalog.
     * ACTIVE = in use, requestable. DEPRECATED = being phased out.
     * ORPHANED = no owner or policy. INACTIVE = disabled.
     */
    @Column(name = "lifecycle", length = 20)
    private String lifecycle = "Active";

    /**
     * Risk classification assigned by governance team or AI analysis.
     */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /**
     * Explanation of the risk classification (human or AI generated).
     */
    @Column(name = "risk_reason", columnDefinition = "TEXT")
    private String riskReason;

    /**
     * Whether this entitlement can be requested through self-service.
     */
    @Column(name = "requestable", nullable = false)
    private boolean requestable = false;

    /**
     * Governance classifications (e.g., "SOX", "PII", "GDPR", "High Risk").
     * Stored as JSONB array for flexible multi-label classification.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "classification", columnDefinition = "jsonb")
    private List<String> classification;

    /**
     * The type of identity this entitlement applies to.
     * Employee, Non-Employee, Non-Human — used for access request filtering.
     */
    @Column(name = "identity_type", length = 30)
    private String identityType;

    /**
     * Privilege level classification (Read, Write, Admin).
     */
    @Column(name = "privilege_level", length = 20)
    private String privilegeLevel;

    /**
     * UUID of the identity that owns/governs this entitlement.
     * Owners are responsible for access reviews and certification.
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Whether granting this entitlement requires approval workflow.
     */
    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval = false;

    /**
     * Whether automated provisioning systems can grant this entitlement
     * without human intervention (e.g., birthright entitlements).
     */
    @Column(name = "allowed_automated_grant", nullable = false)
    private boolean allowedAutomatedGrant = false;

    /**
     * Maximum duration (in milliseconds) an assignment can persist.
     * Null means no limit (permanent assignment).
     */
    @Column(name = "max_duration_ms")
    private Long maxDurationMs;

    /**
     * Auto-expiry duration (in milliseconds) for time-boxed access.
     * Null means no auto-expiry.
     */
    @Column(name = "auto_expire_duration_ms")
    private Long autoExpireDurationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDgraphUid() {
        return dgraphUid;
    }

    public void setDgraphUid(String dgraphUid) {
        this.dgraphUid = dgraphUid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getSourceUid() {
        return sourceUid;
    }

    public void setSourceUid(String sourceUid) {
        this.sourceUid = sourceUid;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getLastSyncRunId() {
        return lastSyncRunId;
    }

    public void setLastSyncRunId(String lastSyncRunId) {
        this.lastSyncRunId = lastSyncRunId;
    }

    // --- Governance Getters and Setters ---

    public String getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }

    public boolean isRequestable() {
        return requestable;
    }

    public void setRequestable(boolean requestable) {
        this.requestable = requestable;
    }

    public List<String> getClassification() {
        return classification;
    }

    public void setClassification(List<String> classification) {
        this.classification = classification;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(String privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public boolean isAllowedAutomatedGrant() {
        return allowedAutomatedGrant;
    }

    public void setAllowedAutomatedGrant(boolean allowedAutomatedGrant) {
        this.allowedAutomatedGrant = allowedAutomatedGrant;
    }

    public Long getMaxDurationMs() {
        return maxDurationMs;
    }

    public void setMaxDurationMs(Long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }

    public Long getAutoExpireDurationMs() {
        return autoExpireDurationMs;
    }

    public void setAutoExpireDurationMs(Long autoExpireDurationMs) {
        this.autoExpireDurationMs = autoExpireDurationMs;
    }
}