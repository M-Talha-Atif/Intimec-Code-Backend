package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.async.AsyncExecutionDispatcher;
import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.client.UserGrpcClient;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowApprovalRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 8: Unit tests for WorkflowApprovalService
 * Tests Phase 7.1 (multi-approval) and Phase 7.2 (rejection handling)
 */
@ExtendWith(MockitoExtension.class)
class WorkflowApprovalServiceTest {

    @Mock
    private WorkflowApprovalRepository approvalRepository;
    
    @Mock
    private WorkflowExecutionRepository executionRepository;
    
    @Mock
    private WorkflowRepository workflowRepository;
    
    @Mock
    private UserGrpcClient userClient;
    
    @Mock
    private NotificationKafkaClient notificationClient;
    
    @Mock
    private AsyncExecutionDispatcher asyncExecutionDispatcher;
    
    @InjectMocks
    private WorkflowApprovalService approvalService;
    
    private UUID executionId;
    private UUID approvalGroupId;
    
    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        approvalGroupId = UUID.randomUUID();
    }
    
    // ============ Phase 7.1: Multi-Approval Tests ============
    
    @Test
    void testApprove_MultiApproval_WaitsForAll_WhenOnlyOneApproves() {
        // Given: 3 approvals in group, only 1 approved so far
        WorkflowApproval approval1 = createApproval(approvalGroupId, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowApproval approval2 = createApproval(approvalGroupId, "user-2", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowApproval approval3 = createApproval(approvalGroupId, "user-3", WorkflowApproval.ApprovalDecision.PENDING);
        
        List<WorkflowApproval> allApprovals = Arrays.asList(approval1, approval2, approval3);
        
        when(approvalRepository.findById(approval1.getId())).thenReturn(Optional.of(approval1));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        when(approvalRepository.findByApprovalGroupId(approvalGroupId)).thenReturn(allApprovals);
        
        // When: First user approves
        WorkflowApproval result = approvalService.approve(approval1.getId(), "Looks good", approval1.getApproverId());
        
        // Then: Workflow should NOT resume (still waiting for others)
        assertEquals(WorkflowApproval.ApprovalDecision.APPROVED, result.getDecision());
        verify(asyncExecutionDispatcher, never()).resume(any());
    }
    
    @Test
    void testApprove_MultiApproval_WaitsForAll_WhenTwoOfThreeApprove() {
        // Given: 3 approvals in group, 2 approved
        WorkflowApproval approval1 = createApproval(approvalGroupId, "user-1", WorkflowApproval.ApprovalDecision.APPROVED);
        WorkflowApproval approval2 = createApproval(approvalGroupId, "user-2", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowApproval approval3 = createApproval(approvalGroupId, "user-3", WorkflowApproval.ApprovalDecision.APPROVED);
        
        List<WorkflowApproval> allApprovals = Arrays.asList(approval1, approval2, approval3);
        
        when(approvalRepository.findById(approval2.getId())).thenReturn(Optional.of(approval2));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        when(approvalRepository.findByApprovalGroupId(approvalGroupId)).thenReturn(allApprovals);
        
        // When: Second user approves (now 2/3 are approved, but not ALL)
        WorkflowApproval result = approvalService.approve(approval2.getId(), "Approved", approval2.getApproverId());
        
        // Then: Workflow should resume since all approvals are now approved (after this call, approval2 becomes APPROVED)
        assertEquals(WorkflowApproval.ApprovalDecision.APPROVED, result.getDecision());
        verify(asyncExecutionDispatcher).resume(any());
    }
    
    @Test
    void testApprove_MultiApproval_ResumesWorkflow_WhenAllApprove() {
        // Given: 3 approvals in group, 2 already approved, 1 pending
        WorkflowApproval approval1 = createApproval(approvalGroupId, "user-1", WorkflowApproval.ApprovalDecision.APPROVED);
        WorkflowApproval approval2 = createApproval(approvalGroupId, "user-2", WorkflowApproval.ApprovalDecision.APPROVED);
        WorkflowApproval approval3 = createApproval(approvalGroupId, "user-3", WorkflowApproval.ApprovalDecision.PENDING);
        
        // After approval3 is approved, all will be approved
        WorkflowApproval approval3Approved = createApproval(approvalGroupId, "user-3", WorkflowApproval.ApprovalDecision.APPROVED);
        List<WorkflowApproval> allApprovals = Arrays.asList(approval1, approval2, approval3Approved);
        
        when(approvalRepository.findById(approval3.getId())).thenReturn(Optional.of(approval3));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenReturn(approval3Approved);
        when(approvalRepository.findByApprovalGroupId(approvalGroupId)).thenReturn(allApprovals);
        
        // When: Third user approves (last one)
        WorkflowApproval result = approvalService.approve(approval3.getId(), "Final approval", approval3.getApproverId());
        
        // Then: Workflow SHOULD resume now
        assertEquals(WorkflowApproval.ApprovalDecision.APPROVED, result.getDecision());
        verify(asyncExecutionDispatcher, times(1)).resume(executionId);
    }
    
    @Test
    void testApprove_SingleApproval_ResumesImmediately() {
        // Given: Single approval (no group ID)
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        
        // When: User approves
        WorkflowApproval result = approvalService.approve(approval.getId(), "Approved", approval.getApproverId());
        
        // Then: Workflow should resume immediately
        assertEquals(WorkflowApproval.ApprovalDecision.APPROVED, result.getDecision());
        verify(asyncExecutionDispatcher, times(1)).resume(executionId);
    }
    
    @Test
    void testApprove_ThrowsException_WhenApprovalNotFound() {
        // Given: Non-existent approval ID
        UUID nonExistentId = UUID.randomUUID();
        when(approvalRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // When & Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            approvalService.approve(nonExistentId, "Approved", UUID.randomUUID());
        });
    }
    
    @Test
    void testApprove_ThrowsException_WhenAlreadyDecided() {
        // Given: Approval already approved
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.APPROVED);
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        
        // When & Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            approvalService.approve(approval.getId(), "Trying to approve again", approval.getApproverId());
        });
    }
    
    @Test
    void testApprove_ThrowsException_WhenUnauthorizedUser() {
        // Given: Approval for user-1
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        
        // When & Then: Different user tries to approve (should fail)
        UUID unauthorizedUser = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            approvalService.approve(approval.getId(), "Unauthorized", unauthorizedUser);
        });
    }
    
    // ============ Phase 7.2: Rejection Handling Tests ============
    
    @Test
    void testReject_FailsWorkflow_WithProperErrorMessage() {
        // Given: Pending approval
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowExecution execution = createExecution();
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(i -> i.getArgument(0));
        
        // When: User rejects
        WorkflowApproval result = approvalService.reject(
            approval.getId(), 
            "Security concerns", 
            approval.getApproverId()
        );
        
        // Then: Approval marked as rejected
        assertEquals(WorkflowApproval.ApprovalDecision.REJECTED, result.getDecision());
        
        // And: Workflow execution failed (errorMessage contains rejected and the approver ID as UUID string)
        verify(executionRepository, atLeastOnce()).save(argThat(exec -> 
            exec.getStatus() == WorkflowExecution.ExecutionStatus.FAILED &&
            exec.getErrorMessage() != null &&
            exec.getErrorMessage().contains("rejected") &&
            exec.getCompletedAt() != null
        ));
    }
    
    @Test
    void testReject_CalculatesDuration_WhenWorkflowFails() {
        // Given: Running execution with start time
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowExecution execution = createExecution();
        execution.setStartedAt(OffsetDateTime.now().minusMinutes(5)); // Started 5 min ago
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(i -> i.getArgument(0));
        
        // When: User rejects
        approvalService.reject(approval.getId(), "Not authorized", approval.getApproverId());
        
        // Then: Duration should be calculated (roughly 5 minutes = 300,000 ms)
        verify(executionRepository).save(argThat(exec -> 
            exec.getDurationMs() != null &&
            exec.getDurationMs() > 290000 && // ~5 min
            exec.getDurationMs() < 310000
        ));
    }
    
    @Test
    void testReject_MultiApproval_FailsImmediately_OnAnyRejection() {
        // Given: Multi-approval with 3 approvers, one rejects
        WorkflowApproval approval = createApproval(approvalGroupId, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        WorkflowExecution execution = createExecution();
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(i -> i.getArgument(0));
        
        // When: First user rejects (don't wait for others)
        approvalService.reject(approval.getId(), "I reject", approval.getApproverId());
        
        // Then: Workflow should fail immediately
        verify(executionRepository).save(argThat(exec -> 
            exec.getStatus() == WorkflowExecution.ExecutionStatus.FAILED
        ));
    }
    
    @Test
    void testReject_ThrowsException_WhenAlreadyDecided() {
        // Given: Approval already rejected
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.REJECTED);
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        
        // When & Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            approvalService.reject(approval.getId(), "Trying to reject again", approval.getApproverId());
        });
    }
    
    // ============ Delegation Tests ============
    
    @Test
    void testDelegate_UpdatesApproval_AndCreatesDelegationRecord() {
        // Given: Pending approval
        WorkflowApproval approval = createApproval(null, "user-1", WorkflowApproval.ApprovalDecision.PENDING);
        UUID delegateToId = UUID.nameUUIDFromBytes("user-2".getBytes());
        
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(i -> i.getArgument(0));
        
        // When: User delegates to another user
        WorkflowApproval result = approvalService.delegate(
            approval.getId(), 
            delegateToId, 
            approval.getApproverId()
        );
        
        // Then: Approval delegated (delegatedTo is set, decision stays PENDING until new approver acts)
        assertEquals(delegateToId, result.getDelegatedTo());
        assertEquals(WorkflowApproval.ApprovalDecision.PENDING, result.getDecision());
        assertNotNull(result.getDelegationChain());
    }
    
    // ============ Helper Methods ============
    
    private WorkflowApproval createApproval(
        UUID groupId, 
        String approverId, 
        WorkflowApproval.ApprovalDecision decision
    ) {
        // Generate valid UUID from string seed for consistent testing
        UUID approverUuid = UUID.nameUUIDFromBytes(approverId.getBytes());
        
        return WorkflowApproval.builder()
            .id(UUID.randomUUID())
            .executionId(executionId)
            .approverId(approverUuid)
            .approvalGroupId(groupId)
            .decision(decision)
            .build();
    }
    
    private WorkflowExecution createExecution() {
        return WorkflowExecution.builder()
            .id(executionId)
            .workflowId(UUID.randomUUID())
            .status(WorkflowExecution.ExecutionStatus.PAUSED)
            .startedAt(OffsetDateTime.now().minusMinutes(1))
            .build();
    }
}
