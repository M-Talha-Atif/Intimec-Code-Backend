package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.client.IdentityGrpcClient;
import com.aidentx.workflowsservice.client.IdentityRestClient;
import com.aidentx.workflowsservice.entity.*;
import com.aidentx.workflowsservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 8: Unit tests for NodeExecutor
 * Tests multi-approval, parallel execution, and failure handling
 */
@ExtendWith(MockitoExtension.class)
class NodeExecutorTest {

    @Mock
    private WorkflowApprovalService approvalService;
    
    @Mock
    private ParallelExecutionRepository parallelExecutionRepository;
    
    @Mock
    private ParallelBranchContextRepository parallelBranchContextRepository;
    
    @Mock
    private ApproverResolver approverResolver;
    
    @Mock
    private IdentityGrpcClient identityGrpcClient;
    
    @Mock
    private IdentityRestClient identityRestClient;
    
    @InjectMocks
    private NodeExecutor nodeExecutor;
    
    private WorkflowExecution execution;
    private Map<String, Object> context;
    
    @BeforeEach
    void setUp() {
        execution = WorkflowExecution.builder()
            .id(UUID.randomUUID())
            .workflowId(UUID.randomUUID())
            .status(WorkflowExecution.ExecutionStatus.RUNNING)
            .build();
        
        context = new HashMap<>();
        context.put("userId", "user-123");
    }
    
    // ============ Phase 7.1: Multi-Approval Tests ============
    
    @Test
    void testExecuteApprovalMulti_CreatesAllApprovalsWithGroupId() {
        // Given: Multi-approval node with 3 approvers
        Map<String, Object> node = createMultiApprovalNode(
            Arrays.asList("approver-1", "approver-2", "approver-3")
        );
        
        WorkflowApproval approval1 = createMockApproval();
        WorkflowApproval approval2 = createMockApproval();
        WorkflowApproval approval3 = createMockApproval();
        
        when(approvalService.createApprovalRequest(
            any(UUID.class), any(), any(UUID.class), any(), any(), any(), any(), any(UUID.class)
        )).thenReturn(approval1, approval2, approval3);
        
        // When: Execute multi-approval node
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: 3 approvals created with same group ID
        verify(approvalService, times(3)).createApprovalRequest(
            eq(execution.getId()),
            isNull(),
            any(UUID.class),
            eq(WorkflowApproval.ApproverType.SPECIFIC),
            eq(context),
            any(Map.class),
            any(Integer.class),
            any(UUID.class) // approval group ID
        );
        
        assertTrue((Boolean) output.get("pauseRequired"));
        assertEquals(3, output.get("requiredApprovals"));
        
        @SuppressWarnings("unchecked")
        List<String> approvalIds = (List<String>) output.get("approvalIds");
        assertEquals(3, approvalIds.size());
    }
    
