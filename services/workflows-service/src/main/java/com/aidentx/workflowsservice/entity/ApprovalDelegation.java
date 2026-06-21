package com.aidentx.workflowsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ApprovalDelegation Entity - Time-based approval delegation rules
 * 
 * Allows users to set up automatic delegation of approvals to another user
 * for a specific time period (e.g., vacation, leave of absence).
 * 
 * When an approval is created for a user with an active delegation,
 * the system automatically routes it to the delegate instead.
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Entity
@Table(name = "approval_delegations", indexes = {
    @Index(name = "idx_delegation_from_user", columnList = "from_user_id, active"),
    @Index(name = "idx_delegation_to_user", columnList = "to_user_id"),
    @Index(name = "idx_delegation_date_range", columnList = "start_date, end_date"),
    @Index(name = "idx_delegation_tenant", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant ID for multi-tenancy support
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * The user who is delegating their approvals
     */
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    /**
     * The user who will receive the delegated approvals
     */
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    /**
     * Start date/time when the delegation becomes active
     */
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    /**
     * End date/time when the delegation expires
     */
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    /**
     * Reason for delegation (e.g., "Vacation", "Medical Leave", "Business Trip")
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Whether this delegation is currently active
     * Can be set to false to manually disable before end_date
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Delegation scope - what types of approvals are delegated
     * ALL = all approvals, WORKFLOW = workflow approvals only
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    @Builder.Default
    private DelegationScope scope = DelegationScope.ALL;

    /**
     * Optional: Specific workflow IDs this delegation applies to
     * If null, applies to all workflows (based on scope)
     */
    @Column(name = "workflow_ids", columnDefinition = "TEXT")
    private String workflowIds; // Comma-separated UUIDs or null for all

    /**
     * Whether to notify the delegate when approvals are routed
     */
    @Column(name = "notify_delegate", nullable = false)
    @Builder.Default
    private Boolean notifyDelegate = true;

    /**
     * Whether to notify the original user when their approvals are delegated
     */
    @Column(name = "notify_delegator", nullable = false)
    @Builder.Default
    private Boolean notifyDelegator = false;

    /**
     * Timestamp when this delegation was created
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * User who created this delegation (may be admin or the user themselves)
     */
    @Column(name = "created_by")
    private UUID createdBy;

    /**
     * Timestamp when this delegation was last updated
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * User who last updated this delegation
     */
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Delegation scope types
     */
    public enum DelegationScope {
        /**
         * All types of approvals are delegated
         */
        ALL,
        
        /**
         * Only workflow approvals are delegated
         */
        WORKFLOW,
        
        /**
         * Only specific workflows are delegated (use workflowIds field)
         */
        SPECIFIC_WORKFLOWS
    }

    /**
     * Check if this delegation is currently effective (active and within date range)
     */
    public boolean isEffective() {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * Check if this delegation applies to a specific workflow
     */
    public boolean appliesToWorkflow(UUID workflowId) {
        if (scope == DelegationScope.ALL || scope == DelegationScope.WORKFLOW) {
            return true;
        }
        if (scope == DelegationScope.SPECIFIC_WORKFLOWS && workflowIds != null && workflowId != null) {
            return workflowIds.contains(workflowId.toString());
        }
        return false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
