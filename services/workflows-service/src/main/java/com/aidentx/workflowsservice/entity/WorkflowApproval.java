package com.aidentx.workflowsservice.entity;

import com.aidentx.workflowsservice.enums.ApprovalPolicy;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WorkflowApproval Entity - Approval tracking for multi-stage approvals
 * Supports delegation, timeout, and evidence bundles
 */
@Entity
@Table(name = "workflow_approvals", indexes = {
    @Index(name = "idx_approvals_execution", columnList = "execution_id"),
    @Index(name = "idx_approvals_approver", columnList = "approver_id"),
    @Index(name = "idx_approvals_decision", columnList = "decision"),
    @Index(name = "idx_approvals_group", columnList = "approval_group_id"),
    @Index(name = "idx_approvals_tenant", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "node_execution_id")
    private UUID nodeExecutionId;

    // Phase 7: Multi-approval group tracking
    @Column(name = "approval_group_id")
    private UUID approvalGroupId;

    // Phase 1: Approval policy configuration (Week 1-2 enhancement)
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_policy")
    @Builder.Default
    private ApprovalPolicy approvalPolicy = ApprovalPolicy.ALL;

    @Column(name = "required_approvals")
    private Integer requiredApprovals;

    // Approval details
    @Column(name = "approver_id")
    private UUID approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type")
    private ApproverType approverType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalDecision decision = ApprovalDecision.PENDING;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "delegated_to")
    private UUID delegatedTo;

    // Timing
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "timeout_at")
    private OffsetDateTime timeoutAt;

    // Context
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "approval_context")
    private Map<String, Object> approvalContext;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "evidence_bundle")
    private Map<String, Object> evidenceBundle;

    @Type(JsonBinaryType.class)
    @Column(name = "delegation_chain", columnDefinition = "jsonb")
    @Builder.Default
    private List<DelegationRecord> delegationChain = new ArrayList<>();

    // Phase 4: Escalation tracking fields
    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @Column(name = "escalated_from_approval_id")
    private UUID escalatedFromApprovalId;

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    public enum ApproverType {
        MANAGER,     // Manager-based approval
        APP_OWNER,   // Application owner
        ROLE_OWNER,  // Role-based owner
        SPECIFIC,    // Specific user
        DYNAMIC,     // Dynamically resolved
        ESCALATED    // Escalated from another approval
    }

    public enum ApprovalDecision {
        PENDING,           // Awaiting response
        APPROVED,          // Approved by approver
        REJECTED,          // Rejected by approver
        TIMEOUT,           // Timed out without response
        DELEGATED,         // Manually delegated to another user
        ESCALATED,         // Escalated to next level approver
        ESCALATED_TIMEOUT  // Escalated due to timeout (auto-escalation)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DelegationRecord {
        private UUID fromApproverId;
        private UUID toApproverId;
        private OffsetDateTime delegatedAt;
        private String reason;
        private Integer sequenceNumber;
    }
}