    @Test
    void testExecuteApprovalMulti_ThrowsException_WhenNoApprovers() {
        // Given: Multi-approval node with no approvers
        Map<String, Object> node = createMultiApprovalNode(Collections.emptyList());
        
        // When & Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            nodeExecutor.execute(node, context, execution);
        });
    }
    
    // ============ Phase 6b: Parallel Execution Tests ============
    
    @Test
    void testExecuteParallelSplit_CreatesParallelExecution() {
        // Given: Parallel split node with 3 branches
        Map<String, Object> node = createParallelSplitNode("split-1", "join-1");
        
        when(parallelExecutionRepository.save(any(ParallelExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: Execute parallel split
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: Parallel execution created
        verify(parallelExecutionRepository).save(argThat(pe -> 
            pe.getExecutionId().equals(execution.getId()) &&
            pe.getSplitNodeId().equals("split-1") &&
            pe.getJoinNodeId().equals("join-1") &&
            pe.getTotalBranches() == 3
        ));
        
        assertTrue((Boolean) output.get("splitNode"));
        assertEquals(3, output.get("branches"));
    }
    
    @Test
    void testExecuteParallelJoin_WaitsForAllBranches() {
        // Given: 3 branches, only 2 completed
        Map<String, Object> node = createParallelJoinNode("join-1");
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(3, 2); // 2 of 3 done
        ParallelBranchContext branchContext = createBranchContext(0);
        branchContext.setIsComplete(true); // Mark as complete
        branchContext.setIsSuccessful(true); // Mark as successful
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(branchContext));
        
        when(parallelExecutionRepository.save(any(ParallelExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: Execute join (3rd branch arrives)
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: Should complete since this is the 3rd branch (2 + 1 = 3)
        assertNotNull(output.get("allBranchesComplete"));
        assertEquals(3, parallelExec.getTotalBranches());
    }
    
    @Test
    void testExecuteParallelJoin_MergesContexts_WhenAllComplete() {
        // Given: All 3 branches complete
        Map<String, Object> node = createParallelJoinNode("join-1");
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(3, 2); // Will become 3 after increment
        ParallelBranchContext currentBranch = createBranchContext(2);
        
        List<ParallelBranchContext> allBranches = Arrays.asList(
            createBranchContextWithData(0, Map.of("var1", "value1", "shared", "branch0")),
            createBranchContextWithData(1, Map.of("var2", "value2", "shared", "branch1")),
            createBranchContextWithData(2, Map.of("var3", "value3", "shared", "branch2"))
        );
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(currentBranch));
        
        when(parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
            any(UUID.class), anyString()
        )).thenReturn(allBranches);
        
        // When: Execute join (last branch arrives)
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: Should merge all contexts
        assertTrue((Boolean) output.get("allBranchesComplete"));
        assertEquals("value1", output.get("var1"));
        assertEquals("value2", output.get("var2"));
        assertEquals("value3", output.get("var3"));
        assertEquals("branch2", output.get("shared")); // Last wins in simple merge
    }
    
    // ============ Phase 7: Parallel Branch Failure Tests ============
    
    @Test
    void testExecuteParallelJoin_FAIL_FAST_ThrowsException_WhenAnyBranchFails() {
        // Given: 3 branches, 1 failed, FAIL_FAST policy
        Map<String, Object> node = createParallelJoinNodeWithPolicy("join-1", "FAIL_FAST");
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(3, 2);
        ParallelBranchContext currentBranch = createBranchContext(2);
        
        List<ParallelBranchContext> allBranches = Arrays.asList(
            createSuccessfulBranch(0, Map.of("var1", "value1")),
            createFailedBranch(1, "Database connection failed"),
            createSuccessfulBranch(2, Map.of("var3", "value3"))
        );
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(currentBranch));
        
        when(parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
            any(UUID.class), anyString()
        )).thenReturn(allBranches);
        
        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            nodeExecutor.execute(node, context, execution);
        });
        
        assertTrue(exception.getMessage().contains("Parallel branch execution failed"));
        assertTrue(exception.getMessage().contains("Database connection failed"));
    }
    
    @Test
    void testExecuteParallelJoin_FAIL_SAFE_ContinuesWithSuccessful_WhenSomeFail() {
        // Given: 3 branches, 1 failed, FAIL_SAFE policy
        Map<String, Object> node = createParallelJoinNodeWithPolicy("join-1", "FAIL_SAFE");
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(3, 2);
        ParallelBranchContext currentBranch = createBranchContext(2);
        
        List<ParallelBranchContext> allBranches = Arrays.asList(
            createSuccessfulBranch(0, Map.of("var1", "value1")),
            createFailedBranch(1, "Network timeout"),
            createSuccessfulBranch(2, Map.of("var3", "value3"))
        );
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(currentBranch));
        
        when(parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
            any(UUID.class), anyString()
        )).thenReturn(allBranches);
        
        // When: Execute join
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: Should merge only successful branches
        assertEquals("value1", output.get("var1"));
        assertEquals("value3", output.get("var3"));
        assertNull(output.get("var2")); // Failed branch data not included
        assertTrue((Boolean) output.get("allBranchesComplete"));
    }
    
    @Test
    void testExecuteParallelJoin_THRESHOLD_FailsWhenExceedsThreshold() {
        // Given: 5 branches, 2 failed (40%), threshold 30%
        Map<String, Object> node = createParallelJoinNodeWithPolicyAndThreshold(
            "join-1", "THRESHOLD", 0.3
        );
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(5, 4);
        ParallelBranchContext currentBranch = createBranchContext(4);
        
        List<ParallelBranchContext> allBranches = Arrays.asList(
            createSuccessfulBranch(0, Map.of("var1", "value1")),
            createFailedBranch(1, "Error 1"),
            createSuccessfulBranch(2, Map.of("var3", "value3")),
            createFailedBranch(3, "Error 2"),
            createSuccessfulBranch(4, Map.of("var5", "value5"))
        );
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(currentBranch));
        
        when(parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
            any(UUID.class), anyString()
        )).thenReturn(allBranches);
        
        // When & Then: Should throw exception (40% > 30%)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            nodeExecutor.execute(node, context, execution);
        });
        
        assertTrue(exception.getMessage().contains("Too many parallel branches failed"));
        assertTrue(exception.getMessage().contains("40%"));
    }
    
    @Test
    void testExecuteParallelJoin_THRESHOLD_ContinuesWhenBelowThreshold() {
        // Given: 5 branches, 1 failed (20%), threshold 30%
        Map<String, Object> node = createParallelJoinNodeWithPolicyAndThreshold(
            "join-1", "THRESHOLD", 0.3
        );
        context.put("branchContextId", UUID.randomUUID().toString());
        
        ParallelExecution parallelExec = createParallelExecution(5, 4);
        ParallelBranchContext currentBranch = createBranchContext(4);
        
        List<ParallelBranchContext> allBranches = Arrays.asList(
            createSuccessfulBranch(0, Map.of("var1", "value1")),
            createSuccessfulBranch(1, Map.of("var2", "value2")),
            createSuccessfulBranch(2, Map.of("var3", "value3")),
            createFailedBranch(3, "Minor error"),
            createSuccessfulBranch(4, Map.of("var5", "value5"))
        );
        
        when(parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
            any(UUID.class), anyString()
        )).thenReturn(Optional.of(parallelExec));
        
        when(parallelBranchContextRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(currentBranch));
        
        when(parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
            any(UUID.class), anyString()
        )).thenReturn(allBranches);
        
        // When: Execute join
        Map<String, Object> output = nodeExecutor.execute(node, context, execution);
        
        // Then: Should merge successful branches (20% < 30%)
        assertEquals("value1", output.get("var1"));
        assertEquals("value2", output.get("var2"));
        assertEquals("value5", output.get("var5"));
        assertTrue((Boolean) output.get("allBranchesComplete"));
    }
    
    // ============ Helper Methods ============
    
    private Map<String, Object> createMultiApprovalNode(List<String> approverIds) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", "approval-multi-1");
        node.put("type", "approval.multi");
        
        // Convert string IDs to valid UUIDs for testing
        List<String> validUuids = approverIds.stream()
            .map(id -> UUID.nameUUIDFromBytes(id.getBytes()).toString())
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("label", "Multi Approval");
        data.put("approvers", validUuids);
        data.put("timeoutMinutes", 1440);
        node.put("data", data);
        
        return node;
    }
    
    private Map<String, Object> createParallelSplitNode(String splitId, String joinId) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", splitId);
        node.put("type", "gateway.parallel-split");
        
        Map<String, Object> data = new HashMap<>();
        data.put("label", "Parallel Split");
        data.put("joinNode", joinId); // Required: join node ID
        data.put("branches", 3); // Required: number of branches
        node.put("data", data);
        
        return node;
    }
    
    private Map<String, Object> createParallelJoinNode(String joinId) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", joinId);
        node.put("type", "gateway.parallel-join");
        
        Map<String, Object> data = new HashMap<>();
        data.put("label", "Parallel Join");
        node.put("data", data);
        
        return node;
    }
    
    private Map<String, Object> createParallelJoinNodeWithPolicy(String joinId, String policy) {
        Map<String, Object> node = createParallelJoinNode(joinId);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        data.put("failurePolicy", policy);
        return node;
    }
    
    private Map<String, Object> createParallelJoinNodeWithPolicyAndThreshold(
        String joinId, String policy, double threshold
    ) {
        Map<String, Object> node = createParallelJoinNodeWithPolicy(joinId, policy);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        data.put("failureThreshold", threshold);
        return node;
    }
    
    private WorkflowApproval createMockApproval() {
        return WorkflowApproval.builder()
            .id(UUID.randomUUID())
            .executionId(execution.getId())
            .decision(WorkflowApproval.ApprovalDecision.PENDING)
            .build();
    }
    
    private ParallelExecution createParallelExecution(int total, int completed) {
        ParallelExecution pe = new ParallelExecution();
        pe.setId(UUID.randomUUID());
        pe.setExecutionId(execution.getId());
        pe.setSplitNodeId("split-1");
        pe.setJoinNodeId("join-1");
        pe.setTotalBranches(total);
        pe.setCompletedBranches(completed);
        return pe;
    }
    
    private ParallelBranchContext createBranchContext(int index) {
        return ParallelBranchContext.builder()
            .id(UUID.randomUUID())
            .executionId(execution.getId())
            .branchIndex(index)
            .isComplete(false)
            .build();
    }
    
    private ParallelBranchContext createBranchContextWithData(int index, Map<String, Object> data) {
        return ParallelBranchContext.builder()
            .id(UUID.randomUUID())
            .executionId(execution.getId())
            .branchIndex(index)
            .branchContext(data)
            .isComplete(true)
            .isSuccessful(true)
            .build();
    }
    
    private ParallelBranchContext createSuccessfulBranch(int index, Map<String, Object> data) {
        return ParallelBranchContext.builder()
            .id(UUID.randomUUID())
            .executionId(execution.getId())
            .branchIndex(index)
            .branchContext(data)
            .isComplete(true)
            .isSuccessful(true)
            .build();
    }
    
    private ParallelBranchContext createFailedBranch(int index, String errorMessage) {
        return ParallelBranchContext.builder()
            .id(UUID.randomUUID())
            .executionId(execution.getId())
            .branchIndex(index)
            .isComplete(true)
            .isSuccessful(false)
            .errorMessage(errorMessage)
            .build();
    }
}
