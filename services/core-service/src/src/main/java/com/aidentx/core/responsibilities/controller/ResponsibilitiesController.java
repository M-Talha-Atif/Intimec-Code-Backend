package com.aidentx.core.responsibilities.controller;

import com.aidentx.core.responsibilities.client.ApplicationPermissionsServiceClient;
import com.aidentx.core.responsibilities.client.WorkflowsServiceClient;
import com.aidentx.core.responsibilities.dto.*;
import com.aidentx.core.responsibilities.mapper.TaskMapper;
import com.aidentx.core.responsibilities.service.TeamMemberService;
import com.aidentx.core.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Responsibilities page.
 * Aggregates governance tasks from multiple services (workflows, certifications, etc.).
 * 
 * This controller serves as a facade/aggregation layer, querying workflows-service
 * and other IAM services to provide a unified view of a user's governance responsibilities.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/responsibilities")
public class ResponsibilitiesController {
    
    private final WorkflowsServiceClient workflowsClient;
    private final ApplicationPermissionsServiceClient applicationPermissionsClient;
    private final TeamMemberService teamMemberService;
    private final TaskMapper taskMapper;
    private final TenantContextService tenantContextService;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    
    public ResponsibilitiesController(
            WorkflowsServiceClient workflowsClient,
            ApplicationPermissionsServiceClient applicationPermissionsClient,
            TeamMemberService teamMemberService,
            TaskMapper taskMapper,
            TenantContextService tenantContextService,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.workflowsClient = workflowsClient;
        this.applicationPermissionsClient = applicationPermissionsClient;
        this.teamMemberService = teamMemberService;
        this.taskMapper = taskMapper;
        this.tenantContextService = tenantContextService;
        this.logger = loggerFactory.getLogger(ResponsibilitiesController.class, "ResponsibilitiesController");
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get all tasks for the current user.
     * Currently fetches workflow approvals; will expand to certifications, delegations, etc.
     * 
     * @param status Optional filter: pending, processed, all
     * @param decision Optional filter for processed: approved, rejected, timeout (only applies when status=processed)
     * @param type Optional filter: workflow, certification, delegation
     * @return List of tasks
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskDTO>> getMyTasks(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        
        // Extract userId and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Fetching tasks for user")
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId)
            .detail("status", status)
            .detail("decision", decision != null ? decision : "all")
            .detail("type", type != null ? type : "all")
            .log();
        
        try {
            // Extract JWT token for service-to-service authentication
            String authToken = request.getHeader("Authorization");
            
            List<WorkflowApprovalDTO> approvals;
            
            // Fetch based on status filter
            if ("processed".equalsIgnoreCase(status)) {
                // Get historical/processed approvals
                approvals = workflowsClient.getProcessedApprovals(userId, tenantId, authToken, decision);
            } else if ("pending".equalsIgnoreCase(status)) {
                // Get only pending approvals
                approvals = workflowsClient.getPendingApprovals(userId, tenantId, authToken);
            } else {
                // Get all approvals (pending + processed)
                List<WorkflowApprovalDTO> pending = workflowsClient.getPendingApprovals(userId, tenantId, authToken);
                List<WorkflowApprovalDTO> processed = workflowsClient.getProcessedApprovals(userId, tenantId, authToken, null);
                approvals = new java.util.ArrayList<>(pending);
                approvals.addAll(processed);
            }
            
            // Map to Task DTOs
            List<TaskDTO> tasks = approvals.stream()
                .map(taskMapper::mapToTaskDTO)
                .collect(Collectors.toList());
            
            // Apply type filter if specified
            if (type != null && !"all".equalsIgnoreCase(type)) {
                tasks = tasks.stream()
                    .filter(task -> type.equalsIgnoreCase(task.getType()))
                    .collect(Collectors.toList());
            }
            
            // TODO: Add certifications from certification-service
            // TODO: Add delegations from delegation-service
            
            logger.info()
                .message("Successfully retrieved tasks")
                .detail("count", tasks.size())
                .log();
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve tasks")
                .detail("userId", userId.toString())
                .log();
            throw e;
        }
    }
    
