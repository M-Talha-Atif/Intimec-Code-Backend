package com.aidentx.workflowsservice.audit;

import com.aidentx.workflowsservice.dto.response.WorkflowResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowExecutionDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.workflowsservice.util.ResponseDataSanitizer;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * AOP Aspect for automatic workflow audit logging.
 * Intercepts service methods and publishes audit events.
 */
@Aspect
@Component
public class WorkflowAuditAspect {

    private static final String LOG_CATEGORY = LOG_CATEGORY_WORKFLOW_AUDIT;
    
    private final AuditEventPublisher auditPublisher;
    private final ObjectMapper objectMapper;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final JwtUtils jwtUtils;
    private final IUnifiedLogger logger;

    public WorkflowAuditAspect(
            AuditEventPublisher auditPublisher,
            ObjectMapper objectMapper,
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository workflowExecutionRepository,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
        this.workflowRepository = workflowRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger("WorkflowAuditAspect", LOG_CATEGORY);
        logger.info().message(LOG_WORKFLOW_AUDIT_ASPECT_INITIALIZED).log();
    }
    
    /**
     * Extract tenant ID from the current HTTP request.
     */
    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return jwtUtils.extractTenantId(request);
            }
            logger.error()
                .category(LOG_CATEGORY)
                .message(LOG_NO_HTTP_CONTEXT_FOR_TENANT)
                .log();
            throw new IllegalStateException("Cannot extract tenant ID - no HTTP context available");
        } catch (Exception e) {
            logger.error()
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_EXTRACT_TENANT_FROM_REQUEST)
                .detail("error", e.getMessage())
                .log();
            throw new IllegalStateException("Failed to extract tenant ID: " + e.getMessage(), e);
        }
    }

    /**
     * Extract current user ID from the current HTTP request.
     */
    private UUID getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = jwtUtils.extractUserId(request);
                if (userId != null) {
                    return UUID.fromString(userId);
                }
            }
        } catch (Exception e) {
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_COULD_NOT_EXTRACT_USER_ID)
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Extract current username from the current HTTP request.
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_ATTEMPTING_EXTRACT_USERNAME)
                .detail("hasAttributes", attributes != null)
                .log();
                
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Log all headers for debugging
                String xUsername = request.getHeader("X-Username");
                String authHeader = request.getHeader("Authorization");
                logger.debug()
                    .category(LOG_CATEGORY)
                    .message(LOG_REQUEST_HEADERS_CHECK)
                    .detail("X-Username", xUsername != null ? xUsername : "NULL")
                    .detail("hasAuthorization", authHeader != null)
                    .log();
                
                String username = jwtUtils.extractUsername(request);
                logger.debug()
                    .category(LOG_CATEGORY)
                    .message(LOG_USERNAME_EXTRACTION_RESULT)
                    .detail("username", username != null ? username : "NULL")
                    .log();
                    
                if (username != null) {
                    return username;
                }
            } else {
                logger.warn()
                    .category(LOG_CATEGORY)
                    .message(LOG_HTTP_CONTEXT_LOST)
                    .log();
            }
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_ERROR_EXTRACTING_USERNAME)
                .log();
        }
        return null;
    }

    /**
     * Extract client IP from the current HTTP request.
     */
    private String getCurrentClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_COULD_NOT_EXTRACT_CLIENT_IP)
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Extract user agent from the current HTTP request.
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_COULD_NOT_EXTRACT_USER_AGENT)
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Audit workflow creation.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowService.createWorkflow(..))",
        returning = "result"
    )
    public void auditWorkflowCreated(JoinPoint joinPoint, Object result) {
        logger.debug()
            .category(LOG_CATEGORY)
            .message(LOG_ASPECT_TRIGGERED_CREATED)
            .log();
        try {
            if (result == null) {
                logger.warn()
                    .category(LOG_CATEGORY)
                    .message(LOG_RESULT_NULL_CANNOT_AUDIT_CREATION)
                    .log();
                return;
            }
            
            // The service returns WorkflowResponseDTO, extract the ID and fetch the entity
            WorkflowResponseDTO dto = (WorkflowResponseDTO) result;
            UUID workflowId = dto.getId();
            
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
            
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_FOUND_WORKFLOW_ENTITY_FOR_AUDIT)
                .objectType("workflow")
                .objectId(workflow.getId().toString())
                .log();
                
            JsonNode stateAfter = sanitizeForAudit(objectMapper.valueToTree(workflow));
            
            UUID actorId = getCurrentUserId();
            String actorName = getCurrentUsername();
            WorkflowAuditEvent event = actorId != null 
                ? WorkflowAuditEvent.user(
                    actorId,
                    actorName != null ? actorName : actorId.toString(),
                    getCurrentTenantId(),
                    workflow.getId(),
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_CREATED,
                    null,
                    stateAfter,
                    getCurrentClientIp(),
                    getCurrentUserAgent()
                )
                : WorkflowAuditEvent.system(
                    getCurrentTenantId(),
                    workflow.getId(),
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_CREATED,
                    null,
                    stateAfter
                );
            
            auditPublisher.publishEvent(event);
            
            logger.info()
                .category(LOG_CATEGORY)
                .message(LOG_PUBLISHED_WORKFLOW_CREATED_AUDIT)
                .objectType("workflow")
                .objectId(workflow.getId().toString())
                .detail("action", "workflow-created")
                .log();
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_WORKFLOW_CREATED_AUDIT)
                .log();
        }
    }

    /**
     * Audit workflow update.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowService.updateWorkflow(..))",
        returning = "result"
    )
    public void auditWorkflowUpdated(JoinPoint joinPoint, Object result) {
        try {
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_ASPECT_TRIGGERED_UPDATED)
                .detail("resultType", result.getClass().getName())
                .log();
            
            WorkflowResponseDTO dto = (WorkflowResponseDTO) result;
            UUID workflowId = dto.getId();
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
            
            JsonNode stateAfter = sanitizeForAudit(objectMapper.valueToTree(workflow));
            
            UUID actorId = getCurrentUserId();
            String actorName = getCurrentUsername();
            WorkflowAuditEvent event = actorId != null 
                ? WorkflowAuditEvent.user(
                    actorId,
                    actorName != null ? actorName : actorId.toString(),
                    getCurrentTenantId(),
                    workflow.getId(),
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_UPDATED,
                    null,
                    stateAfter,
                    getCurrentClientIp(),
                    getCurrentUserAgent()
                )
                : WorkflowAuditEvent.system(
                    getCurrentTenantId(),
                    workflow.getId(),
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_UPDATED,
                    null,
                    stateAfter
                );
            
            auditPublisher.publishEvent(event);
            
            logger.info()
                .category(LOG_CATEGORY)
                .message(LOG_PUBLISHED_WORKFLOW_UPDATED_AUDIT)
                .objectType("workflow")
                .objectId(workflow.getId().toString())
                .detail("action", "workflow-updated")
                .log();
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_WORKFLOW_UPDATED_AUDIT)
                .log();
        }
    }

    /**
     * Audit workflow deletion.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowService.deleteWorkflow(..)) && args(id)",
        argNames = "id"
    )
    public void auditWorkflowDeleted(UUID id) {
        try {
            UUID actorId = getCurrentUserId();
            String actorName = getCurrentUsername();
            WorkflowAuditEvent event = actorId != null 
                ? WorkflowAuditEvent.user(
                    actorId,
                    actorName != null ? actorName : actorId.toString(),
                    getCurrentTenantId(),
                    id,
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_DELETED,
                    null,
                    null,
                    getCurrentClientIp(),
                    getCurrentUserAgent()
                )
                : WorkflowAuditEvent.system(
                    getCurrentTenantId(),
                    id,
                    OBJECT_TYPE_WORKFLOW,
                    AUDIT_ACTION_WORKFLOW_DELETED,
                    null,
                    null
                );
            
            auditPublisher.publishEvent(event);
            
            logger.info()
                .category(LOG_CATEGORY)
                .message(LOG_PUBLISHED_WORKFLOW_DELETED_AUDIT)
                .objectType("workflow")
                .objectId(id.toString())
                .detail("action", "workflow-deleted")
                .log();
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_WORKFLOW_DELETED_AUDIT)
                .objectId(id.toString())
                .log();
        }
    }

    /**
     * Audit workflow execution start.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowExecutionService.executeWorkflow(..))",
        returning = "result"
    )
    public void auditWorkflowExecuted(JoinPoint joinPoint, Object result) {
        try {
            logger.debug()
                .category(LOG_CATEGORY)
                .message(LOG_ASPECT_TRIGGERED_EXECUTED)
                .detail("resultType", result.getClass().getName())
                .log();
            
            WorkflowExecutionDTO dto = (WorkflowExecutionDTO) result;
            UUID executionId = dto.getId();
            WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("WorkflowExecution not found: " + executionId));
            
            // Fetch workflow to get name
            Workflow workflow = execution.getWorkflowId() != null
                ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                : null;
            String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
            
            // Create enriched stateAfter with workflow context for better object name extraction
            ObjectNode enrichedState = objectMapper.valueToTree(execution);
            if (workflowName != null) {
                enrichedState.put("workflowName", workflowName);
            }
            if (workflow != null) {
                enrichedState.set("workflow", objectMapper.valueToTree(workflow));
            }
            // Sanitize to remove sensitive data before audit logging
            JsonNode stateAfter = sanitizeForAudit(enrichedState);
            
            // Determine actor information
            // For event-triggered workflows, get actor from execution context
            // For manual workflows, get from HTTP context
            UUID actorId = null;
            String actorName = null;
            
            Map<String, Object> execContext = execution.getExecutionContext();
            if (execContext != null && execContext.containsKey("createdById")) {
                // Event-triggered workflow - get user from execution context
                try {
                    actorId = UUID.fromString((String) execContext.get("createdById"));
                    actorName = execContext.containsKey("createdBy") ? 
                               (String) execContext.get("createdBy") : null;
                    logger.debug()
                        .category(LOG_CATEGORY)
                        .message(LOG_USING_ACTOR_FROM_EXECUTION_CONTEXT)
                        .detail("actorId", actorId.toString())
                        .detail("actorName", actorName)
                        .log();
                } catch (Exception e) {
                    logger.debug()
                        .category(LOG_CATEGORY)
                        .message(LOG_COULD_NOT_EXTRACT_CREATOR_ID)
                        .log();
                }
            }
            
            // Fallback to HTTP context for manual workflows
            if (actorId == null) {
                actorId = getCurrentUserId();
                actorName = getCurrentUsername();
            }
            
            // Get tenantId - ALWAYS prefer execution context (for event-triggered workflows)
            String tenantId = null;
            
            // Check execution context FIRST (event-triggered workflows store tenantId here)
            if (execContext != null && execContext.containsKey("tenantId")) {
                tenantId = (String) execContext.get("tenantId");
                logger.debug()
                    .category(LOG_CATEGORY)
                    .message(LOG_EXTRACTED_TENANT_FROM_EXECUTION)
                    .detail("tenantId", tenantId)
                    .detail("executionId", execution.getId().toString())
                    .log();
            } 
            
            // Fallback to HTTP request context for manual workflows
            if (tenantId == null) {
                tenantId = getCurrentTenantId();
                logger.debug()
                    .category(LOG_CATEGORY)
                    .message(LOG_EXTRACTED_TENANT_FROM_HTTP_FALLBACK)
                    .detail("tenantId", tenantId)
                    .detail("executionId", execution.getId().toString())
                    .log();
            }
            
            final String finalTenantId = tenantId;
            
            // Check if this is a simulation execution
            boolean isSimulation = execution.getExecutionContext() != null && 
                Boolean.TRUE.equals(execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE));
            String auditAction = isSimulation ? AUDIT_ACTION_WORKFLOW_SIMULATED : AUDIT_ACTION_WORKFLOW_EXECUTED;
            
            logger.info()
                .category(LOG_CATEGORY)
                .message(LOG_PUBLISHING_WORKFLOW_EXECUTION_AUDIT)
                .detail("finalTenantId", finalTenantId)
                .detail("executionId", execution.getId().toString())
                .detail("isSimulation", isSimulation)
                .detail("auditAction", auditAction)
                .log();
            
            WorkflowAuditEvent event = actorId != null
                ? WorkflowAuditEvent.user(
                    actorId,
                    actorName != null ? actorName : actorId.toString(),
                    finalTenantId,
                    execution.getId(),
                    OBJECT_TYPE_WORKFLOW_EXECUTION, // Use consistent object type for filtering
                    auditAction, // Use workflow-simulated for simulations, workflow-executed for normal
                    null,
                    stateAfter,
                    getCurrentClientIp(),
                    getCurrentUserAgent()
                )
                : WorkflowAuditEvent.system(
                    finalTenantId,
                    execution.getId(),
                    OBJECT_TYPE_WORKFLOW_EXECUTION, // Use consistent object type for filtering
                    auditAction, // Use workflow-simulated for simulations, workflow-executed for normal
                    null,
                    stateAfter
                );
            
            auditPublisher.publishEvent(event);
            
            logger.info()
                .category(LOG_CATEGORY)
                .message(LOG_PUBLISHED_WORKFLOW_EXECUTED_AUDIT)
                .objectType("workflow-execution")
                .objectId(execution.getId().toString())
                .detail("action", auditAction)
                .detail("isSimulation", isSimulation)
                .detail("triggeredBy", execution.getTriggeredBy().toString())
                .log();
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_WORKFLOW_EXECUTED_AUDIT)
                .log();
        }
    }

    /**
     * Audit approval created.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowApprovalService.createApprovalRequest(..))",
        returning = "result"
    )
    public void auditApprovalCreated(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof WorkflowApproval approval) {
                // Fetch execution to get workflow name and full context
                WorkflowExecution execution = approval.getExecutionId() != null
                    ? workflowExecutionRepository.findById(approval.getExecutionId()).orElse(null)
                    : null;
                
                // Skip audit logging for simulations
                if (execution != null && execution.getExecutionContext() != null && 
                    Boolean.TRUE.equals(execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE))) {
                    logger.debug()
                        .category(LOG_CATEGORY)
                        .message("Skipping approval-created audit for simulation")
                        .detail("approvalId", approval.getId().toString())
                        .detail("executionId", execution.getId().toString())
                        .log();
                    return;
                }
                Workflow workflow = execution != null && execution.getWorkflowId() != null
                    ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                    : null;
                String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
                
                // Create enriched stateAfter with workflow context for better object name extraction
                ObjectNode enrichedState = objectMapper.valueToTree(approval);
                if (workflowName != null) {
                    enrichedState.put("workflowName", workflowName);
                }
                if (workflow != null) {
                    enrichedState.set("workflow", objectMapper.valueToTree(workflow));
                }
                
                // Extract tenant ID from approval context (contains execution context with tenantId)
                String tenantId = null;
                if (approval.getApprovalContext() != null && approval.getApprovalContext().containsKey("tenantId")) {
                    tenantId = (String) approval.getApprovalContext().get("tenantId");
                }
                
                // Fallback to getCurrentTenantId if not in context (manual approvals via API)
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    tenantId = getCurrentTenantId();
                }
                
                // Actor is the person who CREATED/SENT the approval request (workflow initiator)
                // Get from execution (same as workflow-executed pattern)
                UUID actorId = null;
                String actorName = null;
                String clientIp = null;
                String userAgent = null;
                
                // Get from execution context (contains the user who triggered the workflow)
                if (execution != null) {
                    actorId = execution.getTriggeredBy();
                    Map<String, Object> execContext = execution.getExecutionContext();
                    if (execContext != null) {
                        // For event-triggered workflows, use createdBy field
                        actorName = (String) execContext.get("createdBy");
                        // For manual workflows, fallback to triggeredByUsername or username
                        if (actorName == null) {
                            actorName = (String) execContext.get("triggeredByUsername");
                        }
                        if (actorName == null) {
                            actorName = (String) execContext.get("username");
                        }
                        clientIp = (String) execContext.get("clientIp");
                        userAgent = (String) execContext.get("userAgent");
                    }
                }
                
                WorkflowAuditEvent event = WorkflowAuditEvent.user(
                    actorId,
                    actorName,
                    tenantId,
                    approval.getId(),
                    OBJECT_TYPE_WORKFLOW_APPROVAL,
                    AUDIT_ACTION_APPROVAL_CREATED,
                    null,
                    sanitizeForAudit(enrichedState),
                    clientIp,
                    userAgent
                );
                
                auditPublisher.publishEvent(event);
                
                logger.info()
                    .category(LOG_CATEGORY)
                    .message(LOG_PUBLISHED_APPROVAL_CREATED_AUDIT)
                    .objectType("workflow-approval")
                    .objectId(approval.getId().toString())
                    .detail("action", "approval-created")
                    .detail("tenantId", tenantId)
                    .detail("requestedBy", actorId != null ? actorId.toString() : "system")
                    .detail("assignedTo", approval.getApproverId().toString())
                    .log();
            }
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_APPROVAL_CREATED_AUDIT)
                .log();
        }
    }

    /**
     * Audit approval approved.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowApprovalService.approve(..))",
        returning = "result"
    )
    public void auditApprovalApproved(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof WorkflowApproval approval) {
                // Fetch workflow execution and workflow to get name
                WorkflowExecution execution = approval.getExecutionId() != null
                    ? workflowExecutionRepository.findById(approval.getExecutionId()).orElse(null)
                    : null;
                
                // Skip audit logging for simulations
                if (execution != null && execution.getExecutionContext() != null && 
                    Boolean.TRUE.equals(execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE))) {
                    logger.debug()
                        .category(LOG_CATEGORY)
                        .message("Skipping approval-approved audit for simulation")
                        .detail("approvalId", approval.getId().toString())
                        .detail("executionId", execution.getId().toString())
                        .log();
                    return;
                }
                Workflow workflow = execution != null && execution.getWorkflowId() != null
                    ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                    : null;
                String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
                
                // Create enriched stateAfter with workflow context for better object name extraction
                ObjectNode enrichedState = objectMapper.valueToTree(approval);
                if (workflowName != null) {
                    enrichedState.put("workflowName", workflowName);
                }
                if (workflow != null) {
                    enrichedState.set("workflow", objectMapper.valueToTree(workflow));
                }
                
                // Extract tenant ID from approval context
                String tenantId = null;
                if (approval.getApprovalContext() != null && approval.getApprovalContext().containsKey("tenantId")) {
                    tenantId = (String) approval.getApprovalContext().get("tenantId");
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    tenantId = getCurrentTenantId();
                }
                
                UUID actorId = getCurrentUserId() != null ? getCurrentUserId() : approval.getApproverId();
                String actorName = getCurrentUsername();
                
                WorkflowAuditEvent event = actorId != null
                    ? WorkflowAuditEvent.user(
                        actorId,
                        actorName != null ? actorName : actorId.toString(),
                        tenantId,
                        approval.getId(),
                        OBJECT_TYPE_WORKFLOW_APPROVAL,
                        AUDIT_ACTION_APPROVAL_APPROVED,
                        null,
                        sanitizeForAudit(enrichedState),
                        getCurrentClientIp(),
                        getCurrentUserAgent()
                    )
                    : WorkflowAuditEvent.system(
                        tenantId,
                        approval.getId(),
                        OBJECT_TYPE_WORKFLOW_APPROVAL,
                        AUDIT_ACTION_APPROVAL_APPROVED,
                        null,
                        sanitizeForAudit(enrichedState)
                    );
                
                auditPublisher.publishEvent(event);
                
                logger.info()
                    .category(LOG_CATEGORY)
                    .message(LOG_PUBLISHED_APPROVAL_APPROVED_AUDIT)
                    .objectType("workflow-approval")
                    .objectId(approval.getId().toString())
                    .detail("action", "approval-approved")
                    .detail("approverId", approval.getApproverId().toString())
                    .detail("decision", approval.getDecision().toString())
                    .log();
            }
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_APPROVAL_APPROVED_AUDIT)
                .log();
        }
    }

    /**
     * Audit approval rejected.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowApprovalService.reject(..))",
        returning = "result"
    )
    public void auditApprovalRejected(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof WorkflowApproval approval) {
                // Fetch workflow execution and workflow to get name
                WorkflowExecution execution = approval.getExecutionId() != null
                    ? workflowExecutionRepository.findById(approval.getExecutionId()).orElse(null)
                    : null;
                
                // Skip audit logging for simulations
                if (execution != null && execution.getExecutionContext() != null && 
                    Boolean.TRUE.equals(execution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE))) {
                    logger.debug()
                        .category(LOG_CATEGORY)
                        .message("Skipping approval-rejected audit for simulation")
                        .detail("approvalId", approval.getId().toString())
                        .detail("executionId", execution.getId().toString())
                        .log();
                    return;
                }
                Workflow workflow = execution != null && execution.getWorkflowId() != null
                    ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                    : null;
                String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
                
                // Create enriched stateAfter with workflow context for better object name extraction
                ObjectNode enrichedState = objectMapper.valueToTree(approval);
                if (workflowName != null) {
                    enrichedState.put("workflowName", workflowName);
                }
                if (workflow != null) {
                    enrichedState.set("workflow", objectMapper.valueToTree(workflow));
                }
                
                // Extract tenant ID from approval context
                String tenantId = null;
                if (approval.getApprovalContext() != null && approval.getApprovalContext().containsKey("tenantId")) {
                    tenantId = (String) approval.getApprovalContext().get("tenantId");
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    tenantId = getCurrentTenantId();
                }
                
                UUID actorId = getCurrentUserId() != null ? getCurrentUserId() : approval.getApproverId();
                String actorName = getCurrentUsername();
                
                WorkflowAuditEvent event = actorId != null
                    ? WorkflowAuditEvent.user(
                        actorId,
                        actorName != null ? actorName : actorId.toString(),
                        tenantId,
                        approval.getId(),
                        OBJECT_TYPE_WORKFLOW_APPROVAL,
                        AUDIT_ACTION_APPROVAL_REJECTED,
                        null,
                        sanitizeForAudit(enrichedState),
                        getCurrentClientIp(),
                        getCurrentUserAgent()
                    )
                    : WorkflowAuditEvent.system(
                        tenantId,
                        approval.getId(),
                        OBJECT_TYPE_WORKFLOW_APPROVAL,
                        AUDIT_ACTION_APPROVAL_REJECTED,
                        null,
                        sanitizeForAudit(enrichedState)
                    );
                
                auditPublisher.publishEvent(event);
                
                logger.info()
                    .category(LOG_CATEGORY)
                    .message(LOG_PUBLISHED_APPROVAL_REJECTED_AUDIT)
                    .objectType("workflow-approval")
                    .objectId(approval.getId().toString())
                    .detail("action", "approval-rejected")
                    .detail("approverId", approval.getApproverId().toString())
                    .detail("decision", approval.getDecision().toString())
                    .log();
            }
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_APPROVAL_REJECTED_AUDIT)
                .log();
        }
    }

    /**
     * Audit execution cancelled.
     */
    @AfterReturning(
        pointcut = "execution(* com.aidentx.workflowsservice.service.WorkflowExecutionService.cancelExecution(..))",
        returning = "result"
    )
    public void auditExecutionCancelled(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof WorkflowExecutionDTO executionDTO) {
                // Get execution entity for full details
                WorkflowExecution execution = workflowExecutionRepository.findById(executionDTO.getId())
                    .orElse(null);
                
                if (execution == null) {
                    return;
                }
                
                // Get workflow for name
                Workflow workflow = execution.getWorkflowId() != null
                    ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                    : null;
                String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
                
                // Create enriched stateAfter with workflow context for better object name extraction
                ObjectNode enrichedState = objectMapper.valueToTree(execution);
                if (workflowName != null) {
                    enrichedState.put("workflowName", workflowName);
                }
                if (workflow != null) {
                    enrichedState.set("workflow", objectMapper.valueToTree(workflow));
                }
                // Sanitize to remove sensitive data before audit logging
                JsonNode stateAfter = sanitizeForAudit(enrichedState);
                
                // Extract tenant ID from execution context
                String tenantId = null;
                if (execution.getExecutionContext() != null && execution.getExecutionContext().containsKey("tenantId")) {
                    tenantId = (String) execution.getExecutionContext().get("tenantId");
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    tenantId = getCurrentTenantId();
                }
                
                // Actor is the person who cancelled the execution
                UUID actorId = getCurrentUserId();
                String actorName = getCurrentUsername();
                
                WorkflowAuditEvent event = actorId != null
                    ? WorkflowAuditEvent.user(
                        actorId,
                        actorName != null ? actorName : actorId.toString(),
                        tenantId,
                        execution.getId(),
                        OBJECT_TYPE_WORKFLOW_EXECUTION,
                        AUDIT_ACTION_EXECUTION_CANCELLED,
                        null,
                        stateAfter,
                        getCurrentClientIp(),
                        getCurrentUserAgent()
                    )
                    : WorkflowAuditEvent.system(
                        tenantId,
                        execution.getId(),
                        OBJECT_TYPE_WORKFLOW_EXECUTION,
                        AUDIT_ACTION_EXECUTION_CANCELLED,
                        null,
                        stateAfter
                    );
                
                auditPublisher.publishEvent(event);
                
                logger.info()
                    .category(LOG_CATEGORY)
                    .message(LOG_PUBLISHED_EXECUTION_CANCELLED_AUDIT)
                    .objectType("workflow-execution")
                    .objectId(execution.getId().toString())
                    .detail("action", "Workflow-Execution-Cancelled")
                    .detail("workflowName", workflowName)
                    .detail("cancelledBy", actorId != null ? actorId.toString() : "system")
                    .log();
            }
        } catch (Exception e) {
            logger.error(e)
                .category(LOG_CATEGORY)
                .message(LOG_FAILED_PUBLISH_EXECUTION_CANCELLED_AUDIT)
                .log();
        }
    }
    
    /**
     * Sanitizes JsonNode data to remove sensitive information before audit logging.
     * This ensures tokens, secrets, and other sensitive data are not persisted in audit logs.
     * 
     * @param jsonNode the JsonNode to sanitize
     * @return sanitized JsonNode with sensitive fields redacted
     */
    private JsonNode sanitizeForAudit(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        
        try {
            // Convert JsonNode to Map, sanitize, then convert back to JsonNode
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.convertValue(jsonNode, Map.class);
            Map<String, Object> sanitizedMap = ResponseDataSanitizer.sanitizeMap(dataMap);
            return objectMapper.valueToTree(sanitizedMap);
        } catch (Exception e) {
            logger.warn()
                .category(LOG_CATEGORY)
                .message("Failed to sanitize audit data, returning original")
                .detail("error", e.getMessage())
                .log();
            return jsonNode;
        }
    }
}
