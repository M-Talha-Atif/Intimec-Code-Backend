package com.aidentx.workflowsservice.service;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

import com.aidentx.workflowsservice.client.IdentityGrpcClient;
import com.aidentx.workflowsservice.client.IdentityRestClient;
import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.client.UserGrpcClient;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.event.AccessPortalEventPublisher;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Executes individual workflow nodes
 * Handles different node types: triggers, approvals, conditions, actions, etc.
 */
@Component
public class NodeExecutor {

    private final IdentityGrpcClient identityGrpcClient;
    private final IdentityRestClient identityRestClient;
    private final UserGrpcClient userClient;
    private final NotificationKafkaClient notificationClient;
    private final WorkflowApprovalService approvalService;
    private final ApproverResolver approverResolver;
    private final ApproverResolutionService approverResolutionService;
    private final com.aidentx.workflowsservice.repository.ParallelExecutionRepository parallelExecutionRepository;
    private final com.aidentx.workflowsservice.repository.ParallelBranchContextRepository parallelBranchContextRepository;
    private final ObjectMapper objectMapper;
    private final ActionTemplateService actionTemplateService;
    private final AccessPortalEventPublisher accessPortalEventPublisher;
    private final JwtUtils jwtUtils;
    private final IUnifiedLogger logger;

    public NodeExecutor(
            IdentityGrpcClient identityGrpcClient,
            IdentityRestClient identityRestClient,
            UserGrpcClient userClient,
            NotificationKafkaClient notificationClient,
            WorkflowApprovalService approvalService,
            ApproverResolver approverResolver,
            ApproverResolutionService approverResolutionService,
            com.aidentx.workflowsservice.repository.ParallelExecutionRepository parallelExecutionRepository,
            com.aidentx.workflowsservice.repository.ParallelBranchContextRepository parallelBranchContextRepository,
            ObjectMapper objectMapper,
            ActionTemplateService actionTemplateService,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory,
            AccessPortalEventPublisher accessPortalEventPublisher) {
        this.identityGrpcClient = identityGrpcClient;
        this.identityRestClient = identityRestClient;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
        this.approvalService = approvalService;
        this.approverResolver = approverResolver;
        this.approverResolutionService = approverResolutionService;
        this.parallelExecutionRepository = parallelExecutionRepository;
        this.parallelBranchContextRepository = parallelBranchContextRepository;
        this.objectMapper = objectMapper;
        this.actionTemplateService = actionTemplateService;
        this.jwtUtils = jwtUtils;
        this.accessPortalEventPublisher = accessPortalEventPublisher;
        this.logger = loggerFactory.getLogger(NodeExecutor.class);
    }

    /**
     * Execute a single node
     */
    public Map<String, Object> execute(
        Map<String, Object> node,
        Map<String, Object> context,
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        // Frontend stores actual node type in node.data.stepType, not node.type
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        String nodeType = nodeData != null && nodeData.containsKey("stepType") ? 
            (String) nodeData.get("stepType") : 
            (String) node.get(FIELD_TYPE);
        
        logger.debug()
            .message(LOG_EXECUTING_WORKFLOW_NODE)
            .category("node-execution")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", execution.getId().toString())
            .detail("workflowId", execution.getWorkflowId().toString())
            .detail("nodeType", nodeType)
            .detail("nodeData", node.get("data"))
            .log();
        
        // Execute based on node type
        if (nodeType == null) {
            logger.warn()
                .message(LOG_NODE_HAS_NO_TYPE)
                .category("node-execution")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("executionId", execution.getId().toString())
                .log();
            return Map.of();
        }
        
        return switch (nodeType) {
            case NODE_TYPE_TRIGGER_START -> executeTriggerStart(node, context, execution);
            case NODE_TYPE_TRIGGER_END -> executeTriggerEnd(node, context, execution);
            case NODE_TYPE_APPROVAL_SINGLE -> executeApprovalSingle(node, context, execution);
            case NODE_TYPE_APPROVAL_MULTI -> executeApprovalMulti(node, context, execution);
            case NODE_TYPE_APPROVAL_DYNAMIC -> executeApprovalDynamic(node, context, execution);
            case NODE_TYPE_CONDITION_IF -> executeConditionIf(node, context, execution);
            case NODE_TYPE_CONDITION_SWITCH -> executeConditionSwitch(node, context, execution);
            case NODE_TYPE_ACTION_PROVISION -> executeActionProvision(node, context, execution);
            case NODE_TYPE_ACTION_DEPROVISION -> executeActionDeprovision(node, context, execution);
            case NODE_TYPE_ACTION_NOTIFY -> executeActionNotify(node, context, execution);
            case NODE_TYPE_ACTION_SCRIPT -> executeActionScript(node, context, execution);
            case NODE_TYPE_ACTION_HTTP -> executeActionHttp(node, context, execution);
            case NODE_TYPE_DELAY_WAIT -> executeDelayWait(node, context, execution);
            case NODE_TYPE_DELAY_SCHEDULE -> executeDelaySchedule(node, context, execution);
            case NODE_TYPE_GATEWAY_PARALLEL_SPLIT -> executeParallelSplit(node, context, execution);
            case NODE_TYPE_GATEWAY_PARALLEL_JOIN -> executeParallelJoin(node, context, execution);
            case NODE_TYPE_CONTROL_FOREACH -> executeForEach(node, context, execution);
            case NODE_TYPE_CONTROL_FOREACH_END -> executeForEachEnd(node, context, execution);
            case NODE_TYPE_GATEWAY_SWITCH -> executeDynamicSwitch(node, context, execution);
            case NODE_TYPE_GATEWAY_SWITCH_JOIN -> executeDynamicSwitchJoin(node, context, execution);
            case NODE_TYPE_GATEWAY_DYNAMIC_SPLIT -> executeDynamicParallelSplit(node, context, execution);
            case NODE_TYPE_GATEWAY_DYNAMIC_JOIN -> executeDynamicParallelJoin(node, context, execution);
            default -> {
                // Check if it's a templated action (e.g., action.role.activate, action.user.create)
                if (nodeType != null && actionTemplateService.isTemplatedAction(nodeType)) {
                    logger.info()
                        .message(LOG_EXECUTING_TEMPLATED_ACTION)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .detail("nodeType", nodeType)
                        .log();
                    yield executeTemplatedAction(node, context, execution);
                } else {
                    logger.warn()
                        .message(LOG_UNKNOWN_NODE_TYPE)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .detail("nodeType", nodeType)
                        .log();
                    yield Map.of(CONTEXT_KEY_EXECUTED, true, CONTEXT_KEY_NODE_TYPE, nodeType);
                }
            }
        };
    }