    /**
     * Get detailed information for a specific task.
     * 
     * @param approvalId Task/Approval ID
     * @return Detailed task information with all IAM context
     */
    @GetMapping("/my-tasks/{approvalId}")
    public ResponseEntity<TaskDetailsDTO> getTaskDetails(
            @PathVariable UUID approvalId) {
        
        // Extract userId and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Fetching task details")
            .detail("approvalId", approvalId.toString())
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId)
            .log();
        
        try {
            // Fetch approval details from workflows-service
            WorkflowApprovalDTO approval = workflowsClient.getApprovalDetails(approvalId, userId, tenantId);
            
            // Map to detailed Task DTO
            TaskDetailsDTO details = taskMapper.mapToTaskDetailsDTO(approval);
            
            // Debug: Log what's in the DTO before returning
            // Debug: Log what's in the DTO before returning
            System.out.println("=== ResponsibilitiesController - Returning DTO ===");
            System.out.println("eventType: " + details.getEventType());
            System.out.println("singlePermission: " + details.getSinglePermission());
            System.out.println("permissionList: " + details.getPermissionList());
            System.out.println("targetUsersList: " + details.getTargetUsersList());
            System.out.println("approvalContext: " + (details.getApprovalContext() != null ? details.getApprovalContext().size() + " entries" : "null"));
            
            // Debug: Log actual JSON that will be sent
            try {
                String json = objectMapper.writeValueAsString(details);
                System.out.println("=== ACTUAL JSON RESPONSE (first 2000 chars) ===");
                System.out.println(json.substring(0, Math.min(json.length(), 2000)));
                System.out.println("=== END JSON ===");
            } catch (Exception e) {
                System.out.println("Error serializing to JSON: " + e.getMessage());
            }
            
            logger.info()
                .message("Successfully retrieved task details")
                .detail("approvalId", approvalId.toString())
                .log();
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve task details")
                .detail("approvalId", approvalId.toString())
                .log();
            throw e;
        }
    }
    
    /**
     * Approve a task.
     * Delegates to the appropriate service (workflows-service for approvals).
     * 
     * @param approvalId Task/Approval ID
     * @param body Request body containing optional comment
     * @return Success response
     */
    @PostMapping("/my-tasks/{approvalId}/approve")
    public ResponseEntity<Void> approveTask(
            @PathVariable UUID approvalId,
            @RequestBody(required = false) Map<String, String> body) {
        
        // Extract userId, username, and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String username = tenantContextService.getCurrentUsername();
        String tenantId = tenantContextService.getCurrentTenantId();
        
        String comment = body != null ? body.get("comment") : null;
        
        logger.info()
            .message("Approving task")
            .detail("approvalId", approvalId.toString())
            .detail("userId", userId.toString())
            .detail("username", username)
            .detail("tenantId", tenantId)
            .detail("hasComment", comment != null && !comment.trim().isEmpty())
            .log();
        
        try {
            // Delegate to workflows-service
            workflowsClient.approveApproval(approvalId, userId, tenantId, username, comment);
            
            logger.info()
                .message("Successfully approved task")
                .detail("approvalId", approvalId.toString())
                .log();
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to approve task")
                .detail("approvalId", approvalId.toString())
                .log();
            throw e;
        }
    }
    
    /**
     * Reject a task.
     * Delegates to the appropriate service (workflows-service for approvals).
     * 
     * @param approvalId Task/Approval ID
     * @param body Request body containing required comment
     * @return Success response
     */
    @PostMapping("/my-tasks/{approvalId}/reject")
    public ResponseEntity<Void> rejectTask(
            @PathVariable UUID approvalId,
            @RequestBody Map<String, String> body) {
        
        // Extract userId, username, and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String username = tenantContextService.getCurrentUsername();
        String tenantId = tenantContextService.getCurrentTenantId();
        
        String comment = body != null ? body.get("comment") : null;
        
        if (comment == null || comment.trim().isEmpty()) {
            logger.warn()
                .message("Rejection attempted without comment")
                .detail("approvalId", approvalId.toString())
                .log();
            return ResponseEntity.badRequest().build();
        }
        
        logger.info()
            .message("Rejecting task")
            .detail("approvalId", approvalId.toString())
            .detail("userId", userId.toString())
            .detail("username", username)
            .detail("tenantId", tenantId)
            .log();
        
        try {
            // Delegate to workflows-service
            workflowsClient.rejectApproval(approvalId, userId, tenantId, username, comment);
            
            logger.info()
                .message("Successfully rejected task")
                .detail("approvalId", approvalId.toString())
                .log();
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to reject task")
                .detail("approvalId", approvalId.toString())
                .log();
            throw e;
        }
    }
    
    // TODO: Add endpoints for other responsibilities:
    // - GET /my-business-roles
    // - GET /my-team
    // - GET /my-sod-policies
    // - GET /my-applications
    
    /**
     * Get team members (direct and indirect reports) for the current user.
     * Returns employees where the current user is their manager, along with
     * identity information, access details, and risk metrics.
     * 
     * Integrates with:
     * - auth-service: To get current user's identityId and subordinate user details
     * - identity-service: To query employees where manager_id = current user's identityId
     * - application-permissions-service: To fetch roles and permissions (future enhancement)
     * 
     * @return Team members with comprehensive details
     */
    @GetMapping("/my-team")
    public ResponseEntity<TeamResponse> getMyTeam(HttpServletRequest request) {
        
        // Extract userId, identityId, and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String identityIdStr = tenantContextService.getCurrentIdentityId();
        String tenantId = tenantContextService.getCurrentTenantId();
        
        if (identityIdStr == null || identityIdStr.trim().isEmpty()) {
            logger.warn()
                .message("Identity ID not found in JWT token for user")
                .detail("userId", userId.toString())
                .log();
            return ResponseEntity.ok(TeamResponse.builder()
                .totalCount(0)
                .directReportCount(0)
                .indirectReportCount(0)
                .activeCount(0)
                .inactiveCount(0)
                .averageRiskScore(0.0)
                .teamMembers(new java.util.ArrayList<>())
                .build());
        }
        
        UUID identityId = UUID.fromString(identityIdStr);
        
        logger.info()
            .message("Fetching team members for manager")
            .detail("userId", userId.toString())
            .detail("identityId", identityId.toString())
            .detail("tenantId", tenantId)
            .log();
        
        try {
            // Extract JWT token for service-to-service authentication
            String authToken = request.getHeader("Authorization");
            
            // Delegate to service layer for full integration
            TeamResponse response = teamMemberService.getTeamMembers(identityId, tenantId, authToken);
            
            logger.info()
                .message("Successfully retrieved team members")
                .detail("totalCount", String.valueOf(response.getTotalCount()))
                .detail("directReports", String.valueOf(response.getDirectReportCount()))
                .detail("indirectReports", String.valueOf(response.getIndirectReportCount()))
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve team members")
                .detail("userId", userId.toString())
                .log();
            
            // Return empty response on error rather than throwing exception
            TeamResponse errorResponse = TeamResponse.builder()
                .teamMembers(List.of())
                .totalCount(0)
                .directReportCount(0)
                .indirectReportCount(0)
                .activeCount(0)
                .inactiveCount(0)
                .averageRiskScore(0.0)
                .success(false)
                .message("Failed to retrieve team members: " + e.getMessage())
                .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get internal roles owned by the current user.
     * Fetches roles where the user is a role owner in application-permissions-service.
     * 
     * @return Owned roles with permissions and metadata
     */
    @GetMapping("/my-internal-roles")
    public ResponseEntity<Object> getMyInternalRoles(HttpServletRequest request) {
        
        // Extract userId and tenantId from JWT token via TenantContextService
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Fetching owned roles for user")
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId)
            .log();
        
        try {
            // Extract JWT token for service-to-service authentication
            String authToken = request.getHeader("Authorization");
            
            // Fetch owned roles from application-permissions-service
            Object ownedRoles = applicationPermissionsClient.getOwnedRoles(userId, tenantId, authToken);
            
            logger.info()
                .message("Successfully retrieved owned roles")
                .log();
            
            return ResponseEntity.ok(ownedRoles);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve owned roles")
                .detail("userId", userId.toString())
                .log();
            throw e;
        }
    }
}
