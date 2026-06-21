package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowApproval entity operations
 */
@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApproval, UUID> {

    List<WorkflowApproval> findByExecutionId(UUID executionId);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.approverId = :approverId AND a.tenantId = :tenantId")
    List<WorkflowApproval> findByApproverIdAndTenantId(@Param("approverId") UUID approverId, @Param("tenantId") String tenantId);

    List<WorkflowApproval> findByDecision(WorkflowApproval.ApprovalDecision decision);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.approverId = :approverId AND a.tenantId = :tenantId AND a.decision = 'PENDING'")
    List<WorkflowApproval> findPendingApprovalsByApprover(@Param("approverId") UUID approverId, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.delegatedTo = :userId AND a.tenantId = :tenantId AND a.decision = 'PENDING'")
    List<WorkflowApproval> findDelegatedPendingApprovals(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.decision = 'PENDING' AND a.timeoutAt < :now")
    List<WorkflowApproval> findTimedOutApprovals(@Param("now") OffsetDateTime now);

    @Query("SELECT a FROM WorkflowApproval a WHERE (a.approverId = :userId OR a.delegatedTo = :userId) AND a.tenantId = :tenantId AND a.decision = 'PENDING'")
    List<WorkflowApproval> findAllPendingApprovalsForUser(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.executionId = :executionId AND a.nodeExecutionId = :nodeExecutionId")
    List<WorkflowApproval> findByExecutionAndNode(
        @Param("executionId") UUID executionId,
        @Param("nodeExecutionId") UUID nodeExecutionId
    );

    // Phase 7: Find all approvals in a multi-approval group
    List<WorkflowApproval> findByApprovalGroupId(UUID approvalGroupId);

    // Phase 7: Count approvals by group and decision
    @Query("SELECT COUNT(a) FROM WorkflowApproval a WHERE a.approvalGroupId = :groupId AND a.decision = :decision")
    long countByApprovalGroupIdAndDecision(
        @Param("groupId") UUID groupId,
        @Param("decision") WorkflowApproval.ApprovalDecision decision
    );

    // ==================== Phase 4: Escalation Queries ====================

    /**
     * Find pending approvals that have passed their timeout time
     * Used by the escalation scheduler to identify approvals needing escalation
     */
    @Query("""
        SELECT a FROM WorkflowApproval a 
        WHERE a.decision = 'PENDING' 
        AND a.timeoutAt IS NOT NULL 
        AND a.timeoutAt < :now
        ORDER BY a.timeoutAt ASC
        """)
    List<WorkflowApproval> findPendingApprovalsPassedTimeout(@Param("now") OffsetDateTime now);

    /**
     * Find all escalated approvals that originated from a specific approval
     */
    @Query("""
        SELECT a FROM WorkflowApproval a 
        WHERE a.escalatedFromApprovalId = :originalApprovalId
        ORDER BY a.escalationLevel ASC
        """)
    List<WorkflowApproval> findEscalatedApprovalsFromOriginal(
        @Param("originalApprovalId") UUID originalApprovalId
    );

    /**
     * Count escalation levels for an approval chain
     * Useful for limiting maximum escalation depth
     */
    @Query("""
        SELECT MAX(a.escalationLevel) FROM WorkflowApproval a 
        WHERE a.approvalGroupId = :groupId
        """)
    Integer findMaxEscalationLevelInGroup(@Param("groupId") UUID groupId);

    /**
     * Find approvals by execution that are in escalatable states
     */
    @Query("""
        SELECT a FROM WorkflowApproval a 
        WHERE a.executionId = :executionId 
        AND a.decision IN ('PENDING', 'ESCALATED_TIMEOUT')
        """)
    List<WorkflowApproval> findEscalatableApprovalsByExecution(
        @Param("executionId") UUID executionId
    );

    // ==================== Approval History for Responsibilities Page ====================

    /**
     * Find all processed (non-pending) approvals for a user.
     * Includes approvals where user was the original approver or delegated approver.
     * Used for showing approval history in the Responsibilities page.
     */
    @Query("""
        SELECT a FROM WorkflowApproval a 
        WHERE (a.approverId = :userId OR a.delegatedTo = :userId) 
        AND a.tenantId = :tenantId 
        AND a.decision != 'PENDING'
        ORDER BY a.respondedAt DESC NULLS LAST, a.requestedAt DESC
        """)
    List<WorkflowApproval> findProcessedApprovalsForUser(
        @Param("userId") UUID userId, 
        @Param("tenantId") String tenantId
    );

    /**
     * Find processed approvals for a user filtered by specific decision.
     * Used for filtering history by APPROVED, REJECTED, TIMEOUT, etc.
     */
    @Query("""
        SELECT a FROM WorkflowApproval a 
        WHERE (a.approverId = :userId OR a.delegatedTo = :userId) 
        AND a.tenantId = :tenantId 
        AND a.decision = :decision
        ORDER BY a.respondedAt DESC NULLS LAST, a.requestedAt DESC
        """)
    List<WorkflowApproval> findProcessedApprovalsForUserByDecision(
        @Param("userId") UUID userId, 
        @Param("tenantId") String tenantId,
        @Param("decision") WorkflowApproval.ApprovalDecision decision
    );
}
