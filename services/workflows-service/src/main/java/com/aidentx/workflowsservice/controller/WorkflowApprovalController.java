package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.dto.request.ApproveRejectRequest;
import com.aidentx.workflowsservice.dto.request.CreateApprovalRequest;
import com.aidentx.workflowsservice.dto.request.DelegateApprovalRequest;
import com.aidentx.workflowsservice.dto.response.WorkflowApprovalDTO;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.service.WorkflowApprovalService;
import com.aidentx.workflowsservice.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Workflow Approvals
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Workflow Approvals", description = "Manage workflow approval requests")
public class WorkflowApprovalController {

    private final IUnifiedLogger logger;
    private final WorkflowApprovalService approvalService;
    private final JwtUtils jwtUtils;

    public WorkflowApprovalController(WorkflowApprovalService approvalService, JwtUtils jwtUtils, UnifiedLoggerFactory loggerFactory) {
        this.approvalService = approvalService;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger(WorkflowApprovalController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    /**
     * Create a manual approval request (for testing/admin use)
     */
    @PostMapping
    @Operation(summary = "Create a manual approval request")
    public ResponseEntity<WorkflowApprovalDTO> createApproval(@Valid @RequestBody CreateApprovalRequest request) {
        logger.info()
            .message(LOG_CREATING_APPROVAL_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowExecution")
            .detail("executionId", request.getExecutionId().toString())
            .log();

        WorkflowApproval approval = approvalService.createApprovalRequest(
                request.getExecutionId(),
                request.getNodeExecutionId(),
                request.getApproverId(),
                request.getApproverType(),
                request.getApprovalContext(),
                request.getEvidenceBundle(),
                request.getTimeoutMinutes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowApprovalDTO.from(approval));
    }

    /**
     * Get my pending approvals (including delegated)
     */
    @GetMapping("/my-pending")
    @Operation(summary = "Get my pending approvals")
    public ResponseEntity<List<WorkflowApprovalDTO>> getMyPendingApprovals(
            @RequestParam UUID userId,
            HttpServletRequest request
    ) {
        String tenantId = jwtUtils.extractTenantId(request);
        
        logger.info()
            .message(LOG_GETTING_PENDING_APPROVALS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId)
            .log();

        List<WorkflowApproval> approvals = approvalService.getPendingApprovalsForUser(userId, tenantId);
        List<WorkflowApprovalDTO> response = approvals.stream()
                .map(WorkflowApprovalDTO::from)
                .toList();

        logger.info()
            .message("Returning pending approvals")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("userId", userId.toString())
            .detail("count", response.size())
            .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Get my processed (completed) approvals - approval history.
     * Returns all non-pending approvals: APPROVED, REJECTED, TIMEOUT, etc.
     */
    @GetMapping("/my-history")
    @Operation(summary = "Get my approval history (processed approvals)")
    public ResponseEntity<List<WorkflowApprovalDTO>> getMyApprovalHistory(
            @RequestParam UUID userId,
            @RequestParam(required = false) String decision,
            HttpServletRequest request
    ) {
        String tenantId = jwtUtils.extractTenantId(request);
        
        logger.info()
            .message("Getting approval history for user")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId)
            .detail("decisionFilter", decision != null ? decision : "all")
            .log();

        List<WorkflowApproval> approvals = approvalService.getProcessedApprovalsForUser(userId, tenantId, decision);
        List<WorkflowApprovalDTO> response = approvals.stream()
                .map(WorkflowApprovalDTO::from)
                .toList();

        logger.info()
            .message("Returning approval history")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("userId", userId.toString())
            .detail("count", response.size())
            .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all approvals for an execution
     */
    @GetMapping("/executions/{executionId}")
    @Operation(summary = "Get all approvals for an execution")
    public ResponseEntity<List<WorkflowApprovalDTO>> getApprovalsForExecution(
            @PathVariable UUID executionId
    ) {
        logger.info()
            .message(LOG_GETTING_APPROVALS_FOR_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .log();

        List<WorkflowApproval> approvals = approvalService.getApprovalsForExecution(executionId);
        List<WorkflowApprovalDTO> response = approvals.stream()
                .map(WorkflowApprovalDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific approval by ID
     */
    @GetMapping("/{approvalId}")
    @Operation(summary = "Get an approval by ID")
    public ResponseEntity<WorkflowApprovalDTO> getApproval(@PathVariable UUID approvalId) {
        logger.info()
            .message(LOG_GETTING_APPROVAL)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowApproval")
            .objectId(approvalId.toString())
            .log();

        WorkflowApproval approval = approvalService.getApproval(approvalId);
        return ResponseEntity.ok(WorkflowApprovalDTO.from(approval));
    }

    /**
     * Approve an approval request
     */
    @PostMapping("/{approvalId}/approve")
    @Operation(summary = "Approve an approval request")
    public ResponseEntity<WorkflowApprovalDTO> approve(
            @PathVariable UUID approvalId,
            @Valid @RequestBody ApproveRejectRequest request,
            @RequestHeader("X-User-ID") UUID approverId,
            HttpServletRequest httpRequest
    ) {
        String tenantId = jwtUtils.extractTenantId(httpRequest);
        
        // Validate approval belongs to user's tenant (allow NULL for legacy data)
        WorkflowApproval existingApproval = approvalService.getApproval(approvalId);
        if (existingApproval.getTenantId() != null && !existingApproval.getTenantId().equals(tenantId)) {
            logger.warn()
                .message("Tenant mismatch: User attempted to approve approval from different tenant")
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .detail("approvalId", approvalId.toString())
                .detail("userTenantId", tenantId)
                .detail("approvalTenantId", existingApproval.getTenantId())
                .log();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        logger.info()
            .message(LOG_APPROVING_APPROVAL)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowApproval")
            .objectId(approvalId.toString())
            .detail("approverId", approverId.toString())
            .detail("tenantId", tenantId)
            .log();

        WorkflowApproval approval = approvalService.approve(
                approvalId,
                request.getComments(),
                approverId
        );

        return ResponseEntity.ok(WorkflowApprovalDTO.from(approval));
    }

    /**
     * Reject an approval request
     */
    @PostMapping("/{approvalId}/reject")
    @Operation(summary = "Reject an approval request")
    public ResponseEntity<WorkflowApprovalDTO> reject(
            @PathVariable UUID approvalId,
            @Valid @RequestBody ApproveRejectRequest request,
            @RequestHeader("X-User-ID") UUID approverId,
            HttpServletRequest httpRequest
    ) {
        String tenantId = jwtUtils.extractTenantId(httpRequest);
        
        // Validate approval belongs to user's tenant (allow NULL for legacy data)
        WorkflowApproval existingApproval = approvalService.getApproval(approvalId);
        if (existingApproval.getTenantId() != null && !existingApproval.getTenantId().equals(tenantId)) {
            logger.warn()
                .message("Tenant mismatch: User attempted to reject approval from different tenant")
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .detail("approvalId", approvalId.toString())
                .detail("userTenantId", tenantId)
                .detail("approvalTenantId", existingApproval.getTenantId())
                .log();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        logger.info()
            .message(LOG_REJECTING_APPROVAL)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowApproval")
            .objectId(approvalId.toString())
            .detail("approverId", approverId.toString())
            .detail("tenantId", tenantId)
            .log();

        WorkflowApproval approval = approvalService.reject(
                approvalId,
                request.getComments(),
                approverId
        );

        return ResponseEntity.ok(WorkflowApprovalDTO.from(approval));
    }

    /**
     * Delegate an approval to another user
     */
    @PostMapping("/{approvalId}/delegate")
    @Operation(summary = "Delegate an approval to another user")
    public ResponseEntity<WorkflowApprovalDTO> delegate(
            @PathVariable UUID approvalId,
            @Valid @RequestBody DelegateApprovalRequest request
    ) {
        logger.info()
            .message(LOG_DELEGATING_APPROVAL)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowApproval")
            .objectId(approvalId.toString())
            .detail("fromApproverId", request.getCurrentApproverId().toString())
            .detail("toApproverId", request.getDelegateToUserId().toString())
            .log();

        WorkflowApproval approval = approvalService.delegate(
                approvalId,
                request.getDelegateToUserId(),
                request.getCurrentApproverId()
        );

        return ResponseEntity.ok(WorkflowApprovalDTO.from(approval));
    }

    /**
     * Get approval statistics for an execution's node
     */
    @GetMapping("/executions/{executionId}/nodes/{nodeExecutionId}/stats")
    @Operation(summary = "Get approval statistics for a node execution")
    public ResponseEntity<Map<String, Long>> getApprovalStats(
            @PathVariable UUID executionId,
            @PathVariable UUID nodeExecutionId
    ) {
        logger.info()
            .message(LOG_GETTING_APPROVAL_STATS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("executionId", executionId.toString())
            .detail("nodeExecutionId", nodeExecutionId.toString())
            .log();

        Map<String, Long> stats = approvalService.getApprovalStats(executionId, nodeExecutionId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get pending approval details by notification ID (for frontend action buttons)
     * This endpoint returns the full approval context needed to display action buttons
     */
    @GetMapping("/by-approver/{approverId}/pending")
    @Operation(summary = "Get pending approval details for an approver")
    public ResponseEntity<List<WorkflowApprovalDTO>> getPendingApprovalsByApproverId(
            @PathVariable UUID approverId,
            HttpServletRequest request
    ) {
        String tenantId = jwtUtils.extractTenantId(request);
        
        logger.info()
            .message(LOG_GETTING_PENDING_APPROVALS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("approverId", approverId.toString())
            .detail("tenantId", tenantId)
            .log();

        List<WorkflowApproval> approvals = approvalService.getPendingApprovalsForUser(approverId, tenantId);
        List<WorkflowApprovalDTO> response = approvals.stream()
                .map(WorkflowApprovalDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Process timed-out approvals (admin endpoint)
     */
    @PostMapping("/process-timeouts")
    @Operation(summary = "Process timed-out approvals")
    public ResponseEntity<Void> processTimeouts() {
        logger.info()
            .message(LOG_PROCESSING_APPROVAL_TIMEOUTS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .log();

        approvalService.processTimeouts();
        return ResponseEntity.ok().build();
    }
}
