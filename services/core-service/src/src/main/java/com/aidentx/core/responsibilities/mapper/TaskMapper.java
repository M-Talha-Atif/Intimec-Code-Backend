package com.aidentx.core.responsibilities.mapper;

import com.aidentx.core.responsibilities.dto.TaskDTO;
import com.aidentx.core.responsibilities.dto.TaskDetailsDTO;
import com.aidentx.core.responsibilities.dto.WorkflowApprovalDTO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;

/**
 * Mapper for converting WorkflowApproval DTOs to Task DTOs.
 * Handles extraction of IAM context fields and priority calculation.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Component
public class TaskMapper {
    
    /**
     * Maps WorkflowApprovalDTO to TaskDTO for list views.
     * 
     * @param approval Workflow approval from workflows-service
     * @return Task DTO for frontend
     */
    public TaskDTO mapToTaskDTO(WorkflowApprovalDTO approval) {
        Map<String, Object> context = approval.getApprovalContext();
        
        // Store context and approval in thread-local for helper methods
        contextThreadLocal.set(context);
        approvalThreadLocal.set(approval);
        try {
            return buildTaskDTO(approval, context);
        } finally {
            contextThreadLocal.remove();
            approvalThreadLocal.remove();
        }
    }
    
    private TaskDTO buildTaskDTO(WorkflowApprovalDTO approval, Map<String, Object> context) {
        return TaskDTO.builder()
            .id(approval.getId().toString())
            .title(buildTaskTitle(context))
            .type("workflow")
            .dueDate(approval.getTimeoutAt())
            .status(mapStatus(approval.getDecision()))
            .priority(calculatePriority(context, approval.getTimeoutAt()))
            .assignedTo(approval.getApproverId().toString())
            .risk(extractRiskLevel(context))
            .requester(buildRequesterInfo(context))
            .description(buildDescription(context))
            .createdAt(approval.getRequestedAt())
            .decision(approval.getDecision())
            .respondedAt(approval.getRespondedAt())
            .comments(approval.getComments())
            .roleName(extractString(context, "roleName"))
            .workflowName(extractString(context, "workflowName"))
            .workflowInstanceId(approval.getExecutionId() != null ? approval.getExecutionId().toString() : null)
            .approverType(approval.getApproverType())
            .build();
    }
    
    /**
     * Maps WorkflowApprovalDTO to TaskDetailsDTO for detail modal.
     * Includes all 15+ IAM fields.
     * 
     * @param approval Workflow approval from workflows-service
     * @return Detailed task DTO for frontend
     */
    public TaskDetailsDTO mapToTaskDetailsDTO(WorkflowApprovalDTO approval) {
        Map<String, Object> context = approval.getApprovalContext();
        
        // Store context and approval in thread-local for helper methods
        contextThreadLocal.set(context);
        approvalThreadLocal.set(approval);
        try {
            return buildTaskDetailsDTO(approval, context);
        } finally {
            contextThreadLocal.remove();
            approvalThreadLocal.remove();
        }
    }
    
    private TaskDetailsDTO buildTaskDetailsDTO(WorkflowApprovalDTO approval, Map<String, Object> context) {
        String eventType = extractString(context, "eventType");
        
        // Debug logging
        System.out.println("=== TaskMapper.buildTaskDetailsDTO DEBUG ===");
        System.out.println("eventType: " + eventType);
        System.out.println("context keys: " + context.keySet());
        System.out.println("actionName: " + extractString(context, "actionName"));
        System.out.println("approvalContext size: " + (approval.getApprovalContext() != null ? approval.getApprovalContext().size() : "null"));
        
        TaskDetailsDTO dto = TaskDetailsDTO.builder()
            .id(approval.getId().toString())
            .title(buildTaskTitle(context))
            .type("AccessRequest")
            .status(mapDetailedStatus(approval.getDecision()))
            .priority(calculatePriority(context, approval.getTimeoutAt()))
            .risk(extractRiskLevel(context))
            .createdAt(approval.getRequestedAt())
            .dueAt(approval.getTimeoutAt())
            .requester(buildDetailedRequesterInfo(context))
            .assignee(TaskDetailsDTO.UserInfo.builder()
                .id(approval.getApproverId().toString())
                .build())
            .workflowName(extractString(context, "workflowName"))
            .workflowInstanceId(approval.getExecutionId() != null ? approval.getExecutionId().toString() : null)
            .approvalId(approval.getId().toString())
            .eventType(eventType)  // For dynamic UI rendering
            .tileName(extractString(context, "tileName"))
            .applicationName(extractString(context, "applicationName"))
            .role(buildRoleInfo(context))
            .targetUsers(extractString(context, "targetUsers"))
            .targetUserCount(extractInteger(context, "targetUserCount"))
            .justification(extractString(context, "justification"))
            // Dynamic event-specific data
            .permissionList(extractPermissionList(context, eventType))
            .singlePermission(extractSinglePermission(context, eventType))
            .targetUsersList(extractTargetUsersList(context, eventType))
            .approvalContext(approval.getApprovalContext())
            .evidenceBundle(approval.getEvidenceBundle())
            .approvalGroupId(approval.getApprovalGroupId() != null ? approval.getApprovalGroupId().toString() : null)
            .approvalPolicy(approval.getApprovalPolicy())
            .requiredApprovals(approval.getRequiredApprovals())
            .changes(new ArrayList<>()) // TODO: Extract from context if available
            .sodFindings(new ArrayList<>()) // TODO: Call SoD service
            .timeline(buildTimeline(approval))
            .comments(new ArrayList<>()) // TODO: Extract from context if available
            .build();
        
        // Debug: Verify DTO population
        System.out.println("Built DTO - eventType: " + dto.getEventType());
        System.out.println("Built DTO - singlePermission: " + dto.getSinglePermission());
        System.out.println("Built DTO - permissionList size: " + (dto.getPermissionList() != null ? dto.getPermissionList().size() : "null"));
        System.out.println("Built DTO - targetUsersList size: " + (dto.getTargetUsersList() != null ? dto.getTargetUsersList().size() : "null"));
        System.out.println("Built DTO - approvalContext size: " + (dto.getApprovalContext() != null ? dto.getApprovalContext().size() : "null"));
        
        return dto;
    }
    
    /**
     * Builds a human-readable task title from approval context.
     * Combines workflow name with operation details for maximum clarity.
     * Format: "Approve {WorkflowName} - {OperationDetails} for {UserName}"
     */
    private String buildTaskTitle(Map<String, Object> context) {
        WorkflowApprovalDTO approval = getCurrentApproval();
        String workflowName = extractString(context, "workflowName");
        String eventType = extractString(context, "eventType");
        String roleName = extractString(context, "roleName");
        String tileName = extractString(context, "tileName");
        String userName = extractString(context, "userName");
        
        // Build operation details based on event type
        String operationDetails = null;
        if (eventType != null) {
            operationDetails = buildOperationSummary(eventType);
        }
        
        // For role assignments/revocations, use target user name instead of role name
        boolean isRoleOperation = eventType != null && 
            (eventType.equalsIgnoreCase("role.assigned") || eventType.equalsIgnoreCase("role.revoked"));
        
        // If it's a role operation, try to extract the target username from evidenceBundle
        if (isRoleOperation && (userName == null || userName.isEmpty()) && approval != null && approval.getEvidenceBundle() != null) {
            Map<String, Object> evidenceBundle = approval.getEvidenceBundle();
            // Try multiple fields for target username
            userName = extractString(evidenceBundle, "targetUserDisplayName");
            if (userName == null || userName.isEmpty()) {
                userName = extractString(evidenceBundle, "targetUserName");
            }
            if (userName == null || userName.isEmpty()) {
                userName = extractString(evidenceBundle, "currentUserName");
            }
        }
        
        // Combine workflow name with operation details
        if (workflowName != null && !workflowName.isEmpty()) {
            if (operationDetails != null) {
                // Use userName for role operations, roleName for others
                String targetEntity = isRoleOperation && userName != null && !userName.isEmpty() 
                    ? userName 
                    : roleName;
                
                if (targetEntity != null) {
                    return String.format("Approve %s - %s for %s", workflowName, operationDetails, targetEntity);
                } else {
                    return String.format("Approve %s - %s", workflowName, operationDetails);
                }
            } else if (roleName != null) {
                return String.format("Approve %s for %s", workflowName, roleName);
            } else {
                return String.format("Approve %s", workflowName);
            }
        }
        
        // Fallback: Build from eventType + userName/roleName
        if (eventType != null) {
            if (isRoleOperation && userName != null && !userName.isEmpty()) {
                return buildTitleFromEventType(eventType, userName);
            } else if (roleName != null) {
                return buildTitleFromEventType(eventType, roleName);
            }
        }
        
        // Legacy fallback: workflow name only
        if (workflowName != null && !workflowName.isEmpty()) {
            return String.format("Approve %s", workflowName);
        }
        
        // Legacy fallback: tile + role name
        if (tileName != null && roleName != null) {
            return String.format("Approve %s access for %s", tileName, roleName);
        }
        
        // Last resort: generic role approval
        if (roleName != null) {
            return String.format("Approve %s role assignment", roleName);
        }
        
        return "Approve Workflow Request";
    }
    
    /**
     * Builds a concise operation summary for the title.
     * Examples:
     * - "role.assigned" -> "Role Assignment"
     * - "permission.bulk.assigned" -> "5 Permission Grants"
     * - "permission.assigned" -> "'View' Permission Grant"
     */
    private String buildOperationSummary(String eventType) {
        Map<String, Object> context = getCurrentContext();
        
        return switch (eventType.toLowerCase()) {
            case "role.assigned" -> "Role Assignment";
            case "role.revoked" -> "Role Revocation";
            case "permission.assigned" -> {
                String actionName = extractString(context, "actionName");
                if (actionName != null && !actionName.isEmpty()) {
                    yield String.format("'%s' Permission Grant", actionName);
                }
                yield "Permission Grant";
            }
            case "permission.revoked" -> {
                String actionName = extractString(context, "actionName");
                if (actionName != null && !actionName.isEmpty()) {
                    yield String.format("'%s' Permission Revocation", actionName);
                }
                yield "Permission Revocation";
            }
            case "permission.bulk.assigned" -> {
                Integer assignmentCount = extractInteger(context, "assignmentCount");
                if (assignmentCount != null && assignmentCount > 0) {
                    yield String.format("%d Permission Grants", assignmentCount);
                }
                yield "Multiple Permission Grants";
            }
            case "permission.bulk.revoked" -> {
                Integer assignmentCount = extractInteger(context, "assignmentCount");
                if (assignmentCount != null && assignmentCount > 0) {
                    yield String.format("%d Permission Revocations", assignmentCount);
                }
                yield "Multiple Permission Revocations";
            }
            default -> null;
        };
    }
    
    /**
     * Builds title from event type and role name.
     * Examples:
     * - "role.assigned" -> "Approve role assignment for {userName}"
     * - "role.revoked" -> "Approve role revocation for {userName}"
     * - "permission.assigned" -> "Approve {actionName} permission grant for {roleName}"
     * - "permission.bulk.assigned" -> "Approve 5 permission grants for {roleName}"
     */
    private String buildTitleFromEventType(String eventType, String roleName) {
        return switch (eventType.toLowerCase()) {
            case "role.assigned" -> buildRoleAssignmentTitle(roleName);
            case "role.revoked" -> buildRoleRevocationTitle(roleName);
            case "permission.assigned" -> buildSinglePermissionTitle(roleName, "grant");
            case "permission.revoked" -> buildSinglePermissionTitle(roleName, "revocation");
            case "permission.bulk.assigned" -> buildBulkPermissionTitle(roleName, "grants");
            case "permission.bulk.revoked" -> buildBulkPermissionTitle(roleName, "revocations");
            default -> String.format("Approve %s for %s", eventType, roleName);
        };
    }
    
    /**
     * Builds title for role assignment.
     * Uses userName if available, otherwise falls back to roleName.
     * Example: "Approve role assignment for John Doe"
     */
    private String buildRoleAssignmentTitle(String roleName) {
        Map<String, Object> context = getCurrentContext();
        String userName = extractString(context, "userName");
        
        if (userName != null && !userName.isEmpty()) {
            return String.format("Approve role assignment for %s", userName);
        }
        return String.format("Approve %s role assignment", roleName);
    }
    
    /**
     * Builds title for role revocation.
     * Uses userName if available, otherwise falls back to roleName.
     * Example: "Approve role revocation for John Doe"
     */
    private String buildRoleRevocationTitle(String roleName) {
        Map<String, Object> context = getCurrentContext();
        String userName = extractString(context, "userName");
        
        if (userName != null && !userName.isEmpty()) {
            return String.format("Approve role revocation for %s", userName);
        }
        return String.format("Approve %s role revocation", roleName);
    }
    
    /**
     * Builds title for single permission grant/revocation.
     * Example: "Approve 'View' permission grant for Dashboard Role"
     */
    private String buildSinglePermissionTitle(String roleName, String operation) {
        Map<String, Object> context = getCurrentContext();
        String actionName = extractString(context, "actionName");
        
        if (actionName != null && !actionName.isEmpty()) {
            return String.format("Approve '%s' permission %s for %s", actionName, operation, roleName);
        }
        return String.format("Approve permission %s for %s", operation, roleName);
    }
    
    /**
     * Builds title for bulk permission grants/revocations.
     * Example: "Approve 5 permission grants for Dashboard Role"
     */
    private String buildBulkPermissionTitle(String roleName, String operation) {
        Map<String, Object> context = getCurrentContext();
        Integer assignmentCount = extractInteger(context, "assignmentCount");
        
        if (assignmentCount != null && assignmentCount > 0) {
            return String.format("Approve %d permission %s for %s", assignmentCount, operation, roleName);
        }
        return String.format("Approve multiple permission %s for %s", operation, roleName);
    }
    
    // Thread-local to pass context between methods
    private static final ThreadLocal<Map<String, Object>> contextThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<WorkflowApprovalDTO> approvalThreadLocal = new ThreadLocal<>();
    
    private Map<String, Object> getCurrentContext() {
        Map<String, Object> ctx = contextThreadLocal.get();
        return ctx != null ? ctx : new java.util.HashMap<>();
    }
    
    private WorkflowApprovalDTO getCurrentApproval() {
        return approvalThreadLocal.get();
    }
    
    /**
     * Builds description with justification and permission details for bulk operations.
     * For single permissions: shows justification only
     * For bulk permissions: shows justification + list of permissions
     */
    private String buildDescription(Map<String, Object> context) {
        StringBuilder description = new StringBuilder();
        
        // Add justification if present
        String justification = extractString(context, "justification");
        if (justification != null && !justification.isEmpty()) {
            description.append(justification);
        }
        
        // Add permission list for bulk operations
        String eventType = extractString(context, "eventType");
        if (eventType != null && (eventType.contains("bulk") || eventType.contains("BULK"))) {
            Object assignmentsObj = context.get("assignments");
            if (assignmentsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> assignments = (java.util.List<Map<String, Object>>) assignmentsObj;
                
                if (!assignments.isEmpty()) {
                    if (description.length() > 0) {
                        description.append("\\n\\nPermissions:\\n");
                    } else {
                        description.append("Permissions:\\n");
                    }
                    
                    for (int i = 0; i < assignments.size(); i++) {
                        Map<String, Object> assignment = assignments.get(i);
                        String actionName = assignment.get("actionName") != null ? 
                            assignment.get("actionName").toString() : "Unknown";
                        description.append(String.format("%d. %s\\n", i + 1, actionName));
                    }
                }
            }
        }
        
        return description.length() > 0 ? description.toString() : null;
    }
    
    /**
     * Maps approval decision to task status.
     */
    private String mapStatus(String decision) {
        if (decision == null) {
            return "pending";
        }
        
        // Return the actual decision so UI can display correct badge
        return switch (decision.toUpperCase()) {
            case "PENDING" -> "pending";
            case "APPROVED" -> "approved";
            case "REJECTED" -> "rejected";
            case "TIMEOUT", "ESCALATED_TIMEOUT" -> "timeout";
            default -> "pending";
        };
    }
    
    /**
     * Maps approval decision to detailed status.
     */
    private String mapDetailedStatus(String decision) {
        if (decision == null) {
            return "pending";
        }
        
        // Return the actual decision for detailed views
        return switch (decision.toUpperCase()) {
            case "PENDING" -> "pending";
            case "APPROVED" -> "approved";
            case "REJECTED" -> "rejected";
            case "TIMEOUT", "ESCALATED_TIMEOUT" -> "timeout";
            default -> "pending";
        };
    }
    
    /**
     * Calculates task priority based on risk level and deadline urgency.
     */
    private String calculatePriority(Map<String, Object> context, OffsetDateTime deadline) {
        String riskLevel = extractRiskLevel(context);
        
        // High/Critical risk = high priority
        if ("CRITICAL".equalsIgnoreCase(riskLevel) || "HIGH".equalsIgnoreCase(riskLevel)) {
            return "high";
        }
        
        // Check deadline urgency
        if (deadline != null) {
            long hoursUntilDeadline = Duration.between(OffsetDateTime.now(), deadline).toHours();
            if (hoursUntilDeadline < 24) {
                return "high";
            }
            if (hoursUntilDeadline < 72) {
                return "medium";
            }
        }
        
        return "low";
    }
    
    /**
     * Extracts risk level from context, normalizes to lowercase.
     */
    private String extractRiskLevel(Map<String, Object> context) {
        String risk = extractString(context, "riskLevel");
        return risk != null ? risk.toLowerCase() : "medium";
    }
    
    /**
     * Builds requester information from context.
     */
    private TaskDTO.UserInfo buildRequesterInfo(Map<String, Object> context) {
        // Try multiple field names for compatibility
        String name = extractString(context, "requesterFullName");
        if (name == null || name.isEmpty()) {
            name = extractString(context, "createdBy");
        }
        
        String id = extractString(context, "requesterId");
        if (id == null || id.isEmpty()) {
            id = extractString(context, "createdById");
        }
        
        return TaskDTO.UserInfo.builder()
            .id(id)
            .name(name)
            .email(extractString(context, "requesterEmail"))
            .department(extractString(context, "requesterDepartment"))
            .build();
    }
    
    /**
     * Builds detailed requester information from context for TaskDetailsDTO.
     */
    private TaskDetailsDTO.UserInfo buildDetailedRequesterInfo(Map<String, Object> context) {
        // Try multiple field names for compatibility
        String name = extractString(context, "requesterFullName");
        if (name == null || name.isEmpty()) {
            name = extractString(context, "createdBy");
        }
        
        String id = extractString(context, "requesterId");
        if (id == null || id.isEmpty()) {
            id = extractString(context, "createdById");
        }
        
        return TaskDetailsDTO.UserInfo.builder()
            .id(id)
            .name(name)
            .email(extractString(context, "requesterEmail"))
            .department(extractString(context, "requesterDepartment"))
            .build();
    }
    
    /**
     * Builds role information from context.
     */
    private TaskDetailsDTO.RoleInfo buildRoleInfo(Map<String, Object> context) {
        return TaskDetailsDTO.RoleInfo.builder()
            .id(extractString(context, "roleId"))
            .name(extractString(context, "roleName"))
            .type(extractString(context, "roleType"))
            .description(extractString(context, "roleDescription"))
            .build();
    }
    
    /**
     * Builds timeline from approval lifecycle.
     */
    private java.util.List<TaskDetailsDTO.TimelineEvent> buildTimeline(WorkflowApprovalDTO approval) {
        java.util.List<TaskDetailsDTO.TimelineEvent> timeline = new ArrayList<>();
        Map<String, Object> context = getCurrentContext();
        
        // Created event - use createdBy field (this is the actual requester username)
        String requesterName = extractString(context, "createdBy");
        
        timeline.add(TaskDetailsDTO.TimelineEvent.builder()
            .id("1")
            .type("created")
            .actor(requesterName != null && !requesterName.isEmpty() ? requesterName : "System")
            .description("Approval request created")
            .timestamp(approval.getRequestedAt())
            .build());
        
        // Responded event (if applicable) - use approver username
        if (approval.getRespondedAt() != null) {
            String eventType = "APPROVED".equalsIgnoreCase(approval.getDecision()) ? "approved" : "rejected";
            
            // Try to get approver username from different possible context fields
            String approverName = extractString(context, "respondedBy");
            if (approverName == null || approverName.isEmpty()) {
                approverName = extractString(context, "approvedBy");
            }
            if (approverName == null || approverName.isEmpty()) {
                approverName = extractString(context, "approverUsername");
            }
            if (approverName == null || approverName.isEmpty()) {
                approverName = extractString(context, "approverName");
            }
            // If still not found, fallback to approverId as string
            if ((approverName == null || approverName.isEmpty()) && approval.getApproverId() != null) {
                approverName = approval.getApproverId().toString();
            }
            
            String description;
            if (approverName != null && !approverName.isEmpty()) {
                description = eventType.substring(0, 1).toUpperCase() + eventType.substring(1) + " by " + approverName;
            } else {
                description = eventType.substring(0, 1).toUpperCase() + eventType.substring(1);
            }
            
            timeline.add(TaskDetailsDTO.TimelineEvent.builder()
                .id("2")
                .type(eventType)
                .actor(approverName != null && !approverName.isEmpty() ? approverName : "approver")
                .description(description)
                .timestamp(approval.getRespondedAt())
                .build());
        }
        
        return timeline;
    }
    
    /**
     * Safely extracts string from context map.
     */
    private String extractString(Map<String, Object> context, String key) {
        if (context == null || !context.containsKey(key)) {
            return null;
        }
        Object value = context.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Safely extracts integer from context map.
     */
    private Integer extractInteger(Map<String, Object> context, String key) {
        if (context == null || !context.containsKey(key)) {
            return null;
        }
        Object value = context.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Extracts permission list for bulk permission operations.
     */
    private java.util.List<TaskDetailsDTO.PermissionItem> extractPermissionList(Map<String, Object> context, String eventType) {
        if (eventType == null || !eventType.contains("bulk")) {
            return null;
        }
        
        // For bulk.assigned, use "assignments" list
        // For bulk.revoked, use "actionItems" list (which includes page names)
        Object listObj = eventType.equals("permission.bulk.revoked") 
            ? context.get("actionItems") 
            : context.get("assignments");
            
        if (!(listObj instanceof java.util.List)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) listObj;
        
        return items.stream()
            .map(item -> TaskDetailsDTO.PermissionItem.builder()
                .actionId(item.get("actionId") != null ? item.get("actionId").toString() : null)
                .actionName(item.get("actionName") != null ? item.get("actionName").toString() : "Unknown Permission")
                .pageName(item.get("pageName") != null ? item.get("pageName").toString() : null) // Add page name
                .justification(item.get("justification") != null ? item.get("justification").toString() : null)
                .conditionScript(item.get("conditionScript") != null ? item.get("conditionScript").toString() : null)
                .build())
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Extracts single permission for single permission operations.
     */
    private TaskDetailsDTO.PermissionItem extractSinglePermission(Map<String, Object> context, String eventType) {
        if (eventType == null || 
            (!eventType.equals("permission.assigned") && !eventType.equals("permission.revoked"))) {
            return null;
        }
        
        String actionId = extractString(context, "actionId");
        String actionName = extractString(context, "actionName");
        
        if (actionName == null) {
            return null;
        }
        
        // Extract page name from context (should be added by event publisher)
        String pageName = extractString(context, "pageName");
        
        return TaskDetailsDTO.PermissionItem.builder()
            .actionId(actionId)
            .actionName(actionName)
            .pageName(pageName)
            .justification(extractString(context, "justification"))
            .conditionScript(extractString(context, "conditionScript"))
            .build();
    }
    
    /**
     * Extracts target users list for role assignments and revocations.
     */
    private java.util.List<TaskDetailsDTO.UserInfo> extractTargetUsersList(Map<String, Object> context, String eventType) {
        if (eventType == null || 
            (!eventType.equals("role.assigned") && !eventType.equals("role.revoked"))) {
            return null;
        }
        
        Object userIdsObj = context.get("userIds");
        if (!(userIdsObj instanceof java.util.List)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<String> userIds = (java.util.List<String>) userIdsObj;
        
        // Build userName lookup from managers array (Access Portal payload)
        java.util.Map<String, String> userNameMap = new java.util.HashMap<>();
        Object managersObj = context.get("managers");
        if (managersObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> managers = (java.util.List<java.util.Map<String, Object>>) managersObj;
            for (java.util.Map<String, Object> manager : managers) {
                Object userIdObj = manager.get("userId");
                Object userNameObj = manager.get("userName");
                if (userIdObj != null && userNameObj != null) {
                    userNameMap.put(userIdObj.toString(), userNameObj.toString());
                }
            }
        }
        
        // Fallback: Check for userDetails array
        if (userNameMap.isEmpty()) {
            Object userDetailsObj = context.get("userDetails");
            if (userDetailsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, String>> userDetails = (java.util.List<java.util.Map<String, String>>) userDetailsObj;
                for (java.util.Map<String, String> detail : userDetails) {
                    String userId = detail.get("userId");
                    String userName = detail.get("userName");
                    if (userId != null && userName != null) {
                        userNameMap.put(userId, userName);
                    }
                }
            }
        }
        
        // Build user list with names from map or fallback to short user ID
        return userIds.stream()
            .map(userId -> {
                String userName = userNameMap.get(userId);
                if (userName == null || userName.isEmpty()) {
                    // Fallback to short user ID
                    userName = "User " + userId.substring(Math.max(0, userId.length() - 8));
                }
                return TaskDetailsDTO.UserInfo.builder()
                    .id(userId)
                    .name(userName)
                    .build();
            })
            .collect(java.util.stream.Collectors.toList());
    }
}