    /**
     * Execute trigger.start node
     */
    private Map<String, Object> executeTriggerStart(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_STARTING_WORKFLOW_TRIGGER_START)
            .category("node-execution")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("nodeType", NODE_TYPE_TRIGGER_START)
            .detail("executionId", execution.getId().toString())
            .log();
        
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_TRIGGER_START);
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        output.put(CONTEXT_KEY_EXECUTION_ID, execution.getId());
        output.put(CONTEXT_KEY_WORKFLOW_ID, execution.getWorkflowId());
        
        // Pass through execution context
        if (execution.getExecutionContext() != null) {
            output.putAll(execution.getExecutionContext());
        }
        
        return output;
    }

    /**
     * Execute trigger.end node
     */
    private Map<String, Object> executeTriggerEnd(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_ENDING_WORKFLOW_TRIGGER_END)
            .category("node-execution")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("nodeType", NODE_TYPE_TRIGGER_END)
            .detail("executionId", execution.getId().toString())
            .log();
        
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_TRIGGER_END);
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        output.put(CONTEXT_KEY_STATUS, DEFAULT_STATUS_COMPLETED);
        
        // Don't include full context to avoid circular reference
        // Context is already tracked in execution
        
        return output;
    }

    /**
     * Execute approval.single node (single approver)
     */
    private Map<String, Object> executeApprovalSingle(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_SINGLE_APPROVAL_NODE)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("executionId", execution.getId().toString())
            .log();
        
        // Check simulation mode from execution context
        Boolean simulationMode = execution.getExecutionContext() != null ? 
            (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        if (Boolean.TRUE.equals(simulationMode)) {
            // Check if user has made an approval decision
            Map<String, Object> approvalDecision = (Map<String, Object>) execution.getExecutionContext().get("simulationApprovalDecision");
            if (approvalDecision != null) {
                // User has made a decision - process it
                Boolean approved = (Boolean) approvalDecision.get("approved");
                logger.info()
                    .message("Processing simulation approval decision")
                    .category("approval")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("executionId", execution.getId().toString())
                    .detail("approved", approved)
                    .log();
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_APPROVAL_SINGLE,
                    CONTEXT_KEY_EXECUTED, true,
                    CONTEXT_KEY_APPROVED, approved,
                    CONTEXT_KEY_APPROVER, approvalDecision.get("approverId"),
                    "comments", approvalDecision.getOrDefault("comments", ""),
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
            // No decision yet - pause for user interaction
            logger.info()
                .message("Simulation approval node - pausing for user decision")
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("executionId", execution.getId().toString())
                .log();
            
            // Return pause indicator to trigger interactive approval UI
            Map<String, Object> result = new HashMap<>();
            result.put(CONTEXT_KEY_PAUSE_REQUIRED, true);
            result.put(CONTEXT_KEY_REASON, "Waiting for simulation approval");
            result.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_APPROVAL_SINGLE);
            result.put("approvers", List.of("simulation-user"));
            return result;
        }
        
        // Extract node configuration
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_NODE_DATA_MISSING);
        }
        
        // Get approver ID - try node config first (manual workflows), then fallback to context (event workflows)
        String approverIdStr = (String) nodeData.get(CONTEXT_KEY_APPROVER_ID);
        
        // NEW: Support resolving approverId from a dynamic context key (for forEach loop integration)
        // Example: approverContextKey: "currentApproverId" resolves from context.get("currentApproverId")
        if (approverIdStr == null || approverIdStr.isEmpty()) {
            String approverContextKey = (String) nodeData.get("approverContextKey");
            if (approverContextKey != null && !approverContextKey.isEmpty()) {
                Object contextApproverId = context.get(approverContextKey);
                if (contextApproverId != null) {
                    approverIdStr = contextApproverId.toString();
                    logger.debug()
                        .message("Using approver ID from context key")
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("approverContextKey", approverContextKey)
                        .detail("approverId", approverIdStr)
                        .log();
                }
            }
        }
        
        // Fallback: If not in node config, use ownerIds from context (for event-triggered workflows like role.created)
        if (approverIdStr == null || approverIdStr.isEmpty()) {
            Object ownerIdsObj = context.get("ownerIds");
            if (ownerIdsObj instanceof List) {
                List<?> ownerIds = (List<?>) ownerIdsObj;
                if (!ownerIds.isEmpty()) {
                    approverIdStr = ownerIds.get(0).toString();
                    logger.debug()
                        .message(LOG_USING_FIRST_OWNER_ID_AS_APPROVER)
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("approverId", approverIdStr)
                        .detail("reason", "no_approver_in_node_config")
                        .log();
                }
            }
        }
        
        if (approverIdStr == null || approverIdStr.isEmpty()) {
            throw new IllegalArgumentException(ERROR_APPROVER_ID_REQUIRED);
        }
        
        UUID approverId = UUID.fromString(approverIdStr);
        
        // Get optional timeout (default 24 hours)
        Integer timeoutMinutes = nodeData.get(FIELD_TIMEOUT_MINUTES) != null ? 
            (Integer) nodeData.get(FIELD_TIMEOUT_MINUTES) : DEFAULT_APPROVAL_TIMEOUT_MINUTES;
        
        // Get optional email configuration
        Boolean sendEmail = nodeData.get("sendEmail") != null ? 
            (Boolean) nodeData.get("sendEmail") : false;
        String emailSubject = (String) nodeData.get("emailSubject");
        String emailBody = (String) nodeData.get("emailBody");
        
        // Create approval request
        WorkflowApproval approval = approvalService.createApprovalRequest(
            execution.getId(),
            null, // nodeExecutionId will be set by caller
            approverId,
            WorkflowApproval.ApproverType.SPECIFIC,
            context, // Pass execution context as approval context
            Map.of(
                CONTEXT_KEY_NODE_ID, node.get(FIELD_ID),
                CONTEXT_KEY_NODE_NAME, nodeData.getOrDefault(FIELD_LABEL, DEFAULT_APPROVAL_LABEL)
            ),
            timeoutMinutes,
            null, // No approval group for single approval
            sendEmail,
            emailSubject,
            emailBody
        );
        
        logger.info()
            .message(LOG_CREATED_APPROVAL_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType("ApprovalRequest")
            .objectId(approval.getId().toString())
            .detail("approverId", approverId.toString())
            .log();
        
        // Return pause signal to WorkflowExecutor
        return Map.of(
            CONTEXT_KEY_PAUSE_REQUIRED, true,
            CONTEXT_KEY_REASON, "Waiting for approval",
            CONTEXT_KEY_APPROVAL_ID, approval.getId().toString(),
            CONTEXT_KEY_APPROVER_ID, approverId.toString(),
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_APPROVAL_SINGLE,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute approval.multi node (multiple approvers)
     * Phase 6b: Implements true multi-approval - ALL approvers must approve
     * Supports dynamic approver resolution from context (e.g., role_owner)
     */
    private Map<String, Object> executeApprovalMulti(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.trace()
            .message(LOG_TRACE_ENTERING_MULTI_APPROVAL)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("executionId", execution.getId().toString())
            .log();
        
        logger.info()
            .message(LOG_EXECUTING_MULTI_APPROVAL_NODE)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("executionId", execution.getId().toString())
            .log();
        
        // Extract node configuration
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_NODE_DATA_MISSING);
        }
        
        // Check for structured approvers list from context
        // Access-portal sends "managers" array with {userId, managerId, userName}
        // This allows creating per-user approvals where each manager approves for a specific user
        String structuredApproversKey = (String) nodeData.get("structuredApproversKey");
        
        // Default to "managers" if approverSource is user_manager_pairs but no key specified
        String approverSource = (String) nodeData.get("approverSource");
        if ("user_manager_pairs".equals(approverSource) && structuredApproversKey == null) {
            structuredApproversKey = "managers"; // Default key from access-portal
        }
        
        logger.info()
            .message("Multi-Approval node configuration")
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("structuredApproversKey", structuredApproversKey)
            .detail("approverSource", approverSource)
            .detail("approverType", nodeData.get("approverType"))
            .detail("hasApproverResolution", nodeData.get("approverResolution") != null)
            .log();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> structuredApprovers = structuredApproversKey != null 
            ? (List<Map<String, Object>>) context.get(structuredApproversKey) 
            : null;
        
        // CRITICAL: Filter structuredApprovers based on approvedUserIds from previous approval node
        // This ensures chained approvals only process users approved by previous node
        // ONLY filter if approvedUserIds is BOTH present AND non-empty (indicates previous approval node)
        if (structuredApprovers != null && !structuredApprovers.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> approvedUserIds = (List<String>) context.get("approvedUserIds");
            
            // Only apply filtering if approvedUserIds has actual values (not just exists)
            // This prevents filtering out all approvers when starting a fresh workflow
            if (approvedUserIds != null && !approvedUserIds.isEmpty()) {
                // Filter to only include user-manager pairs where userId is in approvedUserIds
                int originalCount = structuredApprovers.size();
                List<Map<String, Object>> filteredApprovers = structuredApprovers.stream()
                    .filter(pair -> {
                        Object userIdObj = pair.get("userId");
                        return userIdObj != null && approvedUserIds.contains(userIdObj.toString());
                    })
                    .collect(Collectors.toList());
                
                // SAFETY: Only apply filter if it produces results, otherwise keep original list
                // This handles edge case where approvedUserIds exists but doesn't match any userIds
                if (!filteredApprovers.isEmpty()) {
                    structuredApprovers = filteredApprovers;
                    logger.info()
                        .message("Filtered structuredApprovers based on approvedUserIds from previous node")
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("originalCount", originalCount)
                        .detail("filteredCount", structuredApprovers.size())
                        .detail("reason", "chained_approval_filtering")
                        .log();
                } else {
                    logger.warn()
                        .message("ApprovedUserIds filtering would remove all approvers - keeping original list")
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("originalCount", originalCount)
                        .detail("approvedUserIds", approvedUserIds)
                        .log();
                }
            }
        }
        
        // If structuredApproversKey is set but data is missing, FAIL - no fallback
        if (structuredApproversKey != null && (structuredApprovers == null || structuredApprovers.isEmpty())) {
            logger.error()
                .message("structuredApproversKey is configured but data not found in context - failing node")
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("structuredApproversKey", structuredApproversKey)
                .detail("approverSource", approverSource)
                .detail("contextKeys", context.keySet().toString())
                .log();
            throw new IllegalArgumentException(
                "Structured approvers data not found in context for key: " + structuredApproversKey + 
                ". Cannot proceed without required approver data. Context has keys: " + context.keySet()
            );
        }
        
        // Phase 1 (Week 1-2): Check for flexible approver resolution system first
        @SuppressWarnings("unchecked")
        Map<String, Object> approverResolution = (Map<String, Object>) nodeData.get("approverResolution");
        
        List<String> approverIdStrings;
        WorkflowApproval.ApproverType approvalType;
        
        // NEW PATH: Use structured approvers if available
        if (structuredApprovers != null && !structuredApprovers.isEmpty()) {
            logger.info()
                .message("Using structured approvers list for per-user approvals")
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("structuredApproversKey", structuredApproversKey)
                .detail("count", structuredApprovers.size())
                .log();
            
            // Extract approver IDs for logging (actual approval creation happens below)
            approverIdStrings = new ArrayList<>();
            for (Map<String, Object> pair : structuredApprovers) {
                Object managerId = pair.get("managerId");
                if (managerId != null) {
                    approverIdStrings.add(managerId.toString());
                }
            }
            approvalType = WorkflowApproval.ApproverType.MANAGER;
            
        } else if (approverResolution != null) {
            // NEW: Use flexible approver resolution system
            logger.info()
                .message(LOG_USING_FLEXIBLE_RESOLUTION)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("strategy", approverResolution.get(CONFIG_KEY_STRATEGY))
                .log();
            
            approverIdStrings = approverResolutionService.resolveApprovers(approverResolution, context, execution);
            approvalType = WorkflowApproval.ApproverType.DYNAMIC;
            
            if (approverIdStrings == null || approverIdStrings.isEmpty()) {
                throw new IllegalArgumentException(ERROR_NO_APPROVERS_RESOLVED);
            }
            
            logger.info()
                .message(LOG_FLEXIBLE_RESOLUTION_SUCCESS)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("approverCount", approverIdStrings.size())
                .detail("strategy", approverResolution.get(CONFIG_KEY_STRATEGY))
                .log();
            
        } else {
            // BACKWARD COMPATIBILITY: Use legacy resolution logic
            logger.trace()
                .message(LOG_USING_LEGACY_RESOLUTION)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .log();
            
            // Check if approverType is specified for dynamic resolution
            String approverType = (String) nodeData.get("approverType");
        
        if ("role_owner".equals(approverType)) {
            // Dynamic resolution: extract approvers from context
            logger.trace()
                .message(LOG_TRACE_RESOLVED_CONTEXT_APPROVERS)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("approverType", approverType)
                .log();
            
            @SuppressWarnings("unchecked")
            List<String> contextApprovers = (List<String>) context.get("ownerIds");
            
            if (contextApprovers == null || contextApprovers.isEmpty()) {
                // Check if in simulation mode - provide mock approvers when event data is missing
                boolean simulationMode = execution.getExecutionContext() != null && 
                    Boolean.TRUE.equals(execution.getExecutionContext().get("simulationMode"));
                
                if (simulationMode) {
                    // Determine number of mock approvers from test data or default to 3
                    int numberOfApprovers = 3; // default
                    if (context.get("numberOfApprovers") instanceof Integer) {
                        numberOfApprovers = (Integer) context.get("numberOfApprovers");
                    }
                    
                    // Generate mock approver UUIDs
                    approverIdStrings = new ArrayList<>();
                    for (int i = 1; i <= numberOfApprovers; i++) {
                        approverIdStrings.add(String.format("00000000-0000-0000-0000-%012d", i));
                    }
                    
                    approvalType = WorkflowApproval.ApproverType.ROLE_OWNER;
                    logger.info()
                        .message("Using mock approvers for simulation (role_owner type)")
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("approverCount", approverIdStrings.size())
                        .detail("reason", "simulation_mode_no_event_data")
                        .log();
                } else {
                    throw new IllegalArgumentException("Role owner IDs not found in context for approverType: role_owner");
                }
            } else {
                approverIdStrings = contextApprovers;
                approvalType = WorkflowApproval.ApproverType.ROLE_OWNER;
                logger.info()
                    .message(LOG_RESOLVED_ROLE_OWNERS_MULTI_APPROVAL)
                    .category("approval")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("ownerCount", approverIdStrings.size())
                    .log();
            }
            
        } else {
            // Static resolution: get approvers from node configuration
            @SuppressWarnings("unchecked")
            List<String> configApprovers = (List<String>) nodeData.get(FIELD_APPROVERS);
            
            // Fallback: If no approvers in config, try ownerIds from context (event workflows)
            if (configApprovers == null || configApprovers.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<String> contextApprovers = (List<String>) context.get("ownerIds");
                
                if (contextApprovers != null && !contextApprovers.isEmpty()) {
                    approverIdStrings = contextApprovers;
                    approvalType = WorkflowApproval.ApproverType.ROLE_OWNER;
                    logger.info()
                        .message(LOG_USING_OWNER_IDS_MULTI_APPROVAL)
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("ownerCount", approverIdStrings.size())
                        .detail("reason", "no_approvers_in_node_config")
                        .log();
                } else {
                    // Check if in simulation mode - provide mock approvers
                    boolean simulationMode = execution.getExecutionContext() != null && 
                        Boolean.TRUE.equals(execution.getExecutionContext().get("simulationMode"));
                    
                    if (simulationMode) {
                        // Determine number of mock approvers from test data or default to 3
                        int numberOfApprovers = 3; // default
                        if (context.get("numberOfApprovers") instanceof Integer) {
                            numberOfApprovers = (Integer) context.get("numberOfApprovers");
                        }
                        
                        // Generate mock approver UUIDs
                        approverIdStrings = new ArrayList<>();
                        for (int i = 1; i <= numberOfApprovers; i++) {
                            approverIdStrings.add(String.format("00000000-0000-0000-0000-%012d", i));
                        }
                        
                        approvalType = WorkflowApproval.ApproverType.ROLE_OWNER;
                        logger.info()
                            .message("Using mock approvers for simulation")
                            .category("approval")
                            .objectType("WorkflowNode")
                            .objectId((String) node.get(FIELD_ID))
                            .detail("approverCount", approverIdStrings.size())
                            .detail("reason", "simulation_mode_no_event_data")
                            .log();
                    } else {
                        throw new IllegalArgumentException(ERROR_MULTI_APPROVAL_NO_APPROVERS);
                    }
                }
            } else {
                approverIdStrings = configApprovers;
                approvalType = WorkflowApproval.ApproverType.SPECIFIC;
            }
        }
        // End of backward compatibility block
        }
        
        // Phase 1: Extract approval policy configuration (defaults to ALL for backward compatibility)
        String policyStr = (String) nodeData.getOrDefault("approvalPolicy", APPROVAL_POLICY_ALL);
        com.aidentx.workflowsservice.enums.ApprovalPolicy approvalPolicy;
        try {
            approvalPolicy = com.aidentx.workflowsservice.enums.ApprovalPolicy.valueOf(policyStr);
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(ERROR_INVALID_APPROVAL_POLICY)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("invalidPolicy", policyStr)
                .detail("usingDefault", APPROVAL_POLICY_ALL)
                .log();
            approvalPolicy = com.aidentx.workflowsservice.enums.ApprovalPolicy.ALL;
        }
        
        Integer requiredApprovals = null;
        if (approvalPolicy == com.aidentx.workflowsservice.enums.ApprovalPolicy.THRESHOLD) {
            requiredApprovals = (Integer) nodeData.get(CONFIG_KEY_REQUIRED_APPROVALS);
            if (requiredApprovals == null || requiredApprovals <= 0) {
                throw new IllegalArgumentException(ERROR_THRESHOLD_REQUIRED_APPROVALS_MISSING);
            }
        }
        
        // Get optional timeout (default 24 hours)
        Integer timeoutMinutes = nodeData.get(FIELD_TIMEOUT_MINUTES) != null ? 
            (Integer) nodeData.get(FIELD_TIMEOUT_MINUTES) : DEFAULT_APPROVAL_TIMEOUT_MINUTES;
        
        // Get optional email configuration
        Boolean sendEmail = nodeData.get("sendEmail") != null ? 
            (Boolean) nodeData.get("sendEmail") : false;
        String emailSubject = (String) nodeData.get("emailSubject");
        String emailBody = (String) nodeData.get("emailBody");
        
        logger.info()
            .message(LOG_CREATING_MULTI_APPROVAL_REQUESTS)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("approverCount", approverIdStrings.size())
            .detail("executionId", execution.getId().toString())
            .log();
        
        // Phase 7: Generate approval group ID for this multi-approval
        UUID approvalGroupId = UUID.randomUUID();
        
        logger.trace()
            .message(LOG_TRACE_GENERATED_APPROVAL_GROUP_ID)
            .category("approval")
            .objectType("ApprovalGroup")
            .objectId(approvalGroupId.toString())
            .detail("approverCount", approverIdStrings.size())
            .detail("nodeId", (String) node.get(FIELD_ID))
            .log();
        
        logger.debug()
            .message(LOG_GENERATED_APPROVAL_GROUP_ID)
            .category("approval")
            .objectType("ApprovalGroup")
            .objectId(approvalGroupId.toString())
            .detail("approverCount", approverIdStrings.size())
            .log();
        
        // Create approval request for EACH approver with same group ID
        List<String> approvalIds = new ArrayList<>();
        
        // NEW: Handle structured approvers (each approval has user context)
        if (structuredApprovers != null && !structuredApprovers.isEmpty()) {
            for (Map<String, Object> pair : structuredApprovers) {
                try {
                    Object managerIdObj = pair.get("managerId");
                    if (managerIdObj == null) {
                        logger.warn()
                            .message("Structured approver missing managerId, skipping")
                            .category("approval")
                            .detail("pair", pair.toString())
                            .log();
                        continue;
                    }
                    
                    UUID approverId = UUID.fromString(managerIdObj.toString());
                    
                    // Build approval context with user info for this specific approval
                    Map<String, Object> approvalContext = new HashMap<>(context);
                    approvalContext.put("targetUserId", pair.get("userId"));
                    approvalContext.put("currentUserName", pair.get("userName")); // Access Portal uses currentUserName for manager approvals
                    approvalContext.put("targetManagerId", pair.get("managerId"));
                    approvalContext.put("targetManagerName", pair.get("managerName"));
                    
                    // Evidence bundle includes user info for tracking
                    Map<String, Object> evidenceBundle = new HashMap<>();
                    evidenceBundle.put(CONTEXT_KEY_NODE_ID, node.get(FIELD_ID));
                    evidenceBundle.put(CONTEXT_KEY_NODE_NAME, nodeData.getOrDefault(FIELD_LABEL, DEFAULT_APPROVAL_LABEL));
                    evidenceBundle.put(CONTEXT_KEY_APPROVER_IDS, approverIdStrings);
                    evidenceBundle.put(CONTEXT_KEY_REQUIRED_APPROVALS, structuredApprovers.size());
                    evidenceBundle.put("approvalGroupId", approvalGroupId.toString());
                    evidenceBundle.put("targetUserId", pair.get("userId"));
                    evidenceBundle.put("targetUserName", pair.get("userName")); // For frontend display
                    evidenceBundle.put("targetUserDisplayName", pair.get("userName")); // For frontend display
                    evidenceBundle.put("currentUserName", pair.get("userName")); // For notification context
                    
                    WorkflowApproval approval = approvalService.createApprovalRequest(
                        execution.getId(),
                        null,
                        approverId,
                        approvalType,
                        approvalContext,
                        evidenceBundle,
                        timeoutMinutes,
                        approvalGroupId,
                        sendEmail,
                        emailSubject,
                        emailBody,
                        approvalPolicy,
                        requiredApprovals
                    );
                    
                    approvalIds.add(approval.getId().toString());
                    
                    logger.debug()
                        .message("Created structured approval for user-manager pair")
                        .category("approval")
                        .detail("approverId", approverId.toString())
                        .detail("targetUserId", pair.get("userId"))
                        .detail("approvalId", approval.getId().toString())
                        .log();
                        
                } catch (IllegalArgumentException e) {
                    logger.error()
                        .message("Invalid manager ID in structured approvers")
                        .category("approval")
                        .detail("pair", pair.toString())
                        .detail("error", e.getMessage())
                        .log();
                    throw e;
                }
            }
        } else {
            // Check if this is a PER-USER approval scenario (role owners)
            // If userIds exists in context, create one approval per user per owner
            
            // CRITICAL: Use approvedUserIds from previous approval node if available
            // This ensures chained approvals only process approved users
            @SuppressWarnings("unchecked")
            List<String> approvedUserIds = (List<String>) context.get("approvedUserIds");
            
            @SuppressWarnings("unchecked")
            List<String> contextUserIds = approvedUserIds != null && !approvedUserIds.isEmpty() 
                ? approvedUserIds  // Use approved users from previous approval node
                : (List<String>) context.get("userIds");  // Fall back to original userIds for first approval
            
            // Extract user details if available (contains userId + userName)
            @SuppressWarnings("unchecked")
            List<Map<String, String>> userDetails = (List<Map<String, String>>) context.get("userDetails");
            
            // Extract managers array from Access Portal payload (contains userId + userName + managerId)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> managers = (List<Map<String, Object>>) context.get("managers");
            
            if (approvalType == WorkflowApproval.ApproverType.ROLE_OWNER && 
                contextUserIds != null && !contextUserIds.isEmpty()) {
                
                // Log which user list is being used (important for debugging chained approvals)
                if (approvedUserIds != null && !approvedUserIds.isEmpty()) {
                    logger.info()
                        .message("Using approvedUserIds from previous approval node")
                        .category("approval")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .detail("approvedUserCount", approvedUserIds.size())
                        .detail("reason", "chained_approval_filtering")
                        .log();
                }
                
                // PER-USER APPROVALS: Loop over each user and create approvals for all owners
                logger.info()
                    .message("Creating per-user approvals for role owners")
                    .category("approval")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("userCount", contextUserIds.size())
                    .detail("ownerCount", approverIdStrings.size())
                    .detail("totalApprovals", contextUserIds.size() * approverIdStrings.size())
                    .detail("hasUserDetails", userDetails != null)
                    .detail("hasManagers", managers != null)
                    .detail("usingApprovedUsersOnly", approvedUserIds != null && !approvedUserIds.isEmpty())
                    .log();
                
                // Build username lookup from managers array (Access Portal payload)
                // This provides userId -> userName mapping for all users in the workflow
                Map<String, String> userNameLookup = new HashMap<>();
                if (managers != null) {
                    for (Map<String, Object> manager : managers) {
                        Object userIdObj = manager.get("userId");
                        Object userNameObj = manager.get("userName");
                        if (userIdObj != null && userNameObj != null) {
                            userNameLookup.put(userIdObj.toString(), userNameObj.toString());
                        }
                    }
                }
                
                // Create a map for quick user details lookup (fallback for additional user info)
                // IMPORTANT: Only include details for users in contextUserIds (filtered by approval chain)
                Map<String, Map<String, String>> userDetailsLookup = new HashMap<>();
                if (userDetails != null) {
                    for (Map<String, String> detail : userDetails) {
                        String userId = detail.get("userId");
                        // Only include user details for users in the current context (approved users)
                        if (userId != null && contextUserIds.contains(userId)) {
                            userDetailsLookup.put(userId, detail);
                        }
                    }
                }
                
                for (String userId : contextUserIds) {
                    // Look up username from managers array first (Access Portal payload)
                    String userName = userNameLookup.get(userId);
                    
                    // Fallback to userDetails if managers doesn't have it
                    Map<String, String> userDetail = userDetailsLookup.get(userId);
                    
                    for (String ownerIdStr : approverIdStrings) {
                        try {
                            UUID ownerId = UUID.fromString(ownerIdStr);
                            
                            // Build evidence bundle with user context
                            Map<String, Object> evidenceBundle = new HashMap<>();
                            evidenceBundle.put(CONTEXT_KEY_NODE_ID, node.get(FIELD_ID));
                            evidenceBundle.put(CONTEXT_KEY_NODE_NAME, nodeData.getOrDefault(FIELD_LABEL, DEFAULT_APPROVAL_LABEL));
                            evidenceBundle.put(CONTEXT_KEY_APPROVER_IDS, approverIdStrings);
                            evidenceBundle.put(CONTEXT_KEY_REQUIRED_APPROVALS, contextUserIds.size() * approverIdStrings.size());
                            evidenceBundle.put("approvalGroupId", approvalGroupId.toString());
                            evidenceBundle.put("targetUserId", userId); // Key for per-user tracking
                            
                            // Priority 1: Use userName from managers array (Access Portal)
                            if (userName != null && !userName.isEmpty()) {
                                evidenceBundle.put("targetUserName", userName);
                                evidenceBundle.put("targetUserDisplayName", userName);
                                evidenceBundle.put("currentUserName", userName); // For notification context compatibility
                                // Add additional details from userDetails if available
                                if (userDetail != null) {
                                    evidenceBundle.put("targetUserEmail", userDetail.get("email"));
                                    evidenceBundle.put("targetUserFirstName", userDetail.get("firstName"));
                                    evidenceBundle.put("targetUserLastName", userDetail.get("lastName"));
                                }
                            }
                            // Priority 2: Fallback to userDetails if available
                            else if (userDetail != null) {
                                evidenceBundle.put("targetUserName", userDetail.get("userName"));
                                evidenceBundle.put("currentUserName", userDetail.get("userName")); // For notification context compatibility
                                evidenceBundle.put("targetUserEmail", userDetail.get("email"));
                                evidenceBundle.put("targetUserFirstName", userDetail.get("firstName"));
                                evidenceBundle.put("targetUserLastName", userDetail.get("lastName"));
                                evidenceBundle.put("targetUserDisplayName", userDetail.get("displayName"));
                            }
                            // Priority 3: Fallback to short user ID when neither source available
                            else {
                                String shortUserId = userId.length() > 8 ? userId.substring(userId.length() - 8) : userId;
                                evidenceBundle.put("targetUserName", "User " + shortUserId);
                                evidenceBundle.put("targetUserDisplayName", "User " + shortUserId);
                                evidenceBundle.put("currentUserName", "User " + shortUserId); // For notification context compatibility
                            }
                            
                            WorkflowApproval approval = approvalService.createApprovalRequest(
                                execution.getId(),
                                null, // nodeExecutionId will be set by caller
                                ownerId,
                                approvalType,
                                context, // Pass full execution context
                                evidenceBundle,
                                timeoutMinutes,
                                approvalGroupId,
                                sendEmail,
                                emailSubject,
                                emailBody,
                                approvalPolicy,
                                requiredApprovals
                            );
                            
                            approvalIds.add(approval.getId().toString());
                            
                            logger.debug()
                                .message("Created per-user approval for role owner")
                                .category("approval")
                                .detail("ownerId", ownerIdStr)
                                .detail("targetUserId", userId)
                                .detail("targetUserName", userName != null ? userName : (userDetail != null ? userDetail.get("userName") : "N/A"))
                                .detail("targetUserEmail", userDetail != null ? userDetail.get("email") : "N/A")
                                .detail("userNameSource", userName != null ? "managers_array" : (userDetail != null ? "userDetails_array" : "fallback"))
                                .detail("approvalId", approval.getId().toString())
                                .log();
                                
                        } catch (IllegalArgumentException e) {
                            logger.error()
                                .message("Invalid owner ID in per-user approval creation")
                                .category("approval")
                                .objectType("WorkflowNode")
                                .objectId((String) node.get(FIELD_ID))
                                .detail("invalidOwnerId", ownerIdStr)
                                .detail("targetUserId", userId)
                                .detail("error", e.getMessage())
                                .log();
                            throw e;
                        }
                    }
                }
                
            } else {
                // LEGACY: Create one approval per approver ID (no user context)
                for (String approverIdStr : approverIdStrings) {
                    try {
                        UUID approverId = UUID.fromString(approverIdStr);
                        
                        WorkflowApproval approval = approvalService.createApprovalRequest(
                            execution.getId(),
                            null, // nodeExecutionId will be set by caller
                            approverId,
                            approvalType, // Use dynamic approvalType (SPECIFIC or ROLE_OWNER)
                            context, // Pass execution context as approval context
                            Map.of(
                                CONTEXT_KEY_NODE_ID, node.get(FIELD_ID),
                                CONTEXT_KEY_NODE_NAME, nodeData.getOrDefault(FIELD_LABEL, DEFAULT_APPROVAL_LABEL),
                                CONTEXT_KEY_APPROVER_IDS, approverIdStrings,
                                CONTEXT_KEY_REQUIRED_APPROVALS, approverIdStrings.size(),
                                "approvalGroupId", approvalGroupId.toString()
                            ),
                            timeoutMinutes,
                            approvalGroupId,  // Phase 7: Set group ID
                            sendEmail,
                            emailSubject,
                            emailBody,
                            approvalPolicy,  // Phase 1: Set approval policy
                            requiredApprovals  // Phase 1: Set threshold count
                        );
                        
                        approvalIds.add(approval.getId().toString());
                    } catch (IllegalArgumentException e) {
                        logger.error()
                            .message(LOG_INVALID_APPROVER_ID_MULTI_APPROVAL)
                            .category("approval")
                            .objectType("WorkflowNode")
                            .objectId((String) node.get(FIELD_ID))
                            .detail("invalidApproverId", approverIdStr)
                            .detail("error", e.getMessage())
                            .log();
                        throw e;
                    }
                }
            }
        } // End of else block for legacy approvers
        
        logger.info()
            .message(LOG_MULTI_APPROVAL_REQUESTS_CREATED)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("approvalCount", approvalIds.size())
            .detail("approvalGroupId", approvalGroupId.toString())
            .log();
        
        // Return pause signal to WorkflowExecutor
        return Map.of(
            CONTEXT_KEY_PAUSE_REQUIRED, true,
            CONTEXT_KEY_REASON, "Waiting for all approvals",
            CONTEXT_KEY_APPROVAL_IDS, approvalIds,
            CONTEXT_KEY_REQUIRED_APPROVALS, approverIdStrings.size(),
            CONTEXT_KEY_APPROVER_IDS, approverIdStrings,
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_APPROVAL_MULTI,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute approval.dynamic node (dynamic approver selection)
     * Phase 6b: Implements dynamic approver resolution using ApproverResolver
     */
    private Map<String, Object> executeApprovalDynamic(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_DYNAMIC_APPROVAL_NODE)
            .category("approval")
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("executionId", execution.getId().toString())
            .log();
        
        // Extract node configuration
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_NODE_DATA_MISSING);
        }
        
        // Get resolution strategy
        String strategy = (String) nodeData.get(FIELD_RESOLUTION_STRATEGY);
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException(ERROR_APPROVER_STRATEGY_UNKNOWN + strategy);
        }
        
        // Resolve approver dynamically
        UUID approverId;
        try {
            approverId = approverResolver.resolveApprover(strategy, context);
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_RESOLVE_DYNAMIC_APPROVER)
                .category("approval")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("strategy", strategy)
                .detail("error", e.getMessage())
                .log();
            throw e;
        }
        
        // Get optional timeout (default 24 hours)
        Integer timeoutMinutes = nodeData.get(FIELD_TIMEOUT_MINUTES) != null ? 
            (Integer) nodeData.get(FIELD_TIMEOUT_MINUTES) : DEFAULT_APPROVAL_TIMEOUT_MINUTES;
        
        // Get optional email configuration
        Boolean sendEmail = nodeData.get("sendEmail") != null ? 
            (Boolean) nodeData.get("sendEmail") : false;
        String emailSubject = (String) nodeData.get("emailSubject");
        String emailBody = (String) nodeData.get("emailBody");
        
        // Create approval request with resolved approver
        WorkflowApproval approval = approvalService.createApprovalRequest(
            execution.getId(),
            null, // nodeExecutionId will be set by caller
            approverId,
            WorkflowApproval.ApproverType.DYNAMIC,
            context, // Pass execution context as approval context
            Map.of(
                CONTEXT_KEY_NODE_ID, node.get(FIELD_ID),
                CONTEXT_KEY_NODE_NAME, nodeData.getOrDefault(FIELD_LABEL, DEFAULT_APPROVAL_LABEL),
                CONTEXT_KEY_RESOLUTION_STRATEGY, strategy,
                CONTEXT_KEY_RESOLVED_APPROVER_ID, approverId.toString()
            ),
            timeoutMinutes,
            null, // No approval group for dynamic approval
            sendEmail,
            emailSubject,
            emailBody
        );
        
        logger.info()
            .message(LOG_CREATED_APPROVAL_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType("ApprovalRequest")
            .objectId(approval.getId().toString())
            .detail("approverId", approverId.toString())
            .log();
        
        // Return pause signal to WorkflowExecutor
        return Map.of(
            CONTEXT_KEY_PAUSE_REQUIRED, true,
            CONTEXT_KEY_REASON, "Waiting for approval (dynamically resolved)",
            CONTEXT_KEY_APPROVAL_ID, approval.getId().toString(),
            CONTEXT_KEY_RESOLVED_APPROVER_ID, approverId.toString(),
            CONTEXT_KEY_RESOLUTION_STRATEGY, strategy,
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_APPROVAL_DYNAMIC,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute condition.if node
     */
    private Map<String, Object> executeConditionIf(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_IF_ELSE_CONDITION_NODE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
            
            if (nodeData == null) {
                logger.warn()
                    .message(LOG_NO_CONDITION_DATA_DEFAULT_FALSE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .log();
                return buildConditionResult(false, "No condition configured");
            }
            
            // Get condition configuration
            String field = (String) nodeData.get("field");
            String operator = (String) nodeData.get("operator");
            Object expectedValue = nodeData.get("value");
            
            if (field == null || operator == null) {
                logger.warn()
                    .message(LOG_MISSING_FIELD_OR_OPERATOR_DEFAULT_FALSE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .log();
                return buildConditionResult(false, "Missing condition configuration");
            }
            
            // Get actual value from context using dot notation
            Object actualValue = getNestedValue(context, field);
            
            // Evaluate condition
            boolean conditionMet = evaluateCondition(actualValue, operator, expectedValue);
            
            logger.info()
                .message(LOG_CONDITION_EVALUATION)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("field", field)
                .detail("operator", operator)
                .detail("expected", String.valueOf(expectedValue))
                .detail("actual", String.valueOf(actualValue))
                .detail("result", String.valueOf(conditionMet))
                .log();
            
            return buildConditionResult(conditionMet, 
                String.format("Condition: %s %s %s = %s", field, operator, expectedValue, conditionMet));
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_ERROR_EVALUATING_CONDITION)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Condition evaluation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build condition result map
     */
    private Map<String, Object> buildConditionResult(boolean conditionMet, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONDITION_IF);
        result.put(CONTEXT_KEY_EXECUTED, true);
        result.put(CONTEXT_KEY_CONDITION_MET, conditionMet);
        result.put("evaluationMessage", message);
        result.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        return result;
    }
    
    /**
     * Evaluate condition based on operator
     * Supports: equals, notEquals, contains, greaterThan, lessThan, greaterOrEqual, lessOrEqual, 
     *           in, notIn, exists, notExists, startsWith, endsWith, isEmpty, isNotEmpty
     */
    private boolean evaluateCondition(Object actualValue, String operator, Object expectedValue) {
        switch (operator.toLowerCase()) {
            case "equals":
            case "==":
                return compareValues(actualValue, expectedValue) == 0;
                
            case "notequals":
            case "!=":
                return compareValues(actualValue, expectedValue) != 0;
                
            case "contains":
                if (actualValue == null) return false;
                return actualValue.toString().contains(expectedValue.toString());
                
            case "notcontains":
                if (actualValue == null) return true;
                return !actualValue.toString().contains(expectedValue.toString());
                
            case "startswith":
                if (actualValue == null) return false;
                return actualValue.toString().startsWith(expectedValue.toString());
                
            case "endswith":
                if (actualValue == null) return false;
                return actualValue.toString().endsWith(expectedValue.toString());
                
            case "greaterthan":
            case ">":
                return compareValues(actualValue, expectedValue) > 0;
                
            case "lessthan":
            case "<":
                return compareValues(actualValue, expectedValue) < 0;
                
            case "greaterorequal":
            case ">=":
                return compareValues(actualValue, expectedValue) >= 0;
                
            case "lessorequal":
            case "<=":
                return compareValues(actualValue, expectedValue) <= 0;
                
            case "in":
                if (expectedValue instanceof List) {
                    return ((List<?>) expectedValue).contains(actualValue);
                }
                return false;
                
            case "notin":
                if (expectedValue instanceof List) {
                    return !((List<?>) expectedValue).contains(actualValue);
                }
                return true;
                
            case "exists":
                return actualValue != null;
                
            case "notexists":
                return actualValue == null;
                
            case "isempty":
                if (actualValue == null) return true;
                if (actualValue instanceof String) return ((String) actualValue).isEmpty();
                if (actualValue instanceof List) return ((List<?>) actualValue).isEmpty();
                if (actualValue instanceof Map) return ((Map<?, ?>) actualValue).isEmpty();
                return false;
                
            case "isnotempty":
                if (actualValue == null) return false;
                if (actualValue instanceof String) return !((String) actualValue).isEmpty();
                if (actualValue instanceof List) return !((List<?>) actualValue).isEmpty();
                if (actualValue instanceof Map) return !((Map<?, ?>) actualValue).isEmpty();
                return true;
                
            default:
                logger.warn()
                    .message(LOG_UNKNOWN_OPERATOR_DEFAULT_EQUALS)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("operator", operator)
                    .log();
                return compareValues(actualValue, expectedValue) == 0;
        }
    }
    
    /**
     * Compare two values (type-safe)
     * Returns: -1 (less), 0 (equal), 1 (greater)
     */
    private int compareValues(Object actualValue, Object expectedValue) {
        // Null handling
        if (actualValue == null && expectedValue == null) return 0;
        if (actualValue == null) return -1;
        if (expectedValue == null) return 1;
        
        // String comparison
        String actualStr = actualValue.toString();
        String expectedStr = expectedValue.toString();
        
        // Try numeric comparison if both are numbers
        try {
            double actualNum = Double.parseDouble(actualStr);
            double expectedNum = Double.parseDouble(expectedStr);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return actualStr.compareTo(expectedStr);
        }
    }

    /**
     * Execute condition.switch node
     * Evaluates a value against multiple cases and selects the matching branch.
     * 
     * Configuration:
     * - switchExpression: Path to value in context (e.g., "user.department", "request.type")
     * - cases: Array of {value, label} for each case to match
     * - defaultCase: Label of default branch if no case matches
     * - caseSensitive: Whether string comparisons are case-sensitive (default: false)
     * 
     * Output:
     * - selectedBranch: The label of the matched case (used for edge routing)
     * - matchedValue: The value that was matched
     * - switchValue: The actual value from the expression
     */
    private Map<String, Object> executeConditionSwitch(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        String nodeId = (String) node.get(FIELD_ID);
        
        logger.info()
            .message(LOG_EXECUTING_SWITCH_CONDITION_NODE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .log();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
            
            if (nodeData == null) {
                logger.warn()
                    .message("Switch node has no configuration, using default branch")
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectId(nodeId)
                    .log();
                return buildSwitchResult("default", null, null, "No switch configuration");
            }
            
            // Get switch configuration
            String switchExpression = (String) nodeData.get("switchExpression");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cases = (List<Map<String, Object>>) nodeData.get("cases");
            String defaultCase = (String) nodeData.getOrDefault("defaultCase", "default");
            boolean caseSensitive = Boolean.TRUE.equals(nodeData.get("caseSensitive"));
            
            if (switchExpression == null || switchExpression.isBlank()) {
                logger.warn()
                    .message("Switch expression is empty, using default branch")
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectId(nodeId)
                    .log();
                return buildSwitchResult(defaultCase, null, null, "Empty switch expression");
            }
            
            // Get the value to switch on from context
            Object switchValue = getNestedValue(context, switchExpression);
            
            logger.debug()
                .message("Evaluating switch expression")
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectId(nodeId)
                .detail("expression", switchExpression)
                .detail("value", switchValue != null ? switchValue.toString() : "null")
                .log();
            
            // If no cases defined, use default
            if (cases == null || cases.isEmpty()) {
                logger.info()
                    .message("No switch cases defined, using default branch")
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectId(nodeId)
                    .log();
                return buildSwitchResult(defaultCase, switchValue, null, "No cases defined");
            }
            
            // Evaluate each case
            String matchedBranch = null;
            Object matchedValue = null;
            
            for (Map<String, Object> switchCase : cases) {
                Object caseValue = switchCase.get("value");
                String caseLabel = (String) switchCase.get("label");
                String caseOperator = (String) switchCase.getOrDefault("operator", "equals");
                
                if (caseLabel == null || caseLabel.isBlank()) {
                    continue; // Skip cases without labels
                }
                
                boolean matches = evaluateSwitchCase(switchValue, caseValue, caseOperator, caseSensitive);
                
                if (matches) {
                    matchedBranch = caseLabel;
                    matchedValue = caseValue;
                    
                    logger.info()
                        .message("Switch case matched")
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectId(nodeId)
                        .detail("caseLabel", caseLabel)
                        .detail("caseValue", caseValue != null ? caseValue.toString() : "null")
                        .detail("operator", caseOperator)
                        .log();
                    break;
                }
            }
            
            // Use default if no match
            if (matchedBranch == null) {
                matchedBranch = defaultCase;
                
                logger.info()
                    .message("No switch case matched, using default")
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectId(nodeId)
                    .detail("defaultCase", defaultCase)
                    .detail("switchValue", switchValue != null ? switchValue.toString() : "null")
                    .log();
            }
            
            return buildSwitchResult(matchedBranch, switchValue, matchedValue, 
                "Matched: " + matchedBranch + " for value: " + switchValue);
            
        } catch (Exception e) {
            logger.error()
                .message("Error executing switch node")
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectId(nodeId)
                .detail("error", e.getMessage())
                .log();
            
            // On error, use default branch
            String defaultCase = "default";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
                if (nodeData != null) {
                    defaultCase = (String) nodeData.getOrDefault("defaultCase", "default");
                }
            } catch (Exception ignored) {}
            
            return buildSwitchResult(defaultCase, null, null, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Build switch result map
     */
    private Map<String, Object> buildSwitchResult(String selectedBranch, Object switchValue, Object matchedValue, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONDITION_SWITCH);
        result.put(CONTEXT_KEY_EXECUTED, true);
        result.put(CONTEXT_KEY_SELECTED_BRANCH, selectedBranch);
        result.put("switchValue", switchValue);
        result.put("matchedValue", matchedValue);
        result.put("evaluationMessage", message);
        result.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        return result;
    }
    
    /**
     * Evaluate if a switch value matches a case value
     * Supports multiple operators: equals, contains, startsWith, endsWith, regex, in, range
     */
    private boolean evaluateSwitchCase(Object switchValue, Object caseValue, String operator, boolean caseSensitive) {
        if (switchValue == null) {
            // null only matches explicit null case
            return caseValue == null || "null".equals(String.valueOf(caseValue));
        }
        
        if (caseValue == null) {
            return false;
        }
        
        String switchStr = switchValue.toString();
        String caseStr = caseValue.toString();
        
        if (!caseSensitive && operator != null && !operator.equals("regex")) {
            switchStr = switchStr.toLowerCase();
            caseStr = caseStr.toLowerCase();
        }
        
        switch (operator != null ? operator.toLowerCase() : "equals") {
            case "equals":
            case "==":
                // Try numeric comparison first
                try {
                    double switchNum = Double.parseDouble(switchValue.toString());
                    double caseNum = Double.parseDouble(caseValue.toString());
                    return switchNum == caseNum;
                } catch (NumberFormatException e) {
                    return switchStr.equals(caseStr);
                }
                
            case "notequals":
            case "!=":
                return !switchStr.equals(caseStr);
                
            case "contains":
                return switchStr.contains(caseStr);
                
            case "startswith":
                return switchStr.startsWith(caseStr);
                
            case "endswith":
                return switchStr.endsWith(caseStr);
                
            case "regex":
                try {
                    return switchValue.toString().matches(caseValue.toString());
                } catch (Exception e) {
                    return false;
                }
                
            case "in":
                // Case value should be comma-separated list
                String[] values = caseStr.split(",");
                for (String v : values) {
                    if (switchStr.equals(v.trim())) {
                        return true;
                    }
                }
                return false;
                
            case "range":
                // Case value should be "min-max" format
                try {
                    String[] range = caseValue.toString().split("-");
                    if (range.length == 2) {
                        double num = Double.parseDouble(switchValue.toString());
                        double min = Double.parseDouble(range[0].trim());
                        double max = Double.parseDouble(range[1].trim());
                        return num >= min && num <= max;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                return false;
                
            case "greaterthan":
            case ">":
                try {
                    return Double.parseDouble(switchValue.toString()) > Double.parseDouble(caseValue.toString());
                } catch (NumberFormatException e) {
                    return switchStr.compareTo(caseStr) > 0;
                }
                
            case "lessthan":
            case "<":
                try {
                    return Double.parseDouble(switchValue.toString()) < Double.parseDouble(caseValue.toString());
                } catch (NumberFormatException e) {
                    return switchStr.compareTo(caseStr) < 0;
                }
                
            default:
                return switchStr.equals(caseStr);
        }
    }

    /**
     * Execute action.provision node (provision access/role)
     * Now with real gRPC integration to create identities
     */
    private Map<String, Object> executeActionProvision(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_PROVISION_ACTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        Boolean simulationMode = execution.getExecutionContext() != null ? 
            (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        if (Boolean.TRUE.equals(simulationMode)) {
            logger.info()
                .message(LOG_SIMULATION_MODE_SKIPPING_PROVISIONING)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .log();
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                CONTEXT_KEY_EXECUTED, true,
                CONTEXT_KEY_SIMULATED, true,
                CONTEXT_KEY_STATUS, DEFAULT_STATUS_WOULD_PROVISION,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        try {
            // Extract provisioning parameters from node data and context
            String actionType = nodeData != null ? (String) nodeData.get(FIELD_ACTION) : null;
            
            if (ACTION_TYPE_CREATE_USER.equals(actionType) || ACTION_TYPE_CREATE_EMPLOYEE.equals(actionType)) {
                return executeCreateEmployee(nodeData, context, execution);
            } else {
                logger.warn()
                    .message(LOG_UNKNOWN_PROVISION_ACTION_TYPE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("actionType", actionType)
                    .log();
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                    CONTEXT_KEY_EXECUTED, true,
                    "provisioned", true,
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_ERROR_EXECUTING_PROVISION_ACTION)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Provision action failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates an employee identity via REST API
     */
    private Map<String, Object> executeCreateEmployee(Map<String, Object> nodeData, Map<String, Object> context, WorkflowExecution execution) {
        // Resolve variables from context
        String tenantId = resolveVariable(CONTEXT_KEY_TENANT_ID, nodeData, context, execution);
        String username = resolveVariable("username", nodeData, context, execution);
        String email = resolveVariable("email", nodeData, context, execution);
        String firstName = resolveVariable("firstName", nodeData, context, execution);
        String lastName = resolveVariable("lastName", nodeData, context, execution);
        String employeeId = resolveVariable("employeeId", nodeData, context, execution);
        String department = resolveVariable("department", nodeData, context, execution);
        String jobTitle = resolveVariable("jobTitle", nodeData, context, execution);
        String managerId = resolveVariable("managerId", nodeData, context, execution);
        String hireDateStr = resolveVariable("hireDate", nodeData, context, execution);
        
        // Parse hire date if provided
        LocalDate hireDate = null;
        if (hireDateStr != null) {
            try {
                hireDate = LocalDate.parse(hireDateStr);
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_TO_PARSE_HIRE_DATE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("hireDateStr", hireDateStr)
                    .log();
            }
        }
        
        // Generate username from email if not provided
        if (username == null && email != null) {
            username = email.split("@")[0];
        }
        
        // Generate employee ID if not provided
        if (employeeId == null) {
            employeeId = "EMP" + System.currentTimeMillis();
        }
        
        logger.info()
            .message(LOG_CREATING_EMPLOYEE_VIA_REST)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("username", username)
            .detail("email", email)
            .detail("department", department)
            .log();
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalStateException("TenantId is required for employee creation but was not found in execution context");
        }
        
        try {
            String identityId = identityRestClient.createEmployee(
                tenantId,
                username,
                email,
                firstName != null ? firstName : "Unknown",
                lastName != null ? lastName : "",
                employeeId,
                department,
                jobTitle,
                managerId,
                hireDate
            );
            
            logger.info()
                .message(LOG_EMPLOYEE_CREATED_WITH_ID)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("Employee")
                .objectId(identityId)
                .log();
            
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                CONTEXT_KEY_EXECUTED, true,
                FIELD_ACTION, ACTION_TYPE_CREATE_EMPLOYEE,
                "identityId", identityId,
                "success", true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_CREATE_EMPLOYEE_REST)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to create employee: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes an employee identity via gRPC
     */
    private Map<String, Object> executeDeleteEmployee(Map<String, Object> nodeData, Map<String, Object> context, WorkflowExecution execution) {
        String identityId = resolveVariable("identityId", nodeData, context, execution);
        String tenantId = resolveVariable(CONTEXT_KEY_TENANT_ID, nodeData, context, execution);
        
        if (identityId == null) {
            logger.error()
                .message(LOG_CANNOT_DELETE_EMPLOYEE_NULL_ID)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .log();
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                CONTEXT_KEY_EXECUTED, false,
                FIELD_ACTION, "delete_employee",
                CONTEXT_KEY_ERROR, "Missing identityId",
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        if (tenantId == null || tenantId.isEmpty()) {
            logger.error()
                .message(LOG_CANNOT_DELETE_EMPLOYEE_NULL_TENANT)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .log();
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                CONTEXT_KEY_EXECUTED, false,
                FIELD_ACTION, "delete_employee",
                CONTEXT_KEY_ERROR, "Missing tenantId - authentication required",
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        try {
            identityRestClient.deleteIdentity(identityId, tenantId);
            logger.info()
                .message(LOG_EMPLOYEE_DELETED_SUCCESSFULLY)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("Employee")
                .objectId(identityId)
                .detail("tenantId", tenantId)
                .log();
            
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_PROVISION,
                CONTEXT_KEY_EXECUTED, true,
                FIELD_ACTION, "delete_employee",
                "identityId", identityId,
                "success", true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_DELETE_EMPLOYEE_GRPC)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to delete employee: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resolves a variable from node data, context, or execution context
     */
    private String resolveVariable(String variableName, Map<String, Object> nodeData, Map<String, Object> context, WorkflowExecution execution) {
        // Try node data first
        if (nodeData != null && nodeData.containsKey(variableName)) {
            Object value = nodeData.get(variableName);
            return value != null ? value.toString() : null;
        }
        
        // Try workflow context
        if (context != null && context.containsKey(variableName)) {
            Object value = context.get(variableName);
            return value != null ? value.toString() : null;
        }
        
        // Try execution context
        if (execution.getExecutionContext() != null && execution.getExecutionContext().containsKey(variableName)) {
            Object value = execution.getExecutionContext().get(variableName);
            return value != null ? value.toString() : null;
        }
        
        return null;
    }

    /**
     * Execute action.deprovision node
     * Handles employee termination/deletion via gRPC
     */
    private Map<String, Object> executeActionDeprovision(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_DEPROVISION_ACTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        Boolean simulationMode = execution.getExecutionContext() != null ? 
            (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        if (Boolean.TRUE.equals(simulationMode)) {
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_DEPROVISION,
                CONTEXT_KEY_EXECUTED, true,
                CONTEXT_KEY_SIMULATED, true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        try {
            // Get identityId to delete
            String identityId = resolveVariable("identityId", nodeData, context, execution);
            String tenantId = resolveVariable(CONTEXT_KEY_TENANT_ID, nodeData, context, execution);
            
            if (identityId == null) {
                logger.error()
                    .message(LOG_CANNOT_DEPROVISION_NULL_ID)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .log();
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_DEPROVISION,
                    CONTEXT_KEY_EXECUTED, false,
                    CONTEXT_KEY_ERROR, "Missing identityId",
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
            if (tenantId == null || tenantId.isEmpty()) {
                logger.error()
                    .message(LOG_CANNOT_DEPROVISION_NULL_TENANT)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .log();
                throw new IllegalArgumentException("Missing tenantId - authentication required");
            }
            
            // Delete identity via gRPC
            identityGrpcClient.deleteIdentity(identityId, tenantId);
            logger.info()
                .message(LOG_IDENTITY_DEPROVISIONED_SUCCESSFULLY)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("Identity")
                .objectId(identityId)
                .detail("tenantId", tenantId)
                .log();
            
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_DEPROVISION,
                CONTEXT_KEY_EXECUTED, true,
                "identityId", identityId,
                "success", true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_DEPROVISION_IDENTITY)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            // Re-throw the exception so the workflow marks this node as FAILED
            throw new RuntimeException("Failed to deprovision identity: " + e.getMessage(), e);
        }
    }

    /**
     * Execute action.notify node
     * Now with real Kafka-based notifications via core-service
     * Supports simulation mode
     */
    private Map<String, Object> executeActionNotify(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_NOTIFY_ACTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        // Check simulation mode
        Boolean simulationMode = execution.getExecutionContext() != null ? 
            (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        
        try {
            String notificationType = nodeData != null ? (String) nodeData.get(CONTEXT_KEY_NOTIFICATION_TYPE) : NOTIFICATION_TYPE_WORKFLOW_UPDATE;
            String recipientUserId = resolveVariable("recipientUserId", nodeData, context, execution);
            String recipientEmail = resolveVariable("recipientEmail", nodeData, context, execution);
            String recipientName = resolveVariable("recipientName", nodeData, context, execution);
            String tenantId = resolveVariable(CONTEXT_KEY_TENANT_ID, nodeData, context, execution);
            String workflowName = resolveVariable(CONTEXT_KEY_WORKFLOW_NAME, nodeData, context, execution);
            String workflowInstanceId = execution.getId() != null ? execution.getId().toString() : "unknown";
            
            // Simulation mode: skip actual notification
            if (Boolean.TRUE.equals(simulationMode)) {
                logger.info()
                    .message("Simulation mode: skipping notification send")
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("recipientUserId", recipientUserId)
                    .detail("recipientEmail", recipientEmail)
                    .detail("notificationType", notificationType)
                    .log();
                
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_NOTIFY,
                    CONTEXT_KEY_EXECUTED, true,
                    CONTEXT_KEY_SIMULATED, true,
                    "wouldNotify", true,
                    "recipientUserId", recipientUserId != null ? recipientUserId : "unknown",
                    "recipientEmail", recipientEmail != null ? recipientEmail : "unknown",
                    CONTEXT_KEY_NOTIFICATION_TYPE, notificationType,
                    CONTEXT_KEY_STATUS, "would-send-notification",
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalStateException("TenantId is required for notification but was not found in execution context");
            }
            
            if (recipientUserId != null && recipientEmail != null) {
                notificationClient.sendWorkflowCompletedNotification(
                    tenantId,
                    recipientUserId,
                    recipientEmail,
                    recipientName != null ? recipientName : "User",
                    workflowName != null ? workflowName : DEFAULT_WORKFLOW_NAME,
                    workflowInstanceId
                );
                
                logger.info()
                    .message(LOG_NOTIFICATION_SENT_KAFKA)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("recipientUserId", recipientUserId)
                    .detail("notificationType", notificationType)
                    .log();
                
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_NOTIFY,
                    CONTEXT_KEY_EXECUTED, true,
                    "notified", true,
                    "recipientUserId", recipientUserId,
                    CONTEXT_KEY_NOTIFICATION_TYPE, notificationType,
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            } else {
                logger.warn()
                    .message(LOG_NOTIFICATION_RECIPIENTS_MISSING)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .log();
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_NOTIFY,
                    CONTEXT_KEY_EXECUTED, false,
                    CONTEXT_KEY_ERROR, "Missing recipientUserId or recipientEmail",
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_SEND_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Notification action failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute action.script node (custom script)
     * Supports simulation mode
     */
    private Map<String, Object> executeActionScript(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_SCRIPT_ACTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        // Check simulation mode
        Boolean simulationMode = execution.getExecutionContext() != null ? 
            (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        
        if (Boolean.TRUE.equals(simulationMode)) {
            logger.info()
                .message("Simulation mode: skipping script execution")
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("script", String.valueOf(nodeData != null ? nodeData.get("script") : "none"))
                .log();
            
            return Map.of(
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_SCRIPT,
                CONTEXT_KEY_EXECUTED, true,
                CONTEXT_KEY_SIMULATED, true,
                "wouldExecuteScript", nodeData != null ? nodeData.get("script") : "unknown",
                CONTEXT_KEY_STATUS, "would-execute-script",
                "scriptResult", "simulated-success",
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        // In real implementation, execute script securely
        logger.info()
            .message(LOG_EXECUTING_SCRIPT_MOCK)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("script", String.valueOf(nodeData != null ? nodeData.get("script") : "none"))
            .log();
        
        return Map.of(
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_SCRIPT,
            CONTEXT_KEY_EXECUTED, true,
            "scriptResult", "success",
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute delay.wait node
     */
    private Map<String, Object> executeDelayWait(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_DELAY_WAIT_NODE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        // In real implementation, schedule continuation
        logger.info()
            .message(LOG_WAIT_DELAY_MOCK)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("delaySeconds", String.valueOf(nodeData != null ? nodeData.get("delaySeconds") : "unknown"))
            .log();
        
        return Map.of(
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_DELAY_WAIT,
            CONTEXT_KEY_EXECUTED, true,
            "delayed", true,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute delay.schedule node
     */
    private Map<String, Object> executeDelaySchedule(Map<String, Object> node, Map<String, Object> context, WorkflowExecution execution) {
        logger.info()
            .message(LOG_EXECUTING_SCHEDULE_NODE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        logger.info()
            .message(LOG_SCHEDULED_TIME_MOCK)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("scheduledTime", String.valueOf(nodeData != null ? nodeData.get("scheduledTime") : "unknown"))
            .log();
        
        return Map.of(
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_DELAY_SCHEDULE,
            CONTEXT_KEY_EXECUTED, true,
            "scheduled", true,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute gateway.parallel-split node (Phase 6b)
     * Creates multiple parallel branches that execute concurrently.
     * 
     * @param node The parallel-split node
     * @param context Execution context
     * @param execution Workflow execution
     * @return Map indicating split was created and number of branches
     */
    private Map<String, Object> executeParallelSplit(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        // Get join node ID from node configuration
        String joinNodeId = nodeData != null ? (String) nodeData.get(CONTEXT_KEY_JOIN_NODE) : null;
        
        if (joinNodeId == null || joinNodeId.isBlank()) {
            throw new IllegalArgumentException(ERROR_PARALLEL_JOIN_NODE_NOT_FOUND + nodeId);
        }
        
        // Get number of branches from node data (or determine from edges)
        Integer totalBranches = nodeData != null ? (Integer) nodeData.get(CONTEXT_KEY_BRANCHES) : 2;
        
        if (totalBranches == null || totalBranches < 2) {
            throw new IllegalArgumentException(ERROR_PARALLEL_NO_BRANCHES);
        }
        
        logger.info()
            .message(LOG_EXECUTING_PARALLEL_SPLIT)
            .category("parallel-execution")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("totalBranches", totalBranches)
            .log();
        
        // Create parallel execution tracker
        com.aidentx.workflowsservice.entity.ParallelExecution parallelExec = 
            com.aidentx.workflowsservice.entity.ParallelExecution.builder()
                .executionId(execution.getId())
                .splitNodeId(nodeId)
                .joinNodeId(joinNodeId)
                .totalBranches(totalBranches)
                .completedBranches(0)
                .createdAt(java.time.OffsetDateTime.now())
                .build();
        
        parallelExecutionRepository.save(parallelExec);
        
        logger.info()
            .message(LOG_PARALLEL_SPLIT_CREATED)
            .category("parallel-execution")
            .objectType("ParallelExecution")
            .objectId(parallelExec.getId().toString())
            .detail("splitNodeId", nodeId)
            .detail("joinNodeId", joinNodeId)
            .detail("totalBranches", totalBranches)
            .log();
        
        // Return metadata about the split (WorkflowExecutor will handle branch dispatch)
        return Map.of(
            CONTEXT_KEY_SPLIT_NODE, true,
            CONTEXT_KEY_JOIN_NODE, joinNodeId,
            CONTEXT_KEY_BRANCHES, totalBranches,
            CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_PARALLEL_SPLIT,
            CONTEXT_KEY_EXECUTED, true,
            CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
        );
    }

    /**
     * Execute gateway.parallel-join node (Phase 6b)
     * Waits for all parallel branches to complete before continuing.
     * Uses pessimistic locking to prevent race conditions.
     * Merges branch contexts when all branches complete.
     * 
     * @param node The parallel-join node
     * @param context Execution context from the current branch
     * @param execution Workflow execution
     * @return Map indicating if all branches are complete and merged context
     */
    private Map<String, Object> executeParallelJoin(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String joinNodeId = (String) node.get(FIELD_ID);
        
        logger.info()
            .message(LOG_EXECUTING_PARALLEL_JOIN)
            .category("parallel-execution")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .detail("executionId", execution.getId().toString())
            .log();
        
        // Find parallel execution record with pessimistic lock
        com.aidentx.workflowsservice.entity.ParallelExecution parallelExec = 
            parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
                execution.getId(), 
                joinNodeId
            ).orElseThrow(() -> new IllegalStateException(
                ERROR_PARALLEL_EXEC_NOT_FOUND + joinNodeId
            ));
        
        // Get branch context ID from current context
        String branchContextIdStr = (String) context.get("branchContextId");
        if (branchContextIdStr != null) {
            try {
                UUID branchContextId = UUID.fromString(branchContextIdStr);
                
                // Find and update the branch context to mark as complete
                com.aidentx.workflowsservice.entity.ParallelBranchContext branchContext = 
                    parallelBranchContextRepository.findById(branchContextId)
                        .orElseThrow(() -> new IllegalStateException(
                            "Branch context not found: " + branchContextId
                        ));
                
                // Update branch context with final execution context
                branchContext.setBranchContext(context);
                branchContext.setIsComplete(true);
                branchContext.setCompletedAt(java.time.OffsetDateTime.now());
                parallelBranchContextRepository.save(branchContext);
                
                logger.debug()
                    .message(LOG_UPDATED_BRANCH_CONTEXT_COMPLETE)
                    .category("parallel-execution")
                    .objectType("ParallelBranchContext")
                    .objectId(branchContextId.toString())
                    .log();
            } catch (Exception e) {
                logger.error()
                    .message(LOG_FAILED_UPDATE_BRANCH_CONTEXT)
                    .category("parallel-execution")
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        // Atomically increment completed branches
        parallelExec.incrementCompletedBranches();
        parallelExecutionRepository.save(parallelExec);
        
        int completed = parallelExec.getCompletedBranches();
        int total = parallelExec.getTotalBranches();
        
        logger.info()
            .message(LOG_PARALLEL_BRANCH_COMPLETED)
            .category("parallel-execution")
            .objectType("ParallelExecution")
            .objectId(parallelExec.getId().toString())
            .detail("completedBranches", completed)
            .detail("totalBranches", total)
            .log();
        
        // Check if all branches are complete
        if (!parallelExec.isComplete()) {
            // Not all branches done yet - pause here
            logger.info()
                .message(LOG_PARALLEL_WAITING_FOR_BRANCHES)
                .category("parallel-execution")
                .objectType("ParallelExecution")
                .objectId(parallelExec.getId().toString())
                .detail("completedBranches", completed)
                .detail("totalBranches", total)
                .log();
            
            return Map.of(
                CONTEXT_KEY_PAUSE_REQUIRED, true,
                CONTEXT_KEY_REASON, "Waiting for parallel branches",
                CONTEXT_KEY_COMPLETED_BRANCHES, completed,
                CONTEXT_KEY_TOTAL_BRANCHES, total,
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_PARALLEL_JOIN,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        // All branches complete - merge contexts
        logger.info()
            .message(LOG_PARALLEL_ALL_BRANCHES_COMPLETE)
            .category("parallel-execution")
            .objectType("ParallelExecution")
            .objectId(parallelExec.getId().toString())
            .detail("completedBranches", completed)
            .detail("totalBranches", total)
            .log();
        
        // Retrieve all branch contexts
        List<com.aidentx.workflowsservice.entity.ParallelBranchContext> allBranchContexts = 
            parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
                execution.getId(), 
                joinNodeId
            );
        
        // Phase 7: Check for failed branches
        long failedBranches = allBranchContexts.stream()
            .filter(b -> Boolean.FALSE.equals(b.getIsSuccessful()))
            .count();
        
        if (failedBranches > 0) {
            // Get failure policy from node config (or use default FAIL_FAST)
            Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
            String failurePolicy = nodeData != null && nodeData.containsKey(FIELD_FAILURE_POLICY)
                ? (String) nodeData.get(FIELD_FAILURE_POLICY)
                : DEFAULT_FAILURE_POLICY;
            
            logger.info()
                .message(LOG_PARALLEL_JOIN_CHECKING_FAILURES)
                .category("parallel-execution")
                .objectType("ParallelExecution")
                .objectId(parallelExec.getId().toString())
                .detail("totalBranches", allBranchContexts.size())
                .detail("failedBranches", failedBranches)
                .detail("failurePolicy", failurePolicy)
                .log();
            
            if ("FAIL_FAST".equals(failurePolicy)) {
                // Collect error messages from failed branches
                String errorMessages = allBranchContexts.stream()
                    .filter(b -> Boolean.FALSE.equals(b.getIsSuccessful()))
                    .map(b -> String.format("Branch %d: %s", b.getBranchIndex(), b.getErrorMessage()))
                    .collect(java.util.stream.Collectors.joining("; "));
                
                throw new RuntimeException(ERROR_PARALLEL_BRANCH_FAILED + ": " + errorMessages);
                
            } else if ("THRESHOLD".equals(failurePolicy)) {
                // Check if failure rate exceeds threshold
                double failureRate = (double) failedBranches / allBranchContexts.size();
                double threshold = nodeData != null && nodeData.containsKey(FIELD_FAILURE_THRESHOLD)
                    ? ((Number) nodeData.get(FIELD_FAILURE_THRESHOLD)).doubleValue()
                    : DEFAULT_FAILURE_THRESHOLD;
                
                if (failureRate > threshold) {
                    logger.error()
                        .message(LOG_PARALLEL_JOIN_FAILURE_THRESHOLD_EXCEEDED)
                        .category("parallel-execution")
                        .objectType("ParallelExecution")
                        .objectId(parallelExec.getId().toString())
                        .detail("failureRatePercent", (int)(failureRate * 100))
                        .detail("thresholdPercent", (int)(threshold * 100))
                        .log();
                    
                    String errorMessages = allBranchContexts.stream()
                        .filter(b -> Boolean.FALSE.equals(b.getIsSuccessful()))
                        .map(b -> String.format("Branch %d: %s", b.getBranchIndex(), b.getErrorMessage()))
                        .collect(java.util.stream.Collectors.joining("; "));
                    
                    throw new RuntimeException(
                        String.format(ERROR_PARALLEL_BRANCH_THRESHOLD_EXCEEDED + ": %.0f%% failed (threshold: %.0f%%). Errors: %s",
                                failureRate * 100, threshold * 100, errorMessages));
                }
                // Below threshold - continue with successful branches
                logger.warn()
                    .message(LOG_FAILURE_RATE_BELOW_THRESHOLD)
                    .category("parallel-execution")
                    .objectType("ParallelExecution")
                    .objectId(parallelExec.getId().toString())
                    .detail("failureRatePercent", (int)(failureRate * 100))
                    .detail("thresholdPercent", (int)(threshold * 100))
                    .log();
                
            } else if ("FAIL_SAFE".equals(failurePolicy)) {
                // Continue with successful branches only
                logger.info()
                    .message(LOG_PARALLEL_JOIN_FAIL_SAFE_CONTINUING)
                    .category("parallel-execution")
                    .objectType("ParallelExecution")
                    .objectId(parallelExec.getId().toString())
                    .detail("successfulBranches", allBranchContexts.size() - failedBranches)
                    .log();
            } else {
                throw new IllegalArgumentException(ERROR_UNKNOWN_FAILURE_POLICY + failurePolicy);
            }
        }
        
        // Merge only SUCCESSFUL branch contexts (Phase 7)
        Map<String, Object> mergedContext = new HashMap<>();
        for (com.aidentx.workflowsservice.entity.ParallelBranchContext branchCtx : allBranchContexts) {
            if (Boolean.TRUE.equals(branchCtx.getIsSuccessful()) && branchCtx.getBranchContext() != null) {
                mergedContext.putAll(branchCtx.getBranchContext());
            }
        }
        
        long successfulBranches = allBranchContexts.stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsSuccessful()))
            .count();
        
        logger.info()
            .message(LOG_MERGED_SUCCESSFUL_BRANCH_CONTEXTS)
            .category("parallel-execution")
            .objectType("ParallelExecution")
            .objectId(parallelExec.getId().toString())
            .detail("successfulBranches", successfulBranches)
            .detail("mergedKeyCount", mergedContext.size())
            .log();
        
        // Add merge metadata
        mergedContext.put(CONTEXT_KEY_ALL_BRANCHES_COMPLETE, true);
        mergedContext.put(CONTEXT_KEY_COMPLETED_BRANCHES, completed);
        mergedContext.put(CONTEXT_KEY_TOTAL_BRANCHES, total);
        mergedContext.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_PARALLEL_JOIN);
        mergedContext.put(CONTEXT_KEY_EXECUTED, true);
        mergedContext.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return mergedContext;
    }

    // ==================== PHASE 7: FOREACH LOOP CONTROL ====================

    /**
     * Execute control.foreach node - Start of ForEach loop
     * Supports three iteration types:
     * 1. Collection: Iterate over items from context (e.g., users, roles)
     * 2. Count: Iterate a fixed number of times
     * 3. While: Continue while a condition is true
     * 
     * Configuration:
     * - iterationType: "collection" | "count" | "while"
     * - collectionPath: Path to collection in context (for collection type)
     * - countValue: Number of iterations (for count type) 
     * - countExpression: Dynamic count from context (for count type)
     * - whileCondition: Condition expression to evaluate (for while type)
     * - itemVariable: Variable name for current item (default: "item")
     * - indexVariable: Variable name for index (default: "index")
     * - maxIterations: Safety limit (default: 1000)
     * - loopEndNode: ID of the corresponding forEach-end node
     */
    private Map<String, Object> executeForEach(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_FOREACH_NO_COLLECTION);
        }
        
        // Get loop configuration
        String iterationType = (String) nodeData.getOrDefault("iterationType", FOREACH_ITERATION_COLLECTION);
        String loopEndNode = (String) nodeData.get("loopEndNode");
        String itemVariable = (String) nodeData.getOrDefault("itemVariable", "item");
        String indexVariable = (String) nodeData.getOrDefault("indexVariable", "index");
        int maxIterations = nodeData.containsKey("maxIterations") 
            ? ((Number) nodeData.get("maxIterations")).intValue() 
            : FOREACH_DEFAULT_MAX_ITERATIONS;
        
        // NEW: Property mappings to extract nested properties from each item
        // Example: {"currentApproverId": "managerId", "currentUserId": "userId"}
        // This exposes item.managerId as "currentApproverId" in context
        @SuppressWarnings("unchecked")
        Map<String, String> itemPropertyMappings = (Map<String, String>) nodeData.get("itemPropertyMappings");
        
        if (loopEndNode == null || loopEndNode.isBlank()) {
            throw new IllegalArgumentException(ERROR_FOREACH_END_NOT_FOUND + nodeId);
        }
        
        // Check if we're resuming an existing loop (loop state already exists)
        String loopStateKey = CONTEXT_KEY_LOOP_STATE_PREFIX + nodeId;
        @SuppressWarnings("unchecked")
        Map<String, Object> existingLoopState = (Map<String, Object>) context.get(loopStateKey);
        
        if (existingLoopState != null) {
            // Resuming existing loop - advance to next iteration
            return advanceForEachLoop(nodeId, existingLoopState, context, execution, 
                                      itemVariable, indexVariable, maxIterations);
        }
        
        // Starting a new loop - initialize based on iteration type
        List<Object> items;
        int totalItems;
        
        switch (iterationType) {
            case FOREACH_ITERATION_COLLECTION -> {
                String collectionPath = (String) nodeData.get("collectionPath");
                if (collectionPath == null || collectionPath.isBlank()) {
                    throw new IllegalArgumentException(ERROR_FOREACH_NO_COLLECTION);
                }
                
                // Resolve collection from context
                Object collection = resolveContextPath(context, collectionPath);
                if (collection == null) {
                    // Empty collection - skip loop entirely
                    logger.info()
                        .message(LOG_FOREACH_COMPLETED)
                        .category("foreach-loop")
                        .objectType("WorkflowNode")
                        .objectId(nodeId)
                        .detail("reason", "Empty collection")
                        .detail("totalIterations", 0)
                        .log();
                    
                    return Map.of(
                        CONTEXT_KEY_EXECUTED, true,
                        CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH,
                        CONTEXT_KEY_LOOP_CONTINUE, false,
                        CONTEXT_KEY_LOOP_END_NODE, loopEndNode,
                        CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                    );
                }
                
                items = convertToList(collection, collectionPath);
                totalItems = items.size();
            }
            case FOREACH_ITERATION_COUNT -> {
                Integer countValue = nodeData.containsKey("countValue") 
                    ? ((Number) nodeData.get("countValue")).intValue() 
                    : null;
                String countExpression = (String) nodeData.get("countExpression");
                
                if (countValue == null && countExpression != null) {
                    // Resolve count from context expression
                    Object resolvedCount = resolveContextPath(context, countExpression);
                    countValue = resolvedCount != null ? ((Number) resolvedCount).intValue() : 0;
                }
                
                if (countValue == null || countValue <= 0) {
                    // No iterations needed
                    return Map.of(
                        CONTEXT_KEY_EXECUTED, true,
                        CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH,
                        CONTEXT_KEY_LOOP_CONTINUE, false,
                        CONTEXT_KEY_LOOP_END_NODE, loopEndNode,
                        CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                    );
                }
                
                // Create index-based items list
                items = new ArrayList<>();
                for (int i = 0; i < countValue; i++) {
                    items.add(i);
                }
                totalItems = countValue;
            }
            case FOREACH_ITERATION_WHILE -> {
                // For while loops, we don't know total upfront
                // Start with empty items list and evaluate condition each iteration
                items = new ArrayList<>();
                totalItems = -1; // Unknown for while loops
                
                // Evaluate initial condition
                String whileCondition = (String) nodeData.get("whileCondition");
                if (whileCondition == null || whileCondition.isBlank()) {
                    throw new IllegalArgumentException("While condition is required for while-type ForEach");
                }
                
                boolean conditionMet = evaluateCondition(whileCondition, context);
                if (!conditionMet) {
                    // Condition false from start - skip loop
                    logger.info()
                        .message(LOG_FOREACH_WHILE_CONDITION)
                        .category("foreach-loop")
                        .objectType("WorkflowNode")
                        .objectId(nodeId)
                        .detail("conditionResult", false)
                        .detail("condition", whileCondition)
                        .log();
                    
                    return Map.of(
                        CONTEXT_KEY_EXECUTED, true,
                        CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH,
                        CONTEXT_KEY_LOOP_CONTINUE, false,
                        CONTEXT_KEY_LOOP_END_NODE, loopEndNode,
                        CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                    );
                }
            }
            default -> throw new IllegalArgumentException("Unknown iteration type: " + iterationType);
        }
        
        logger.info()
            .message(LOG_FOREACH_STARTING)
            .category("foreach-loop")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("iterationType", iterationType)
            .detail("totalItems", totalItems)
            .log();
        
        // Create initial loop state
        Map<String, Object> loopState = new HashMap<>();
        loopState.put(CONTEXT_KEY_LOOP_INDEX, 0);
        loopState.put(CONTEXT_KEY_LOOP_ITEMS, items);
        loopState.put(CONTEXT_KEY_LOOP_TOTAL, totalItems);
        loopState.put(CONTEXT_KEY_LOOP_RESULTS, new ArrayList<>());
        loopState.put(CONTEXT_KEY_LOOP_START_NODE, nodeId);
        loopState.put(CONTEXT_KEY_LOOP_END_NODE, loopEndNode);
        loopState.put("iterationType", iterationType);
        loopState.put("itemVariable", itemVariable);
        loopState.put("indexVariable", indexVariable);
        loopState.put("maxIterations", maxIterations);
        loopState.put("itemPropertyMappings", itemPropertyMappings); // Store mappings for resume
        if (iterationType.equals(FOREACH_ITERATION_WHILE)) {
            loopState.put("whileCondition", nodeData.get("whileCondition"));
        }
        
        // Set up first iteration variables
        Object currentItem = items.isEmpty() ? null : items.get(0);
        boolean isFirst = true;
        boolean isLast = items.size() <= 1 && totalItems != -1;
        boolean hasMore = items.size() > 1 || totalItems == -1;
        
        logger.info()
            .message(LOG_FOREACH_ITERATION)
            .category("foreach-loop")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("index", 0)
            .detail("currentItem", currentItem != null ? currentItem.toString() : "null")
            .log();
        
        // Return output with loop state and current iteration variables
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH);
        output.put(CONTEXT_KEY_LOOP_CONTINUE, true);
        output.put(CONTEXT_KEY_LOOP_END_NODE, loopEndNode);
        output.put(CONTEXT_KEY_LOOP_START_NODE, nodeId);
        output.put(loopStateKey, loopState);
        
        // Expose current iteration variables
        output.put(itemVariable, currentItem);
        output.put(indexVariable, 0);
        output.put(CONTEXT_KEY_LOOP_IS_FIRST, isFirst);
        output.put(CONTEXT_KEY_LOOP_IS_LAST, isLast);
        output.put(CONTEXT_KEY_LOOP_HAS_MORE, hasMore);
        output.put(CONTEXT_KEY_LOOP_TOTAL, totalItems);
        
        // NEW: Apply property mappings to extract nested properties to top-level context
        applyItemPropertyMappings(output, currentItem, itemPropertyMappings);
        
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return output;
    }
    
    /**
     * Advance to the next iteration of a ForEach loop
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> advanceForEachLoop(
        String nodeId,
        Map<String, Object> loopState,
        Map<String, Object> context,
        WorkflowExecution execution,
        String itemVariable,
        String indexVariable,
        int maxIterations
    ) {
        int currentIndex = ((Number) loopState.get(CONTEXT_KEY_LOOP_INDEX)).intValue();
        List<Object> items = (List<Object>) loopState.get(CONTEXT_KEY_LOOP_ITEMS);
        int totalItems = ((Number) loopState.get(CONTEXT_KEY_LOOP_TOTAL)).intValue();
        String loopEndNode = (String) loopState.get(CONTEXT_KEY_LOOP_END_NODE);
        String iterationType = (String) loopState.get("iterationType");
        
        // Advance index
        int nextIndex = currentIndex + 1;
        
        // Check max iterations safety limit
        if (nextIndex >= maxIterations) {
            throw new RuntimeException(ERROR_FOREACH_MAX_ITERATIONS_EXCEEDED + maxIterations);
        }
        
        // Check if we've completed all iterations
        boolean loopComplete = false;
        Object currentItem = null;
        
        if (iterationType.equals(FOREACH_ITERATION_WHILE)) {
            // For while loops, check condition
            String whileCondition = (String) loopState.get("whileCondition");
            boolean conditionMet = evaluateCondition(whileCondition, context);
            
            logger.info()
                .message(LOG_FOREACH_WHILE_CONDITION)
                .category("foreach-loop")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("conditionResult", conditionMet)
                .detail("iteration", nextIndex)
                .log();
            
            if (!conditionMet) {
                loopComplete = true;
            } else {
                currentItem = nextIndex; // Use index as item for while loops
            }
        } else {
            // For collection/count loops, check if we've exhausted items
            if (nextIndex >= items.size()) {
                loopComplete = true;
            } else {
                currentItem = items.get(nextIndex);
            }
        }
        
        if (loopComplete) {
            // Loop is complete
            List<Object> results = (List<Object>) loopState.get(CONTEXT_KEY_LOOP_RESULTS);
            
            // NEW: Get approved/rejected items collected during loop
            @SuppressWarnings("unchecked")
            List<Object> approvedItems = loopState.get("approvedItems") != null 
                ? (List<Object>) loopState.get("approvedItems") 
                : new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Object> rejectedItems = loopState.get("rejectedItems") != null 
                ? (List<Object>) loopState.get("rejectedItems") 
                : new ArrayList<>();
            
            logger.info()
                .message(LOG_FOREACH_COMPLETED)
                .category("foreach-loop")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("totalIterations", nextIndex)
                .detail("approvedCount", approvedItems.size())
                .detail("rejectedCount", rejectedItems.size())
                .log();
            
            String loopStateKey = CONTEXT_KEY_LOOP_STATE_PREFIX + nodeId;
            
            Map<String, Object> output = new HashMap<>();
            output.put(CONTEXT_KEY_EXECUTED, true);
            output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH);
            output.put(CONTEXT_KEY_LOOP_CONTINUE, false);
            output.put(CONTEXT_KEY_LOOP_END_NODE, loopEndNode);
            output.put(CONTEXT_KEY_LOOP_RESULTS, results);
            
            // Only expose approval tracking if items were actually collected
            if (!approvedItems.isEmpty() || !rejectedItems.isEmpty()) {
                // Use configurable output keys or default to "approved"/"rejected"
                String approvedOutputKey = (String) loopState.getOrDefault("approvedOutputKey", "approved");
                String rejectedOutputKey = (String) loopState.getOrDefault("rejectedOutputKey", "rejected");
                
                output.put(approvedOutputKey, approvedItems);
                output.put(rejectedOutputKey, rejectedItems);
                output.put("allApproved", rejectedItems.isEmpty() && !approvedItems.isEmpty());
                output.put("allRejected", approvedItems.isEmpty() && !rejectedItems.isEmpty());
                output.put("anyApproved", !approvedItems.isEmpty());
                output.put("anyRejected", !rejectedItems.isEmpty());
                output.put("partialApproval", !approvedItems.isEmpty() && !rejectedItems.isEmpty());
            }
            
            // Always output counts and lists for condition nodes to check
            output.put("approvedList", approvedItems);
            output.put("rejectedList", rejectedItems);
            output.put("approvedCount", approvedItems.size());
            output.put("rejectedCount", rejectedItems.size());
            output.put("totalCount", approvedItems.size() + rejectedItems.size());
            
            output.put(loopStateKey, null); // Clear loop state
            output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
            
            return output;
        }
        
        // Continue with next iteration
        loopState.put(CONTEXT_KEY_LOOP_INDEX, nextIndex);
        
        boolean isFirst = false;
        boolean isLast = nextIndex == items.size() - 1 && totalItems != -1;
        boolean hasMore = nextIndex < items.size() - 1 || totalItems == -1;
        
        logger.info()
            .message(LOG_FOREACH_ITERATION)
            .category("foreach-loop")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("index", nextIndex)
            .detail("currentItem", currentItem != null ? currentItem.toString() : "null")
            .log();
        
        String loopStateKey = CONTEXT_KEY_LOOP_STATE_PREFIX + nodeId;
        
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH);
        output.put(CONTEXT_KEY_LOOP_CONTINUE, true);
        output.put(CONTEXT_KEY_LOOP_END_NODE, loopEndNode);
        output.put(CONTEXT_KEY_LOOP_START_NODE, nodeId);
        output.put(loopStateKey, loopState);
        
        // Expose current iteration variables
        output.put(itemVariable, currentItem);
        output.put(indexVariable, nextIndex);
        output.put(CONTEXT_KEY_LOOP_IS_FIRST, isFirst);
        output.put(CONTEXT_KEY_LOOP_IS_LAST, isLast);
        output.put(CONTEXT_KEY_LOOP_HAS_MORE, hasMore);
        output.put(CONTEXT_KEY_LOOP_TOTAL, totalItems);
        
        // NEW: Apply property mappings to extract nested properties to top-level context
        @SuppressWarnings("unchecked")
        Map<String, String> itemPropertyMappings = (Map<String, String>) loopState.get("itemPropertyMappings");
        applyItemPropertyMappings(output, currentItem, itemPropertyMappings);
        
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return output;
    }

    /**
     * Execute control.foreach-end node - End of ForEach loop
     * Collects results from current iteration and determines whether to continue looping
     * or proceed to the next node after the loop.
     */
    private Map<String, Object> executeForEachEnd(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        // Get the corresponding loop start node
        String loopStartNode = nodeData != null ? (String) nodeData.get("loopStartNode") : null;
        
        // Find active loop state
        String loopStateKey = null;
        Map<String, Object> loopState = null;
        
        if (loopStartNode != null) {
            loopStateKey = CONTEXT_KEY_LOOP_STATE_PREFIX + loopStartNode;
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) context.get(loopStateKey);
            loopState = state;
        } else {
            // Search for any active loop state (for backward compatibility)
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getKey().startsWith(CONTEXT_KEY_LOOP_STATE_PREFIX) && entry.getValue() instanceof Map) {
                    loopStateKey = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> state = (Map<String, Object>) entry.getValue();
                    loopState = state;
                    loopStartNode = loopStateKey.substring(CONTEXT_KEY_LOOP_STATE_PREFIX.length());
                    break;
                }
            }
        }
        
        if (loopState == null) {
            throw new IllegalStateException(ERROR_FOREACH_NO_LOOP_STATE);
        }
        
        int currentIndex = ((Number) loopState.get(CONTEXT_KEY_LOOP_INDEX)).intValue();
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) loopState.get(CONTEXT_KEY_LOOP_RESULTS);
        
        // NEW: Support for approved/rejected user tracking
        // This allows collecting only approved users for the final action
        @SuppressWarnings("unchecked")
        List<Object> approvedItems = loopState.get("approvedItems") != null 
            ? (List<Object>) loopState.get("approvedItems") 
            : new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object> rejectedItems = loopState.get("rejectedItems") != null 
            ? (List<Object>) loopState.get("rejectedItems") 
            : new ArrayList<>();
        
        // Check approval status and collect accordingly
        // The approval node sets "approved" in context after decision
        Boolean wasApproved = (Boolean) context.get(CONTEXT_KEY_APPROVED);
        
        // Get the value to collect (configured via collectApprovedKey, e.g., "currentUserId")
        String collectApprovedKey = nodeData != null ? (String) nodeData.get("collectApprovedKey") : null;
        // Get configurable output key names (defaults to "approved" + key + "s" / "rejected" + key + "s")
        String approvedOutputKey = nodeData != null ? (String) nodeData.get("approvedOutputKey") : null;
        String rejectedOutputKey = nodeData != null ? (String) nodeData.get("rejectedOutputKey") : null;
        
        // Store output key names in loop state for use during advanceForEachLoop
        if (approvedOutputKey != null) {
            loopState.put("approvedOutputKey", approvedOutputKey);
        }
        if (rejectedOutputKey != null) {
            loopState.put("rejectedOutputKey", rejectedOutputKey);
        }
        
        if (collectApprovedKey != null && context.containsKey(collectApprovedKey)) {
            Object valueToCollect = context.get(collectApprovedKey);
            if (Boolean.TRUE.equals(wasApproved)) {
                approvedItems.add(valueToCollect);
                logger.debug()
                    .message("Collected approved item in forEach loop")
                    .category("foreach-loop")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("collectApprovedKey", collectApprovedKey)
                    .detail("value", valueToCollect.toString())
                    .detail("totalApproved", approvedItems.size())
                    .log();
            } else if (Boolean.FALSE.equals(wasApproved)) {
                rejectedItems.add(valueToCollect);
                logger.debug()
                    .message("Collected rejected item in forEach loop")
                    .category("foreach-loop")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("collectApprovedKey", collectApprovedKey)
                    .detail("value", valueToCollect.toString())
                    .detail("totalRejected", rejectedItems.size())
                    .log();
            }
        }
        
        // Store updated approval tracking
        loopState.put("approvedItems", approvedItems);
        loopState.put("rejectedItems", rejectedItems);
        
        // Optionally collect result from this iteration (legacy behavior)
        String resultCollectionKey = nodeData != null ? (String) nodeData.get("resultCollectionKey") : null;
        if (resultCollectionKey != null && context.containsKey(resultCollectionKey)) {
            results.add(context.get(resultCollectionKey));
        }
        
        // Store updated results
        loopState.put(CONTEXT_KEY_LOOP_RESULTS, results);
        
        logger.info()
            .message(LOG_FOREACH_END_CONTINUE)
            .category("foreach-loop")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("currentIndex", currentIndex)
            .detail("resultsCollected", results.size())
            .log();
        
        // Return with flag to loop back to start node
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_CONTROL_FOREACH_END);
        output.put(CONTEXT_KEY_LOOP_CONTINUE, true);
        output.put(CONTEXT_KEY_LOOP_START_NODE, loopStartNode);
        output.put(loopStateKey, loopState);
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return output;
    }
    
    /**
     * Resolve a dot-notation path from context
     * e.g., "users.selected" or "approval.approvedRoles"
     */
    @SuppressWarnings("unchecked")
    private Object resolveContextPath(Map<String, Object> context, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Apply property mappings to extract nested properties from current item to top-level output
     * Example: If currentItem is {"userId": "u1", "managerId": "m1"}
     * and mappings is {"currentApproverId": "managerId", "currentUserId": "userId"}
     * then output will have "currentApproverId" = "m1" and "currentUserId" = "u1"
     * 
     * This allows approval nodes inside forEach loops to access nested properties directly
     * from the loop item, e.g., approval.single can use approverId = currentApproverId
     */
    @SuppressWarnings("unchecked")
    private void applyItemPropertyMappings(
            Map<String, Object> output, 
            Object currentItem, 
            Map<String, String> itemPropertyMappings) {
        
        if (itemPropertyMappings == null || itemPropertyMappings.isEmpty()) {
            return;
        }
        
        if (currentItem == null) {
            logger.warn()
                .message("Cannot apply property mappings: currentItem is null")
                .category("foreach-loop")
                .log();
            return;
        }
        
        if (!(currentItem instanceof Map)) {
            logger.warn()
                .message("Cannot apply property mappings: currentItem is not a Map")
                .category("foreach-loop")
                .detail("itemType", currentItem.getClass().getName())
                .log();
            return;
        }
        
        Map<String, Object> itemMap = (Map<String, Object>) currentItem;
        
        for (Map.Entry<String, String> mapping : itemPropertyMappings.entrySet()) {
            String outputKey = mapping.getKey();      // e.g., "currentApproverId"
            String propertyPath = mapping.getValue(); // e.g., "managerId" or "nested.property"
            
            // Support dot-notation for nested properties within the item
            Object value;
            if (propertyPath.contains(".")) {
                // Create a temp context with just the item to use resolveContextPath
                Map<String, Object> tempContext = new HashMap<>();
                tempContext.put("_item", itemMap);
                value = resolveContextPath(tempContext, "_item." + propertyPath);
            } else {
                value = itemMap.get(propertyPath);
            }
            
            if (value != null) {
                output.put(outputKey, value);
                logger.debug()
                    .message("Applied property mapping")
                    .category("foreach-loop")
                    .detail("outputKey", outputKey)
                    .detail("propertyPath", propertyPath)
                    .detail("value", value.toString())
                    .log();
            } else {
                logger.warn()
                    .message("Property not found in item for mapping")
                    .category("foreach-loop")
                    .detail("outputKey", outputKey)
                    .detail("propertyPath", propertyPath)
                    .detail("availableKeys", itemMap.keySet().toString())
                    .log();
            }
        }
    }
    
    /**
     * Convert various collection types to a List
     */
    @SuppressWarnings("unchecked")
    private List<Object> convertToList(Object collection, String path) {
        if (collection instanceof List) {
            return (List<Object>) collection;
        } else if (collection instanceof Collection) {
            return new ArrayList<>((Collection<?>) collection);
        } else if (collection instanceof Object[]) {
            return Arrays.asList((Object[]) collection);
        } else if (collection instanceof Map) {
            // If it's a map, iterate over entries
            Map<String, Object> map = (Map<String, Object>) collection;
            List<Object> list = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("key", entry.getKey());
                item.put("value", entry.getValue());
                list.add(item);
            }
            return list;
        } else {
            throw new IllegalArgumentException(ERROR_FOREACH_INVALID_COLLECTION + path);
        }
    }
    
    /**
     * Evaluate a simple condition expression
     * Supports: comparisons (==, !=, <, >, <=, >=), boolean context values
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        
        // Simple variable reference (e.g., "shouldContinue")
        if (!condition.contains("==") && !condition.contains("!=") && 
            !condition.contains("<") && !condition.contains(">")) {
            Object value = resolveContextPath(context, condition.trim());
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return value != null && !value.equals(0) && !value.equals("");
        }
        
        // Comparison expression
        String[] operators = {"==", "!=", "<=", ">=", "<", ">"};
        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(Pattern.quote(op), 2);
                if (parts.length == 2) {
                    Object left = resolveOrParseValue(parts[0].trim(), context);
                    Object right = resolveOrParseValue(parts[1].trim(), context);
                    return compareValues(left, right, op);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Resolve a value from context or parse as literal
     */
    private Object resolveOrParseValue(String value, Map<String, Object> context) {
        if (value == null) return null;
        
        // Check if it's a quoted string literal
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        // Check if it's a number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // Not a number
        }
        
        // Check for boolean literals
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        if ("null".equalsIgnoreCase(value)) return null;
        
        // Resolve from context
        return resolveContextPath(context, value);
    }
    
    /**
     * Compare two values with the given operator
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compareValues(Object left, Object right, String operator) {
        // Handle null comparisons
        if (left == null && right == null) {
            return "==".equals(operator);
        }
        if (left == null || right == null) {
            return "!=".equals(operator);
        }
        
        // String comparison
        if (left instanceof String && right instanceof String) {
            int cmp = ((String) left).compareTo((String) right);
            return evaluateComparison(cmp, operator);
        }
        
        // Numeric comparison
        if (left instanceof Number && right instanceof Number) {
            double leftD = ((Number) left).doubleValue();
            double rightD = ((Number) right).doubleValue();
            int cmp = Double.compare(leftD, rightD);
            return evaluateComparison(cmp, operator);
        }
        
        // Boolean comparison
        if (left instanceof Boolean && right instanceof Boolean) {
            if ("==".equals(operator)) return left.equals(right);
            if ("!=".equals(operator)) return !left.equals(right);
            return false;
        }
        
        // Generic equals/not-equals
        if ("==".equals(operator)) return left.equals(right);
        if ("!=".equals(operator)) return !left.equals(right);
        
        return false;
    }
    
    /**
     * Evaluate a comparison result based on operator
     */
    private boolean evaluateComparison(int cmp, String operator) {
        return switch (operator) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case "<" -> cmp < 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case ">=" -> cmp >= 0;
            default -> false;
        };
    }

    // ==================== PHASE 7: DYNAMIC SWITCH GATEWAY ====================

    /**
     * Execute gateway.switch node - Dynamic routing based on conditions
     * Routes items from a collection to DIFFERENT handlers based on case conditions.
     * Unlike parallel-split (same handler for all), switch routes to specific branches.
     * 
     * Configuration:
     * - itemsPath: Path to collection in context (e.g., "selectedRoles")
     * - cases: Array of case definitions with condition and branchId
     * - defaultBranch: Optional default branch if no case matches
     * - executionMode: "parallel" | "sequential"
     * - joinNode: ID of the corresponding switch-join node
     */
    private Map<String, Object> executeDynamicSwitch(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_SWITCH_NO_ITEMS);
        }
        
        // Get configuration
        String itemsPath = (String) nodeData.get("itemsPath");
        String joinNodeId = (String) nodeData.get("joinNode");
        String executionMode = (String) nodeData.getOrDefault("executionMode", SWITCH_MODE_SEQUENTIAL);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = (List<Map<String, Object>>) nodeData.get("cases");
        String defaultBranch = (String) nodeData.get("defaultBranch");
        
        if (joinNodeId == null || joinNodeId.isBlank()) {
            throw new IllegalArgumentException(ERROR_SWITCH_JOIN_NOT_FOUND + nodeId);
        }
        
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException(ERROR_SWITCH_NO_CASES);
        }
        
        // Resolve items from context
        Object itemsObj = resolveContextPath(context, itemsPath);
        List<Object> items = itemsObj != null ? convertToList(itemsObj, itemsPath) : new ArrayList<>();
        
        if (items.isEmpty()) {
            logger.info()
                .message(LOG_SWITCH_STARTING)
                .category("switch-gateway")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("totalItems", 0)
                .detail("cases", cases.size())
                .log();
            
            // No items to process - skip directly to join
            return Map.of(
                CONTEXT_KEY_EXECUTED, true,
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_SWITCH,
                CONTEXT_KEY_SWITCH_JOIN_NODE, joinNodeId,
                CONTEXT_KEY_SWITCH_RESULTS, new ArrayList<>(),
                "isEmpty", true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        logger.info()
            .message(LOG_SWITCH_STARTING)
            .category("switch-gateway")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("totalItems", items.size())
            .detail("cases", cases.size())
            .detail("executionMode", executionMode)
            .log();
        
        // Route each item to appropriate case
        List<Map<String, Object>> routingPlan = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            String matchedBranch = null;
            String matchedCase = null;
            
            // Create item-specific context for condition evaluation
            Map<String, Object> itemContext = new HashMap<>(context);
            itemContext.put(CONTEXT_KEY_SWITCH_ITEM, item);
            itemContext.put(CONTEXT_KEY_SWITCH_ITEM_INDEX, i);
            
            // Evaluate each case condition
            for (Map<String, Object> caseConfig : cases) {
                String condition = (String) caseConfig.get("condition");
                String branchId = (String) caseConfig.get("branchId");
                String caseName = (String) caseConfig.getOrDefault("name", condition);
                
                boolean conditionMet = evaluateSwitchCondition(condition, item, itemContext);
                
                if (conditionMet) {
                    matchedBranch = branchId;
                    matchedCase = caseName;
                    
                    logger.info()
                        .message(LOG_SWITCH_ROUTING_ITEM)
                        .category("switch-gateway")
                        .objectType("WorkflowNode")
                        .objectId(nodeId)
                        .detail("itemIndex", i)
                        .detail("condition", caseName)
                        .detail("branch", branchId)
                        .log();
                    break;
                }
            }
            
            // Use default branch if no case matched
            if (matchedBranch == null && defaultBranch != null) {
                matchedBranch = defaultBranch;
                matchedCase = "default";
                
                logger.info()
                    .message(LOG_SWITCH_DEFAULT_ROUTE)
                    .category("switch-gateway")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("itemIndex", i)
                    .log();
            }
            
            if (matchedBranch != null) {
                Map<String, Object> routing = new HashMap<>();
                routing.put("itemIndex", i);
                routing.put("item", item);
                routing.put("branchId", matchedBranch);
                routing.put("caseName", matchedCase);
                routingPlan.add(routing);
            }
        }
        
        // Store execution state for join node
        Map<String, Object> switchExecState = new HashMap<>();
        switchExecState.put("splitNodeId", nodeId);
        switchExecState.put("joinNodeId", joinNodeId);
        switchExecState.put("totalItems", routingPlan.size());
        switchExecState.put("completedItems", 0);
        switchExecState.put("results", new ArrayList<>());
        switchExecState.put("routingPlan", routingPlan);
        switchExecState.put("executionMode", executionMode);
        
        String switchStateKey = CONTEXT_KEY_SWITCH_EXEC + nodeId;
        
        // Return output that will trigger branch execution
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_SWITCH);
        output.put(CONTEXT_KEY_SWITCH_JOIN_NODE, joinNodeId);
        output.put(CONTEXT_KEY_SWITCH_MODE, executionMode);
        output.put("routingPlan", routingPlan);
        output.put(switchStateKey, switchExecState);
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        // Mark as parallel execution if in parallel mode (WorkflowExecutor will handle dispatch)
        if (SWITCH_MODE_PARALLEL.equals(executionMode) && routingPlan.size() > 1) {
            output.put(CONTEXT_KEY_SPLIT_NODE, true);
            output.put(CONTEXT_KEY_BRANCHES, routingPlan.size());
        }
        
        return output;
    }
    
    /**
     * Evaluate a switch condition for a specific item
     * Supports:
     * - Simple field comparison: "item.type == 'admin'"
     * - Context path comparison: "item.priority > 5"
     * - Boolean item properties: "item.isActive"
     */
    private boolean evaluateSwitchCondition(String condition, Object item, Map<String, Object> context) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        
        // Replace "item" references with actual item data in context
        if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) item;
            context.put("item", itemMap);
        } else {
            context.put("item", item);
        }
        
        // Use existing condition evaluation
        return evaluateCondition(condition, context);
    }

    /**
     * Execute gateway.switch-join node - Collect results from switch branches
     * Waits for all routed items to be processed before continuing.
     */
    private Map<String, Object> executeDynamicSwitchJoin(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String joinNodeId = (String) node.get(FIELD_ID);
        
        logger.info()
            .message("Executing switch-join node")
            .category("switch-gateway")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .log();
        
        // Find the switch execution state
        String switchStateKey = null;
        Map<String, Object> switchExecState = null;
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey().startsWith(CONTEXT_KEY_SWITCH_EXEC) && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> state = (Map<String, Object>) entry.getValue();
                if (joinNodeId.equals(state.get("joinNodeId"))) {
                    switchStateKey = entry.getKey();
                    switchExecState = state;
                    break;
                }
            }
        }
        
        if (switchExecState == null) {
            throw new IllegalStateException(ERROR_SWITCH_EXEC_NOT_FOUND + joinNodeId);
        }
        
        int totalItems = ((Number) switchExecState.get("totalItems")).intValue();
        int completedItems = ((Number) switchExecState.getOrDefault("completedItems", 0)).intValue();
        
        // Increment completed count
        completedItems++;
        switchExecState.put("completedItems", completedItems);
        
        // Collect result from current branch
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) switchExecState.get("results");
        if (results == null) {
            results = new ArrayList<>();
        }
        
        // Capture current branch result
        Map<String, Object> branchResult = new HashMap<>();
        branchResult.put("itemIndex", context.get(CONTEXT_KEY_SWITCH_ITEM_INDEX));
        branchResult.put("item", context.get(CONTEXT_KEY_SWITCH_ITEM));
        branchResult.put("caseName", context.get(CONTEXT_KEY_SWITCH_CASE));
        branchResult.put("branchOutput", new HashMap<>(context));
        results.add(branchResult);
        switchExecState.put("results", results);
        
        logger.info()
            .message(LOG_SWITCH_JOIN_WAITING)
            .category("switch-gateway")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .detail("completedItems", completedItems)
            .detail("totalItems", totalItems)
            .log();
        
        // Check if all items are processed
        if (completedItems < totalItems) {
            // Still waiting for more branches
            Map<String, Object> output = new HashMap<>();
            output.put(CONTEXT_KEY_PAUSE_REQUIRED, true);
            output.put(CONTEXT_KEY_REASON, "Waiting for switch branches");
            output.put(CONTEXT_KEY_COMPLETED_BRANCHES, completedItems);
            output.put(CONTEXT_KEY_TOTAL_BRANCHES, totalItems);
            output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_SWITCH_JOIN);
            output.put(switchStateKey, switchExecState);
            output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
            
            return output;
        }
        
        // All items processed - merge results
        logger.info()
            .message(LOG_SWITCH_JOIN_COMPLETE)
            .category("switch-gateway")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .detail("totalResults", results.size())
            .log();
        
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_SWITCH_JOIN);
        output.put(CONTEXT_KEY_SWITCH_RESULTS, results);
        output.put(CONTEXT_KEY_ALL_BRANCHES_COMPLETE, true);
        output.put(switchStateKey, null); // Clear state
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return output;
    }

    // ==================== PHASE 7: DYNAMIC PARALLEL SPLIT ====================

    /**
     * Execute gateway.dynamic-split node - Split into parallel branches based on collection
     * Unlike regular parallel-split (fixed branches), this creates a branch for EACH ITEM
     * in a collection. All branches execute the SAME handler but with different item data.
     * 
     * Use case: Process multiple roles, users, or resources in parallel where each
     * item goes through the same workflow steps.
     * 
     * Configuration:
     * - itemsPath: Path to collection in context (e.g., "selectedRoles")
     * - itemVariable: Variable name for current item (default: "dynamicItem")
     * - indexVariable: Variable name for index (default: "dynamicItemIndex")
     * - joinNode: ID of the corresponding dynamic-join node
     * - maxParallel: Maximum concurrent branches (optional, for throttling)
     */
    private Map<String, Object> executeDynamicParallelSplit(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        
        if (nodeData == null) {
            throw new IllegalArgumentException(ERROR_DYNAMIC_SPLIT_NO_ITEMS);
        }
        
        // Get configuration
        String itemsPath = (String) nodeData.get("itemsPath");
        String joinNodeId = (String) nodeData.get("joinNode");
        String itemVariable = (String) nodeData.getOrDefault("itemVariable", "dynamicItem");
        String indexVariable = (String) nodeData.getOrDefault("indexVariable", "dynamicItemIndex");
        
        if (itemsPath == null || itemsPath.isBlank()) {
            throw new IllegalArgumentException(ERROR_DYNAMIC_SPLIT_NO_ITEMS);
        }
        
        if (joinNodeId == null || joinNodeId.isBlank()) {
            throw new IllegalArgumentException(ERROR_DYNAMIC_SPLIT_JOIN_NOT_FOUND + nodeId);
        }
        
        // Resolve items from context
        Object itemsObj = resolveContextPath(context, itemsPath);
        List<Object> items = itemsObj != null ? convertToList(itemsObj, itemsPath) : new ArrayList<>();
        
        if (items.isEmpty()) {
            logger.info()
                .message(LOG_DYNAMIC_SPLIT_STARTING)
                .category("dynamic-split")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("totalItems", 0)
                .log();
            
            // No items - skip directly to join with empty results
            return Map.of(
                CONTEXT_KEY_EXECUTED, true,
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_DYNAMIC_SPLIT,
                CONTEXT_KEY_JOIN_NODE, joinNodeId,
                CONTEXT_KEY_DYNAMIC_RESULTS, new ArrayList<>(),
                "isEmpty", true,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        logger.info()
            .message(LOG_DYNAMIC_SPLIT_STARTING)
            .category("dynamic-split")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("totalItems", items.size())
            .log();
        
        // Create parallel execution tracker in database
        com.aidentx.workflowsservice.entity.ParallelExecution parallelExec = 
            com.aidentx.workflowsservice.entity.ParallelExecution.builder()
                .executionId(execution.getId())
                .splitNodeId(nodeId)
                .joinNodeId(joinNodeId)
                .totalBranches(items.size())
                .completedBranches(0)
                .createdAt(java.time.OffsetDateTime.now())
                .build();
        
        parallelExecutionRepository.save(parallelExec);
        
        // Create branch info for each item
        List<Map<String, Object>> branchInfo = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> branch = new HashMap<>();
            branch.put("itemIndex", i);
            branch.put("item", items.get(i));
            branch.put(itemVariable, items.get(i));
            branch.put(indexVariable, i);
            branchInfo.add(branch);
            
            logger.debug()
                .message(LOG_DYNAMIC_SPLIT_BRANCH_DISPATCHED)
                .category("dynamic-split")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("itemIndex", i)
                .detail("item", items.get(i).toString())
                .log();
        }
        
        // Store execution state for join node
        String splitStateKey = CONTEXT_KEY_DYNAMIC_SPLIT_EXEC + nodeId;
        Map<String, Object> splitExecState = new HashMap<>();
        splitExecState.put("splitNodeId", nodeId);
        splitExecState.put("joinNodeId", joinNodeId);
        splitExecState.put("totalItems", items.size());
        splitExecState.put("completedItems", 0);
        splitExecState.put("results", new ArrayList<>());
        splitExecState.put("itemVariable", itemVariable);
        splitExecState.put("indexVariable", indexVariable);
        
        // Return output that signals parallel split to WorkflowExecutor
        Map<String, Object> output = new HashMap<>();
        output.put(CONTEXT_KEY_EXECUTED, true);
        output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_DYNAMIC_SPLIT);
        output.put(CONTEXT_KEY_SPLIT_NODE, true); // Signal parallel dispatch
        output.put(CONTEXT_KEY_JOIN_NODE, joinNodeId);
        output.put(CONTEXT_KEY_BRANCHES, items.size());
        output.put("branchInfo", branchInfo);
        output.put("isDynamicSplit", true); // Flag to distinguish from regular parallel split
        output.put(splitStateKey, splitExecState);
        output.put(CONTEXT_KEY_DYNAMIC_TOTAL_ITEMS, items.size());
        output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return output;
    }

    /**
     * Execute gateway.dynamic-join node - Collect results from dynamic parallel branches
     * Waits for all item-based parallel branches to complete before continuing.
     */
    private Map<String, Object> executeDynamicParallelJoin(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        String joinNodeId = (String) node.get(FIELD_ID);
        
        logger.info()
            .message("Executing dynamic-join node")
            .category("dynamic-split")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .log();
        
        // Find parallel execution record with pessimistic lock
        com.aidentx.workflowsservice.entity.ParallelExecution parallelExec = 
            parallelExecutionRepository.findByExecutionIdAndJoinNodeIdWithLock(
                execution.getId(), 
                joinNodeId
            ).orElseThrow(() -> new IllegalStateException(
                ERROR_DYNAMIC_SPLIT_EXEC_NOT_FOUND + joinNodeId
            ));
        
        // Atomically increment completed branches
        parallelExec.incrementCompletedBranches();
        parallelExecutionRepository.save(parallelExec);
        
        int completed = parallelExec.getCompletedBranches();
        int total = parallelExec.getTotalBranches();
        
        logger.info()
            .message(LOG_DYNAMIC_SPLIT_JOIN_WAITING)
            .category("dynamic-split")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .detail("completedBranches", completed)
            .detail("totalBranches", total)
            .log();
        
        // Check if all branches are complete
        if (!parallelExec.isComplete()) {
            // Not all branches done yet - pause here
            return Map.of(
                CONTEXT_KEY_PAUSE_REQUIRED, true,
                CONTEXT_KEY_REASON, "Waiting for dynamic parallel branches",
                CONTEXT_KEY_COMPLETED_BRANCHES, completed,
                CONTEXT_KEY_TOTAL_BRANCHES, total,
                CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_DYNAMIC_JOIN,
                CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
            );
        }
        
        // All branches complete - retrieve branch contexts
        List<com.aidentx.workflowsservice.entity.ParallelBranchContext> allBranchContexts = 
            parallelBranchContextRepository.findByExecutionIdAndJoinNodeId(
                execution.getId(), 
                joinNodeId
            );
        
        // Collect results from all branches - each branch's output stored separately
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Keys that are branch-specific and should NOT be merged to parent context
        Set<String> branchSpecificKeys = Set.of(
            "dynamicItem", "dynamicItemIndex", "itemIndex", "item",
            "branchContextId", "branchIndex", CONTEXT_KEY_DYNAMIC_ITEM, CONTEXT_KEY_DYNAMIC_ITEM_INDEX
        );
        
        // Track which keys were set by the split node's itemVariable/indexVariable
        String splitNodeId = parallelExec.getSplitNodeId();
        
        for (com.aidentx.workflowsservice.entity.ParallelBranchContext branchCtx : allBranchContexts) {
            if (branchCtx.getBranchContext() != null) {
                Map<String, Object> branchData = branchCtx.getBranchContext();
                
                // Create result entry with full branch output
                Map<String, Object> result = new HashMap<>();
                result.put("branchIndex", branchCtx.getBranchIndex());
                result.put("item", branchData.get(CONTEXT_KEY_DYNAMIC_ITEM));
                result.put("isSuccessful", branchCtx.getIsSuccessful());
                result.put("errorMessage", branchCtx.getErrorMessage());
                
                // Store branch-specific output (excluding inherited parent context keys)
                // This contains what this branch actually produced
                Map<String, Object> branchOutput = new HashMap<>();
                for (Map.Entry<String, Object> entry : branchData.entrySet()) {
                    String key = entry.getKey();
                    // Include keys that are likely branch outputs (not inherited from parent)
                    // This is a heuristic - branch outputs typically have node IDs or specific patterns
                    if (!branchSpecificKeys.contains(key)) {
                        branchOutput.put(key, entry.getValue());
                    }
                }
                result.put("output", branchOutput);
                results.add(result);
            }
        }
        
        // Sort results by branch index for consistent ordering
        results.sort((a, b) -> {
            Integer indexA = (Integer) a.get("branchIndex");
            Integer indexB = (Integer) b.get("branchIndex");
            return indexA.compareTo(indexB);
        });
        
        logger.info()
            .message(LOG_DYNAMIC_SPLIT_JOIN_COMPLETE)
            .category("dynamic-split")
            .objectType("WorkflowNode")
            .objectId(joinNodeId)
            .detail("totalResults", results.size())
            .log();
        
        // Start with the ORIGINAL parent context (passed to this node)
        // This preserves jwt, tenantId, and all pre-split data
        Map<String, Object> outputContext = new HashMap<>(context);
        
        // Remove any branch-specific keys that might have leaked into context
        branchSpecificKeys.forEach(outputContext::remove);
        
        // Add the collected results - this is the main output of the join
        outputContext.put(CONTEXT_KEY_DYNAMIC_RESULTS, results);
        
        // Add convenience aggregations
        List<Map<String, Object>> successfulResults = results.stream()
            .filter(r -> Boolean.TRUE.equals(r.get("isSuccessful")))
            .toList();
        List<Map<String, Object>> failedResults = results.stream()
            .filter(r -> !Boolean.TRUE.equals(r.get("isSuccessful")))
            .toList();
        
        outputContext.put("successfulBranches", successfulResults);
        outputContext.put("failedBranches", failedResults);
        outputContext.put("successCount", successfulResults.size());
        outputContext.put("failureCount", failedResults.size());
        
        // Add metadata
        outputContext.put(CONTEXT_KEY_ALL_BRANCHES_COMPLETE, true);
        outputContext.put(CONTEXT_KEY_COMPLETED_BRANCHES, completed);
        outputContext.put(CONTEXT_KEY_TOTAL_BRANCHES, total);
        outputContext.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_GATEWAY_DYNAMIC_JOIN);
        outputContext.put(CONTEXT_KEY_EXECUTED, true);
        outputContext.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
        
        return outputContext;
    }

    /**
     * Execute action.http node - Generic HTTP/REST API call
     * Enables workflows to interact with any HTTP service with template variable support
     * 
     * Configuration:
     * - method: GET, POST, PUT, DELETE, PATCH
     * - url: Target URL with template variables {{context.field}}
     * - headers: Map of HTTP headers (optional)
     * - body: Request body (optional, for POST/PUT/PATCH)
     * - authType: none, bearer, basic, apiKey
     * - timeout: Request timeout in milliseconds (default: 30000)
     * - retries: Number of retry attempts (default: 3)
     * - successCodes: List of success HTTP codes (default: [200, 201, 204])
     */
    private Map<String, Object> executeActionHttp(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        logger.info()
            .message(LOG_EXECUTING_HTTP_ACTION_NODE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .log();
        
        try {
            // Extract node configuration
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
            if (nodeData == null) {
                throw new IllegalArgumentException("Node data is required for HTTP action");
            }
            
            // Check simulation mode early
            Boolean simulationMode = execution.getExecutionContext() != null ? 
                (Boolean) execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;

            boolean isAccessPortalWorkflow = context != null && context.containsKey("accessRequestId");

                  // --- PUBLISH PROVISIONING STARTED EVENT ---
            if (isAccessPortalWorkflow && !Boolean.TRUE.equals(simulationMode)) {
            try {
                if (accessPortalEventPublisher != null) {
                    accessPortalEventPublisher.publishProvisioningStarted(execution.getId(), context);
                    logger.info()
                        .message("Published provisioning.started event for HTTP action")
                        .category("provisioning-events")
                        .detail("executionId", execution.getId().toString())
                        .log();
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Failed to publish provisioning.started event, continuing with HTTP call")
                    .detail("error", e.getMessage())
                    .log();
            }
        }
            
            // Check if node has a template that should be applied at runtime
            // If nodeData contains a "template" field (e.g., "action.role.create"), we need to
            // merge the template's bodyTemplate with the current body to include any missing fields
            String templateName = (String) nodeData.get("template");
            if (templateName != null && !templateName.isEmpty() && actionTemplateService != null) {
                logger.info()
                    .message(LOG_APPLYING_ACTION_TEMPLATE_RUNTIME)
                    .category("http-action")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("template", templateName)
                    .log();
                
                // Get the template from the service
                ActionTemplateService.ActionTemplate templateObj = actionTemplateService.getTemplate(templateName);
                if (templateObj != null && templateObj.getTemplate() != null) {
                    Map<String, Object> template = templateObj.getTemplate();
                    
                    // Merge bodyTemplate from template into nodeData.body
                    if (template.containsKey("bodyTemplate")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bodyTemplate = (Map<String, Object>) template.get("bodyTemplate");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentBody = (Map<String, Object>) nodeData.get("body");
                        
                        if (currentBody == null) {
                            currentBody = new HashMap<>();
                        } else {
                            currentBody = new HashMap<>(currentBody); // Make mutable copy
                        }
                        
                        // Merge template body fields into current body (template takes precedence for missing fields)
                        for (Map.Entry<String, Object> entry : bodyTemplate.entrySet()) {
                            if (!currentBody.containsKey(entry.getKey())) {
                                currentBody.put(entry.getKey(), entry.getValue());
                                logger.trace()
                                    .message(LOG_ADDED_MISSING_FIELD_FROM_TEMPLATE)
                                    .detail("field", entry.getKey())
                                    .detail("value", entry.getValue())
                                    .log();
                            }
                        }
                        
                        nodeData.put("body", currentBody);
                    }
                }
            }
            // =============================================================================
            
            // NEW: Support contextMappings to remap context keys before template substitution
            // Example: {"userIds": "approvedUserIds"} will make {{userIds}} use context.approvedUserIds
            // This allows workflows to dynamically select which context values to use in templates
            @SuppressWarnings("unchecked")
            Map<String, String> contextMappings = (Map<String, String>) nodeData.get("contextMappings");
            Map<String, Object> effectiveContext = context;
            
            if (contextMappings != null && !contextMappings.isEmpty()) {
                // Create a new context with remapped keys
                effectiveContext = new HashMap<>(context);
                for (Map.Entry<String, String> mapping : contextMappings.entrySet()) {
                    String targetKey = mapping.getKey();     // e.g., "userIds" (what template expects)
                    String sourceKey = mapping.getValue();   // e.g., "approvedUserIds" (actual context key)
                    
                    if (context.containsKey(sourceKey)) {
                        Object sourceValue = context.get(sourceKey);
                        effectiveContext.put(targetKey, sourceValue);
                        
                        // Prevent userId/userIds conflicts: if mapping to userIds, remove userId and vice versa
                        // This prevents "Either userId or userIds must be provided, but not both" API errors
                        if ("userIds".equals(targetKey) && effectiveContext.containsKey("userId")) {
                            effectiveContext.remove("userId");
                            logger.debug()
                                .message("Removed conflicting 'userId' key when mapping to 'userIds'")
                                .category("http-action")
                                .objectType("WorkflowNode")
                                .objectId((String) node.get(FIELD_ID))
                                .log();
                        } else if ("userId".equals(targetKey) && effectiveContext.containsKey("userIds")) {
                            effectiveContext.remove("userIds");
                            logger.debug()
                                .message("Removed conflicting 'userIds' key when mapping to 'userId'")
                                .category("http-action")
                                .objectType("WorkflowNode")
                                .objectId((String) node.get(FIELD_ID))
                                .log();
                        }
                        
                        logger.debug()
                            .message("Applied context mapping for HTTP action")
                            .category("http-action")
                            .objectType("WorkflowNode")
                            .objectId((String) node.get(FIELD_ID))
                            .detail("targetKey", targetKey)
                            .detail("sourceKey", sourceKey)
                            .detail("valueType", sourceValue != null ? sourceValue.getClass().getSimpleName() : "null")
                            .log();
                    } else {
                        logger.warn()
                            .message("Context mapping source key not found")
                            .category("http-action")
                            .objectType("WorkflowNode")
                            .objectId((String) node.get(FIELD_ID))
                            .detail("targetKey", targetKey)
                            .detail("sourceKey", sourceKey)
                            .log();
                    }
                }
            }
            
            // ============================================================================
            // JWT Token Refresh: Replace expired tokens before HTTP call
            // If the workflow has been paused for approval (or any other reason) and the
            // JWT token has expired, generate a fresh service token to prevent 401 errors
            // ============================================================================
            if (effectiveContext.containsKey("jwtToken")) {
                // If effectiveContext is still pointing to original context, make a copy now
                if (effectiveContext == context) {
                    effectiveContext = new HashMap<>(context);
                }
                
                String currentToken = (String) effectiveContext.get("jwtToken");
                if (currentToken != null && !currentToken.isEmpty()) {
                    // Check if token is expired or invalid
                    if (jwtUtils.isTokenExpired(currentToken)) {
                        // Token is expired - generate fresh service token
                        String tenantId = (String) effectiveContext.get("tenantId");
                        String userId = (String) effectiveContext.get("userId");
                        String username = (String) effectiveContext.get("triggeredBy"); // Use triggeredBy as username
                        
                        if (tenantId != null && userId != null && username != null) {
                            try {
                                String freshToken = jwtUtils.generateServiceToken(tenantId, userId, username);
                                effectiveContext.put("jwtToken", freshToken);
                                
                                logger.info()
                                    .message("Refreshed expired JWT token for HTTP action")
                                    .category("http-action")
                                    .objectType("WorkflowNode")
                                    .objectId((String) node.get(FIELD_ID))
                                    .detail("executionId", execution.getId().toString())
                                    .log();
                            } catch (Exception e) {
                                logger.warn()
                                    .message("Failed to refresh JWT token, will use expired token (may fail)")
                                    .category("http-action")
                                    .objectType("WorkflowNode")
                                    .objectId((String) node.get(FIELD_ID))
                                    .detail("error", e.getMessage())
                                    .log();
                                // Continue with expired token - let the HTTP call fail with proper error
                            }
                        } else {
                            logger.warn()
                                .message("Cannot refresh JWT token - missing required context values")
                                .category("http-action")
                                .objectType("WorkflowNode")
                                .objectId((String) node.get(FIELD_ID))
                                .detail("hasTenantId", tenantId != null)
                                .detail("hasUserId", userId != null)
                                .detail("hasUsername", username != null)
                                .log();
                        }
                    }
                }
            }
            
            logger.trace()
                .message(LOG_EXECUTE_ACTION_HTTP_PROCESSING_DIRECTLY)
                .category("http-action")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .log();
            
            // Get HTTP method (required)
            String method = (String) nodeData.get("method");
            if (method == null || method.isEmpty()) {
                throw new IllegalArgumentException("HTTP method is required");
            }
            method = method.toUpperCase();
            
            // Get URL with template substitution (required)
            String urlTemplate = (String) nodeData.get("url");
            if (urlTemplate == null || urlTemplate.isEmpty()) {
                throw new IllegalArgumentException("URL is required");
            }
            String url = substituteTemplateVariables(urlTemplate, effectiveContext);
            
            // Get headers with template substitution (optional)
            @SuppressWarnings("unchecked")
            Map<String, Object> headersConfig = (Map<String, Object>) nodeData.get("headers");
            Map<String, String> headers = new HashMap<>();
            if (headersConfig != null) {
                for (Map.Entry<String, Object> entry : headersConfig.entrySet()) {
                    String value = substituteTemplateVariables(
                        entry.getValue().toString(), effectiveContext
                    );
                    headers.put(entry.getKey(), value);
                }
            }
            
            logger.trace()
                .message(LOG_TRACE_HTTP_REQUEST_PREPARED)
                .category("http-action")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("method", method)
                .detail("url", url)
                .detail("headerCount", headers.size())
                .log();
            
            // Get body with template substitution (optional)
            Object bodyConfig = nodeData.get("body");
            String body = null;
            if (bodyConfig != null) {
                if (bodyConfig instanceof String) {
                    body = substituteTemplateVariables((String) bodyConfig, effectiveContext);
                } else if (bodyConfig instanceof Map) {
                    // If body is Map, substitute variables in Map BEFORE serializing to preserve types (arrays, objects)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bodyMap = (Map<String, Object>) bodyConfig;
                    Map<String, Object> substitutedBodyMap = substituteMapVariables(bodyMap, effectiveContext);
                    body = objectMapper.writeValueAsString(substitutedBodyMap);
                } else {
                    // For other types, convert to JSON then substitute
                    String jsonBody = objectMapper.writeValueAsString(bodyConfig);
                    body = substituteTemplateVariables(jsonBody, effectiveContext);
                }
            }
            
            // If simulation mode, return mock response without making actual HTTP call
            if (Boolean.TRUE.equals(simulationMode)) {
                logger.info()
                    .message("Simulation mode: skipping HTTP request")
                    .category("http-action")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("method", method)
                    .detail("url", url)
                    .log();
                
                return Map.of(
                    CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_HTTP,
                    CONTEXT_KEY_EXECUTED, true,
                    CONTEXT_KEY_SIMULATED, true,
                    "wouldCallUrl", url,
                    "wouldUseMethod", method,
                    "wouldSendHeaders", headers,
                    "wouldSendBody", body != null ? "body present" : "no body",
                    CONTEXT_KEY_STATUS, "would-make-http-request",
                    "simulatedResponse", Map.of(
                        "statusCode", 200,
                        "success", true,
                        "message", "Simulated success response"
                    ),
                    CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis()
                );
            }
            
            // Get timeout (optional, default 30 seconds)
            Integer timeout = nodeData.get("timeout") != null ? 
                ((Number) nodeData.get("timeout")).intValue() : 30000;
            
            // Get retries (optional, default 3)
            Integer retries = nodeData.get("retries") != null ? 
                ((Number) nodeData.get("retries")).intValue() : 3;
            
            // Get success codes (optional, default [200, 201, 204])
            @SuppressWarnings("unchecked")
            List<Integer> successCodes = nodeData.get("successCodes") != null ?
                (List<Integer>) nodeData.get("successCodes") : 
                List.of(200, 201, 204);
            
            // Add authentication if configured
            String authType = (String) nodeData.get("authType");
            if ("bearer".equalsIgnoreCase(authType)) {
                String token = (String) nodeData.get("bearerToken");
                if (token != null) {
                    token = substituteTemplateVariables(token, context);
                    headers.put("Authorization", "Bearer " + token);
                }
            } else if ("basic".equalsIgnoreCase(authType)) {
                String username = (String) nodeData.get("username");
                String password = (String) nodeData.get("password");
                if (username != null && password != null) {
                    username = substituteTemplateVariables(username, context);
                    password = substituteTemplateVariables(password, context);
                    String auth = Base64.getEncoder().encodeToString(
                        (username + ":" + password).getBytes()
                    );
                    headers.put("Authorization", "Basic " + auth);
                }
            } else if ("apiKey".equalsIgnoreCase(authType)) {
                String apiKeyHeader = (String) nodeData.get("apiKeyHeader");
                String apiKeyValue = (String) nodeData.get("apiKeyValue");
                if (apiKeyHeader != null && apiKeyValue != null) {
                    apiKeyValue = substituteTemplateVariables(apiKeyValue, context);
                    headers.put(apiKeyHeader, apiKeyValue);
                }
            } else {
                // No explicit auth configured - check if JWT token is available in context
                // This automatically adds authentication to all HTTP requests without requiring configuration
                // IMPORTANT: Use effectiveContext (not context) to get potentially refreshed token
                if (!headers.containsKey("Authorization") && effectiveContext.containsKey("jwtToken")) {
                    String jwtToken = effectiveContext.get("jwtToken").toString();
                    headers.put("Authorization", "Bearer " + jwtToken);
                    logger.trace()
                        .message(LOG_AUTO_ADDED_JWT_TO_AUTHORIZATION)
                        .category("http-action")
                        .objectType("WorkflowNode")
                        .objectId((String) node.get(FIELD_ID))
                        .log();
                }
            }
            
            // Add tenant context if not already present
            if (!headers.containsKey("X-Tenant-ID") && effectiveContext.containsKey("tenantId")) {
                headers.put("X-Tenant-ID", effectiveContext.get("tenantId").toString());
            }

            // Propagate workflow execution context to downstream services.
            // This allows services to mark emitted events as workflow-origin and prevents loops.
            if (!headers.containsKey(HEADER_WORKFLOW_EXECUTION_ID)
                && execution != null
                && execution.getId() != null) {
                headers.put(HEADER_WORKFLOW_EXECUTION_ID, execution.getId().toString());
            }
            
            // Set default Content-Type if body is present and not set
            if (body != null && !headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/json");
            }
            
            logger.info()
                .message(LOG_EXECUTING_HTTP_REQUEST)
                .category("http-action")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("method", method)
                .detail("url", url)
                .detail("requestBody", body != null ? body : "null")
                .log();
            
            // Execute HTTP request with retry logic
            HttpResponse response = executeWithRetry(
                method, url, headers, body, timeout, retries
            );
            
            logger.trace()
                .message(LOG_TRACE_HTTP_RESPONSE_RECEIVED)
                .category("http-action")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("statusCode", response.statusCode)
                .detail("hasBody", response.body != null && !response.body.isEmpty())
                .log();
            
            // Check if response is successful
            boolean success = successCodes.contains(response.getStatusCode());
            
            // Parse response body
            Object responseBody = null;
            try {
                if (response.getBody() != null && !response.getBody().isEmpty()) {
                    responseBody = objectMapper.readValue(
                        response.getBody(), Object.class
                    );
                }
            } catch (Exception e) {
                // If not JSON, return as string
                responseBody = response.getBody();
            }
            
            // Build output
            Map<String, Object> output = new HashMap<>();
            output.put(CONTEXT_KEY_NODE_TYPE, NODE_TYPE_ACTION_HTTP);
            output.put(CONTEXT_KEY_EXECUTED, true);
            output.put(CONTEXT_KEY_HTTP_METHOD, method);
            output.put(CONTEXT_KEY_HTTP_URL, url);
            output.put(CONTEXT_KEY_HTTP_STATUS, response.getStatusCode());
            output.put(CONTEXT_KEY_HTTP_RESPONSE, responseBody);
            output.put(CONTEXT_KEY_HTTP_SUCCESS, success);
            output.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());
            
            if (!success) {
                String errorMsg = "HTTP request failed with status: " + response.getStatusCode();
                output.put(CONTEXT_KEY_HTTP_ERROR, errorMsg);
                logger.error()
                    .message(LOG_HTTP_ACTION_FAILED)
                    .category("http-action")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("method", method)
                    .detail("url", url)
                    .detail("statusCode", response.getStatusCode())
                    .log();

                // --- PUBLISH PROVISIONING FAILED EVENT ---
                if (isAccessPortalWorkflow) {
                try {

                    if (accessPortalEventPublisher != null) {
                        String roleId = context.getOrDefault("roleId", "").toString();

                        @SuppressWarnings("unchecked")
                        List<String> approvedUserIds = (List<String>) context.getOrDefault("approvedUserIds", List.of());

                        accessPortalEventPublisher.publishRoleAssignmentFailed(
                            execution.getId(),
                            roleId,
                            approvedUserIds,
                            errorMsg + ". Response: " + responseBody,
                            context
                        );

                        logger.info()
                            .message("Published role.assignment.failed event for HTTP action")
                            .category("provisioning-events")
                            .detail("executionId", execution.getId().toString())
                            .detail("statusCode", response.getStatusCode())
                            .log();
                    }
                } catch (Exception pubEx) {
                    logger.warn()
                        .message("Failed to publish role.assignment.failed event")
                        .detail("error", pubEx.getMessage())
                        .log();
                }
            }
            // --- END PROVISIONING FAILED EVENT ---

                // Throw exception to fail the workflow execution
                throw new RuntimeException(errorMsg + ". Response: " + responseBody);
            } else {
                logger.info()
                    .message(LOG_HTTP_ACTION_SUCCEEDED)
                    .category("http-action")
                    .objectType("WorkflowNode")
                    .objectId((String) node.get(FIELD_ID))
                    .detail("method", method)
                    .detail("url", url)
                    .detail("statusCode", response.getStatusCode())
                    .log();

                 // --- PUBLISH PROVISIONING COMPLETED EVENT ---
            if (isAccessPortalWorkflow) {
                try {
                    if (accessPortalEventPublisher!= null) {
                        String roleId = context.getOrDefault("roleId", "").toString();

                        // Get approved user IDs from context or extract from response
                        @SuppressWarnings("unchecked")
                        List<String> approvedUserIds = (List<String>) context.getOrDefault("approvedUserIds", List.of());

                        // If no approvedUserIds, try to extract from response body
                        if (approvedUserIds.isEmpty() && responseBody != null) {
                            if (responseBody instanceof Map) {
                                Map<?, ?> responseMap = (Map<?, ?>) responseBody;
                                if (responseMap.containsKey("userId")) {
                                    approvedUserIds = List.of(responseMap.get("userId").toString());
                                } else if (responseMap.containsKey("userIds")) {
                                    Object userIdsObj = responseMap.get("userIds");
                                    if (userIdsObj instanceof List) {
                                        approvedUserIds = (List<String>) userIdsObj;
                                    }
                                }
                            } else if (responseBody instanceof String) {
                                // Assume the response body itself is the user ID
                                approvedUserIds = List.of(responseBody.toString());
                            }
                        }

                        if (!approvedUserIds.isEmpty()) {
                            accessPortalEventPublisher.publishRoleAssigned(
                                execution.getId(),
                                roleId,
                                approvedUserIds,
                                context
                            );

                            logger.info()
                                .message("Published role.assigned event for HTTP action")
                                .category("provisioning-events")
                                .detail("executionId", execution.getId().toString())
                                .detail("roleId", roleId)
                                .detail("userCount", approvedUserIds.size())
                                .log();
                        }
                    }
                } catch (Exception e) {
                    logger.warn()
                        .message("Failed to publish role.assigned event")
                        .detail("error", e.getMessage())
                        .log();
                }
            }
            // --- END PROVISIONING COMPLETED EVENT ---
            }


            
            return output;
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_HTTP_ACTION_EXECUTION_FAILED)
                .category("http-action")
                .objectType("WorkflowNode")
                .objectId((String) node.get(FIELD_ID))
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            // Re-throw to fail the workflow execution
            throw new RuntimeException("HTTP action execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Substitute template variables in string
     * Supports: {{context.fieldName}}, {{fieldName}}, {{context.nested.field}}
     * Example: "https://api.com/roles/{{context.roleId}}" → "https://api.com/roles/123"
     */
    /**
     * Substitute template variables in a Map, preserving data types (arrays, objects, etc.)
     */
    private Map<String, Object> substituteMapVariables(Map<String, Object> template, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String strValue = (String) value;
                // Check if the entire value is a single template variable without quotes
                Pattern singleVarPattern = Pattern.compile("^\\{\\{(context\\.)?([^}]+)\\}\\}$");
                Matcher singleVarMatcher = singleVarPattern.matcher(strValue);
                
                if (singleVarMatcher.matches()) {
                    // Entire value is a template variable - use the actual object value
                    String fieldPath = singleVarMatcher.group(2);
                    Object contextValue = getNestedValue(context, fieldPath);
                    result.put(key, contextValue != null ? contextValue : null);
                } else {
                    // String contains template variables - substitute as string
                    result.put(key, substituteTemplateVariables(strValue, context));
                }
            } else if (value instanceof Map) {
                // Recursively handle nested maps
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.put(key, substituteMapVariables(nestedMap, context));
            } else {
                // Keep other values as-is
                result.put(key, value);
            }
        }
        
        return result;
    }

    private String substituteTemplateVariables(String template, Map<String, Object> context) {
        if (template == null) return null;
        
        String result = template;
        
        // Match {{context.fieldName}} or {{fieldName}}
        Pattern pattern = Pattern.compile("\\{\\{(context\\.)?([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            String fieldPath = matcher.group(2);
            Object value = getNestedValue(context, fieldPath);
            
            if (value != null) {
                logger.trace()
                    .message(LOG_TRACE_TEMPLATE_VARIABLE_REPLACED)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("variable", matcher.group(0))
                    .detail("value", value.toString())
                    .log();
                
                result = result.replace(matcher.group(0), value.toString());
            } else {
                logger.warn()
                    .message(LOG_TEMPLATE_VARIABLE_NOT_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("fieldPath", fieldPath)
                    .log();
            }
        }
        
        return result;
    }

    /**
     * Get nested value from context using dot notation
     * Example: "user.email" → context.get("user").get("email")
     */
    private Object getNestedValue(Map<String, Object> context, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }

    /**
     * Execute HTTP request with exponential backoff retry
     */
    private HttpResponse executeWithRetry(
        String method, String url, Map<String, String> headers, 
        String body, int timeout, int maxRetries
    ) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                // Build request
                HttpHeaders httpHeaders = new HttpHeaders();
                headers.forEach(httpHeaders::set);
                
                HttpEntity<String> entity = body != null ? 
                    new HttpEntity<>(body, httpHeaders) : 
                    new HttpEntity<>(httpHeaders);
                
                // Configure timeout
                RestTemplate restTemplate = new RestTemplate();
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(timeout);
                factory.setReadTimeout(timeout);
                restTemplate.setRequestFactory(factory);
                
                // Execute request
                ResponseEntity<String> response = restTemplate.exchange(
                    url, 
                    HttpMethod.valueOf(method), 
                    entity, 
                    String.class
                );
                
                return new HttpResponse(response.getStatusCode().value(), response.getBody());
                
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                // HTTP error response received (4xx, 5xx)
                return new HttpResponse(e.getStatusCode().value(), e.getResponseBodyAsString());
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s, 8s...
                    int delayMs = (int) (1000 * Math.pow(2, attempt - 1));
                    logger.warn()
                        .message(LOG_HTTP_REQUEST_FAILED_RETRYING)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .detail("attempt", String.valueOf(attempt))
                        .detail("maxRetries", String.valueOf(maxRetries))
                        .detail("delayMs", String.valueOf(delayMs))
                        .detail("error", e.getMessage())
                        .log();
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted
        throw new RuntimeException("HTTP request failed after " + maxRetries + 
                                   " retries: " + lastException.getMessage(), 
                                   lastException);
    }

    /**
     * Simple HTTP response holder
     */
    @Data
    @AllArgsConstructor
    private static class HttpResponse {
        private int statusCode;
        private String body;
    }

    // ==================== TEMPLATE-BASED ACTIONS ====================
    
    /**
     * Generic executor for template-based actions (action.role.activate, action.user.create, etc.)
     * 
     * Templates are loaded from action-templates.json at startup
     * No code changes needed to add new actions - just update config file!
     * 
     * Example template in config:
     * {
     *   "type": "action.role.activate",
     *   "template": {
     *     "method": "PUT",
     *     "urlTemplate": "http://service/api/v1/roles/{{roleId}}/activate",
     *     "headersTemplate": {"X-Tenant-ID": "{{tenantId}}"},
     *     "bodyTemplate": {"activatedBy": "{{approver}}"}
     *   }
     * }
     * 
     * At runtime:
     * - Template loaded from ActionTemplateService
     * - Variables replaced from context: {{roleId}} → actual value
     * - HTTP call executed via executeActionHttp()
     */
    private Map<String, Object> executeTemplatedAction(
        Map<String, Object> node, 
        Map<String, Object> context, 
        WorkflowExecution execution
    ) {
        // Read node type from data.stepType
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get(FIELD_DATA);
        String nodeType = nodeData != null && nodeData.containsKey("stepType") ? 
            (String) nodeData.get("stepType") : 
            (String) node.get("type");
        logger.info()
            .message(LOG_EXECUTING_TEMPLATE_ACTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowNode")
            .objectId((String) node.get(FIELD_ID))
            .detail("nodeType", nodeType)
            .log();
        
        // Check if node has template configuration embedded
        Object templateObj = node.get("template");
        
        // Check if template is specified in config (for action.http with template dropdown)
        Object configObj = node.get("config");
        if (configObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) configObj;
            Object configTemplateType = config.get("template");
            
            if (configTemplateType instanceof String && !((String) configTemplateType).isEmpty()) {
                // Load template from service using template type from config
                logger.info()
                    .message(LOG_LOADING_CONFIG_TEMPLATE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("configTemplateType", (String) configTemplateType)
                    .log();
                node = actionTemplateService.applyTemplate((String) configTemplateType, node);
                templateObj = node.get("template");
            }
        }
        
        // If still no template in node, try loading from template service using node type
        if (templateObj == null && nodeType != null) {
            node = actionTemplateService.applyTemplate(nodeType, node);
            templateObj = node.get("template");
        }
        
        if (templateObj == null) {
            // Fallback: treat as direct HTTP node
            return executeActionHttp(node, context, execution);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) templateObj;
        
        // Build HTTP config from template with context substitution
        Map<String, Object> httpConfig = new HashMap<>();
        
        // Method
        httpConfig.put("method", template.getOrDefault("method", "GET"));
        
        // URL with template substitution
        String urlTemplate = (String) template.get("urlTemplate");
        if (urlTemplate != null) {
            httpConfig.put("url", substituteTemplateVariables(urlTemplate, context));
        }
        
        // Headers with template substitution
        // executeActionHttp expects headers as Map<String, Object>, NOT JSON string
        Object headersTemplateObj = template.get("headersTemplate");
        if (headersTemplateObj != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> headersTemplate = (Map<String, String>) headersTemplateObj;
                Map<String, Object> headers = new HashMap<>();
                for (Map.Entry<String, String> entry : headersTemplate.entrySet()) {
                    headers.put(entry.getKey(), substituteTemplateVariables(entry.getValue(), context));
                }
                httpConfig.put("headers", headers);  // Put Map directly, not JSON string
            } catch (Exception e) {
                logger.error()
                    .message(LOG_FAILED_TO_PROCESS_HEADERS_TEMPLATE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        // Body with template substitution
        // executeActionHttp expects body as String or Object (Map)
        Object bodyTemplateObj = template.get("bodyTemplate");
        if (bodyTemplateObj != null) {
            try {
                // Substitute variables in the Map BEFORE serializing to JSON
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyTemplate = (Map<String, Object>) bodyTemplateObj;
                Map<String, Object> substitutedBodyMap = substituteMapVariables(bodyTemplate, context);
                // Now serialize the substituted Map to JSON
                String bodyJson = objectMapper.writeValueAsString(substitutedBodyMap);
                httpConfig.put("body", bodyJson);
            } catch (Exception e) {
                logger.error()
                    .message(LOG_FAILED_TO_PROCESS_BODY_TEMPLATE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        // Timeout and retries
        httpConfig.put("timeout", template.getOrDefault("timeout", 30000));
        httpConfig.put("retries", template.getOrDefault("retries", 3));
        
        // CRITICAL: httpConfig must NOT contain "template" field
        // If template map itself has a "template" field, explicitly ensure it's not copied
        httpConfig.remove("template");
        
        // Log every key in httpConfig to diagnose issue
        logger.info()
            .message(LOG_HTTPCONFIG_KEYS_BEFORE_MARKER)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("keys", httpConfig.keySet().toString())
            .log();
        logger.info()
            .message(LOG_HTTPCONFIG_CONTAINS_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("containsTemplate", String.valueOf(httpConfig.containsKey("template")))
            .log();
        
        // CRITICAL: Add skip marker FIRST before any other operations
        httpConfig.put("__skipTemplateCheck", true);
        
        // Log again after marker
        logger.info()
            .message(LOG_HTTPCONFIG_AFTER_MARKER)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("containsSkipTemplateCheck", String.valueOf(httpConfig.containsKey("__skipTemplateCheck")))
            .detail("containsTemplate", String.valueOf(httpConfig.containsKey("template")))
            .log();
        
        Map<String, Object> nodeWithConfig = new HashMap<>();
        nodeWithConfig.put("id", node.get("id"));
        nodeWithConfig.put("type", "action.http");  // Use standard HTTP type
        nodeWithConfig.put("data", httpConfig);     // httpConfig with skip marker
        
        logger.info()
            .message(LOG_DELEGATING_TO_EXECUTE_ACTION_HTTP)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .log();
        
        // Delegate to generic HTTP executor (will skip template check due to marker)
        return executeActionHttp(nodeWithConfig, context, execution);
    }
}
