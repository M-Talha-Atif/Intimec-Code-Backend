package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.async.AsyncExecutionDispatcher;
import com.aidentx.workflowsservice.client.EmailRestClient;
import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.client.UserGrpcClient;
import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.aidentx.workflowsservice.event.AccessPortalEventPublisher;
import com.aidentx.workflowsservice.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.repository.WorkflowApprovalRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Service for managing workflow approvals
 * Phase 6a: Enhanced with async execution resume on approval decisions
 * Phase 3: Enhanced with automatic delegation based on time-based rules
 */
@Service
public class WorkflowApprovalService {

    private final IUnifiedLogger logger;
    private final WorkflowApprovalRepository approvalRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final UserGrpcClient userClient;
    private final NotificationKafkaClient notificationClient;
    private final EmailRestClient emailClient;
    private final AsyncExecutionDispatcher asyncExecutionDispatcher;
    private final JwtUtils jwtUtils;
    private final ApprovalDelegationService approvalDelegationService;
    private final RestTemplate restTemplate;
    private final AccessPortalEventPublisher accessPortalEventPublisher; // for access portal

    @Value("${auth.service.url:http://localhost:8090}")
    private String authServiceUrl;

    public WorkflowApprovalService(
            WorkflowApprovalRepository approvalRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowRepository workflowRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            UserGrpcClient userClient,
            NotificationKafkaClient notificationClient,
            EmailRestClient emailClient,
            @Lazy AsyncExecutionDispatcher asyncExecutionDispatcher,
            JwtUtils jwtUtils,
            @Lazy ApprovalDelegationService approvalDelegationService,
            RestTemplate restTemplate,
            UnifiedLoggerFactory loggerFactory,
            AccessPortalEventPublisher accessPortalEventPublisher) {
        this.logger = loggerFactory.getLogger(WorkflowApprovalService.class, LOG_CATEGORY_WORKFLOW_APPROVAL);
        this.approvalRepository = approvalRepository;
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.userClient = userClient;
        this.jwtUtils = jwtUtils;
        this.notificationClient = notificationClient;
        this.emailClient = emailClient;
        this.asyncExecutionDispatcher = asyncExecutionDispatcher;
        this.approvalDelegationService = approvalDelegationService;
        this.restTemplate = restTemplate;
        this.accessPortalEventPublisher = accessPortalEventPublisher;
    }

    /**
     * Extract tenant ID from the current HTTP request or execution context.
     */
    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return jwtUtils.extractTenantId(request);
            }
            logger.error()
                .message(LOG_TENANT_ID_EXTRACTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .log();
            throw new IllegalStateException("Cannot extract tenant ID - no HTTP context available");
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_EXTRACT_TENANT_ID)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
            throw new IllegalStateException("Failed to extract tenant ID: " + e.getMessage(), e);
        }
    }

    /**
     * Extract username from the current HTTP request.
     * Used to capture approver username for timeline display.
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = jwtUtils.extractUsername(request);
                if (username != null && !username.trim().isEmpty()) {
                    return username;
                }
            }
        } catch (Exception e) {
            logger.debug()
                .message("Failed to extract username from request")
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Fetch user details from auth-service by user ID.
     * Returns username/email for monitoring purposes.
     */
    private String fetchUserUsername(UUID userId) {
        try {
            String url = authServiceUrl + "/api/v1/users/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "OK".equals(response.get("status"))) {
                String username = (String) response.get("username");
                if (username == null || username.trim().isEmpty()) {
                    username = (String) response.get("email");
                }
                return username;
            }
        } catch (Exception e) {
            logger.debug()
                .message("Failed to fetch user details from auth-service")
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("userId", userId.toString())
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Extract tenant ID from approval/execution context or fallback to HTTP request.
     */
    private String getTenantId(Map<String, Object> context) {
        // Try to get tenantId from execution context first (for async executions)
        if (context != null && context.containsKey("tenantId")) {
            return context.get("tenantId").toString();
        }
        // Fallback to HTTP context (for API-triggered approvals)
        return getCurrentTenantId();
    }

    /**
     * Create a new approval request
     */
    @Transactional
    public WorkflowApproval createApprovalRequest(
            UUID executionId,
            UUID nodeExecutionId,
            UUID approverId,
            WorkflowApproval.ApproverType approverType,
            Map<String, Object> approvalContext,
            Map<String, Object> evidenceBundle,
            Integer timeoutMinutes
    ) {
        return createApprovalRequest(executionId, nodeExecutionId, approverId, approverType,
                                    approvalContext, evidenceBundle, timeoutMinutes, null,
                                    null, null, null);
    }

    /**
     * Create a new approval request with optional approval group ID
     * Phase 7: Enhanced for multi-approval tracking
     */
    @Transactional
    public WorkflowApproval createApprovalRequest(
            UUID executionId,
            UUID nodeExecutionId,
            UUID approverId,
            WorkflowApproval.ApproverType approverType,
            Map<String, Object> approvalContext,
            Map<String, Object> evidenceBundle,
            Integer timeoutMinutes,
            UUID approvalGroupId
    ) {
        return createApprovalRequest(executionId, nodeExecutionId, approverId, approverType,
                                    approvalContext, evidenceBundle, timeoutMinutes, approvalGroupId,
                                    null, null, null, null, null);
    }

    /**
     * Create a new approval request with optional email notification
     * Enhanced with custom email support
     */
    @Transactional
    public WorkflowApproval createApprovalRequest(
            UUID executionId,
            UUID nodeExecutionId,
            UUID approverId,
            WorkflowApproval.ApproverType approverType,
            Map<String, Object> approvalContext,
            Map<String, Object> evidenceBundle,
            Integer timeoutMinutes,
            UUID approvalGroupId,
            Boolean sendEmail,
            String emailSubject,
            String emailBody
    ) {
        return createApprovalRequest(executionId, nodeExecutionId, approverId, approverType,
                                    approvalContext, evidenceBundle, timeoutMinutes, approvalGroupId,
                                    sendEmail, emailSubject, emailBody,
                                    com.aidentx.workflowsservice.enums.ApprovalPolicy.ALL, null);
    }

    /**
     * Create a new approval request with approval policy configuration
     * Phase 1 (Week 1-2): Enhanced with flexible approval policies
     */
    @Transactional
    public WorkflowApproval createApprovalRequest(
            UUID executionId,
            UUID nodeExecutionId,
            UUID approverId,
            WorkflowApproval.ApproverType approverType,
            Map<String, Object> approvalContext,
            Map<String, Object> evidenceBundle,
            Integer timeoutMinutes,
            UUID approvalGroupId,
            Boolean sendEmail,
            String emailSubject,
            String emailBody,
            com.aidentx.workflowsservice.enums.ApprovalPolicy approvalPolicy,
            Integer requiredApprovals
    ) {
        logger.info()
            .message(LOG_CREATING_APPROVAL_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .detail("approverId", approverId.toString())
            .detail("approvalPolicy", approvalPolicy != null ? approvalPolicy.name() : "ALL")
            .log();

        // Phase 3: Check for active delegation and apply automatic routing
        UUID effectiveApproverId = approverId;
        ApprovalDelegation activeDelegation = null;
        String tenantId = getTenantId(approvalContext);

        if (approvalDelegationService != null && tenantId != null) {
            Optional<ApprovalDelegation> delegation = approvalDelegationService.findActiveDelegation(approverId, tenantId);
            if (delegation.isPresent()) {
                activeDelegation = delegation.get();
                effectiveApproverId = activeDelegation.getToUserId();
                logger.info()
                    .message(LOG_AUTO_DELEGATION_APPLIED)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail(DETAIL_KEY_ORIGINAL_APPROVER_ID, approverId.toString())
                    .detail(DETAIL_KEY_EFFECTIVE_APPROVER_ID, effectiveApproverId.toString())
                    .detail(DETAIL_KEY_DELEGATION_ID, activeDelegation.getId().toString())
                    .log();
            }
        }

        // Fetch approver username for monitoring purposes
        String approverUsername = fetchUserUsername(effectiveApproverId);
        if (approverUsername != null && !approverUsername.trim().isEmpty()) {
            Map<String, Object> contextToUse = approvalContext != null ? new HashMap<>(approvalContext) : new HashMap<>();
            contextToUse.put("approverUsername", approverUsername);
            approvalContext = contextToUse;

            logger.debug()
                .message("Stored approver username in approval context")
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("approverId", effectiveApproverId.toString())
                .detail("approverUsername", approverUsername)
                .log();
        }

        WorkflowApproval approval = WorkflowApproval.builder()
                .tenantId(tenantId)
                .executionId(executionId)
                .nodeExecutionId(nodeExecutionId)
                .approverId(effectiveApproverId)
                .approverType(approverType)
                .decision(WorkflowApproval.ApprovalDecision.PENDING)
                .requestedAt(OffsetDateTime.now())
                .approvalContext(approvalContext)
                .evidenceBundle(evidenceBundle)
                .approvalGroupId(approvalGroupId)
                .approvalPolicy(approvalPolicy != null ? approvalPolicy : com.aidentx.workflowsservice.enums.ApprovalPolicy.ALL)
                .requiredApprovals(requiredApprovals)
                .build();

        // Phase 3: If auto-delegated, track original approver in delegation chain
        if (activeDelegation != null) {
            approval.setDelegatedTo(effectiveApproverId);
            List<WorkflowApproval.DelegationRecord> chain = new ArrayList<>();
            chain.add(WorkflowApproval.DelegationRecord.builder()
                    .fromApproverId(approverId)
                    .toApproverId(effectiveApproverId)
                    .delegatedAt(OffsetDateTime.now())
                    .reason(LOG_AUTO_DELEGATION_APPLIED + " - " + (activeDelegation.getReason() != null ? activeDelegation.getReason() : "No reason"))
                    .sequenceNumber(1)
                    .build());
            approval.setDelegationChain(chain);
        }

        // Set timeout if specified
        if (timeoutMinutes != null && timeoutMinutes > 0) {
            approval.setTimeoutAt(OffsetDateTime.now().plusMinutes(timeoutMinutes));
        }

        WorkflowApproval savedApproval = approvalRepository.save(approval);



         // Publish approval created event -- Access Portal Event Publisher
        try {
            accessPortalEventPublisher.publishApprovalCreated(
                executionId,
                savedApproval.getId(),
                effectiveApproverId,
                approverType.name(), // type
                approverUsername, // name
                approvalContext,
                evidenceBundle
            );
            logger.info().
            message("Published approval created event")
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvalId", savedApproval.getId().toString())
                    .detail("executionId", executionId.toString())
                    .detail("approverId", effectiveApproverId.toString())
                    .detail("approverType", approverType.name())
                    .detail("approverUserName", approverUsername)
                    .log();

        } catch (Exception e) {
            // Log but don't fail
            logger.debug()
                .message("Event publishing skipped or failed")
                .detail("error", e.getMessage())
                .log();
        }

        // Send notification to approver (they need to know to approve/reject)
        try {
            WorkflowExecution execution = executionRepository.findById(executionId)
                .orElse(null);
            Workflow workflow = execution != null && execution.getWorkflowId() != null
                ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                : null;

            String workflowName = workflow != null ? workflow.getName() : "Workflow";
            String requestedBy = execution != null && execution.getTriggeredBy() != null
                ? execution.getTriggeredBy().toString()
                : "System";

            // Fetch approver details from user-service via gRPC
            // Phase 3: Use effectiveApproverId for notifications (may be delegate)
            String approverEmail = DEFAULT_APPROVER_EMAIL;
            String approverName = DEFAULT_APPROVER_NAME;

            try {
                // Attempt to fetch real user details for the effective approver
                var userResponse = userClient.getUserById(effectiveApproverId.toString(), getTenantId(approvalContext));
                if (userResponse != null && userResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userResponse;
                    approverEmail = userMap.get("email") != null ? (String) userMap.get("email") : approverEmail;
                    approverName = userMap.get("fullName") != null ? (String) userMap.get("fullName") : approverName;
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_APPROVER_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("error", e.getMessage())
                    .log();
            }

            // Phase 3: If auto-delegated, send special notification to delegate
            if (activeDelegation != null && approvalDelegationService != null) {
                approvalDelegationService.sendApprovalDelegatedNotification(
                    activeDelegation,
                    savedApproval.getId(),
                    workflowName,
                    executionId.toString()
                );
            } else {
                // Build rich enterprise notification context
                com.aidentx.workflowsservice.dto.notification.ApprovalNotificationContext notificationContext =
                    buildApprovalNotificationContext(approvalContext, execution, workflow, timeoutMinutes, requestedBy, tenantId);

                // Standard notification - send in-app notification via Kafka with rich context
                notificationClient.sendApprovalRequestNotification(
                    getTenantId(approvalContext),
                    effectiveApproverId.toString(),
                    approverEmail,
                    approverName,
                    workflowName,
                    executionId.toString(),
                    savedApproval.getId().toString(),
                    requestedBy,
                    notificationContext
                );
            }

            // Send email notification if enabled
            if (Boolean.TRUE.equals(sendEmail) && emailSubject != null && emailBody != null) {
                // Replace template variables in subject and body
                String processedSubject = emailSubject
                    .replace("{{workflowName}}", workflowName)
                    .replace("{{requestedBy}}", requestedBy)
                    .replace("{{approverName}}", approverName);

                String processedBody = emailBody
                    .replace("{{workflowName}}", workflowName)
                    .replace("{{requestedBy}}", requestedBy)
                    .replace("{{approverName}}", approverName)
                    .replace("{{executionId}}", executionId.toString());

                emailClient.sendEmail(
                    getTenantId(approvalContext),
                    approverEmail,
                    processedSubject,
                    processedBody
                );

                logger.info()
                    .message(LOG_CUSTOM_EMAIL_SENT)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approverId", effectiveApproverId.toString())
                    .detail("approverEmail", approverEmail)
                    .log();
            }

            logger.info()
                .message(LOG_APPROVAL_REQUEST_NOTIFICATION_SENT)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("approverId", approverId.toString())
                .log();
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_APPROVAL_REQUEST_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            // Don't fail the approval creation if notification fails
        }

        return savedApproval;
    }

    /**
     * Approve an approval request
     */
    @Transactional
    public WorkflowApproval approve(UUID approvalId, String comments, UUID approverId) {
        logger.info()
            .message(LOG_APPROVING_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
            .objectId(approvalId.toString())
            .detail("approverId", approverId.toString())
            .log();

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_APPROVAL_NOT_FOUND + approvalId));

        // Validate approver
        if (!approval.getApproverId().equals(approverId) &&
            (approval.getDelegatedTo() == null || !approval.getDelegatedTo().equals(approverId))) {
            throw new IllegalArgumentException(ERROR_APPROVAL_NOT_AUTHORIZED);
        }

        // Check if already decided
        if (approval.getDecision() != WorkflowApproval.ApprovalDecision.PENDING) {
            throw new IllegalStateException(ERROR_APPROVAL_ALREADY_DECIDED + approval.getDecision());
        }

        approval.setDecision(WorkflowApproval.ApprovalDecision.APPROVED);
        approval.setComments(comments);
        approval.setRespondedAt(OffsetDateTime.now());

        // Store approver username in approvalContext for timeline display
        String approverUsername = getCurrentUsername();
        if (approverUsername != null && !approverUsername.trim().isEmpty()) {
            Map<String, Object> context = approval.getApprovalContext();
            if (context == null) {
                context = new HashMap<>();
                approval.setApprovalContext(context);
            }
            context.put("respondedBy", approverUsername);
        }

        WorkflowApproval savedApproval = approvalRepository.save(approval);

           //   Publish approval responded event  -- Access Portal Event Publisher
        // add respondedBy
        try {
            accessPortalEventPublisher.publishApprovalResponded(
                approvalId,
                approverId,
                approverUsername,
                "APPROVED",
                comments,
                approval.getApprovalContext(),
                approval.getEvidenceBundle(),
                approval.getApproverType()
            );
        } catch (Exception e) {
            logger.debug()
                .message("Event publishing skipped or failed")
                .detail("error", e.getMessage())
                .log();
        }


        // Check if execution is still in PAUSED state before resuming
        WorkflowExecution currentExecution = executionRepository.findById(approval.getExecutionId())
            .orElse(null);

        if (currentExecution == null) {
            logger.warn()
                .message(LOG_CANNOT_RESUME_EXECUTION_NOT_FOUND)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("executionId", approval.getExecutionId().toString())
                .log();
            return savedApproval;
        }

        if (currentExecution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
            logger.warn()
                .message(LOG_CANNOT_RESUME_EXECUTION_NOT_PAUSED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("currentStatus", currentExecution.getStatus().toString())
                .detail("executionId", approval.getExecutionId().toString())
                .log();
            return savedApproval;
        }

        // Phase 7: Check if this is part of multi-approval group
        // Phase 1 (Week 1-2): Enhanced with flexible approval policies
        UUID executionIdToResume = null;

        if (approval.getApprovalGroupId() != null) {
            List<WorkflowApproval> allApprovalsInGroup =
                approvalRepository.findByApprovalGroupId(approval.getApprovalGroupId());

            // Get policy from first approval (all approvals in group share same policy)
            WorkflowApproval firstApproval = allApprovalsInGroup.get(0);
            com.aidentx.workflowsservice.enums.ApprovalPolicy policy =
                firstApproval.getApprovalPolicy() != null
                    ? firstApproval.getApprovalPolicy()
                    : com.aidentx.workflowsservice.enums.ApprovalPolicy.ALL;

            long totalApprovals = allApprovalsInGroup.size();
            long approvedCount = allApprovalsInGroup.stream()
                .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED)
                .count();
            long rejectedCount = allApprovalsInGroup.stream()
                .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED)
                .count();
            long pendingCount = totalApprovals - approvedCount - rejectedCount;

            logger.info()
                .message(LOG_CHECKING_MULTI_APPROVAL_STATUS)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("approvedCount", approvedCount)
                .detail("rejectedCount", rejectedCount)
                .detail("totalApprovals", totalApprovals)
                .detail("approvalPolicy", policy.name())
                .log();

            // CRITICAL FIX: Check if this is per-user approval (has targetUserId in evidence bundle)
            boolean isPerUserApproval = allApprovalsInGroup.stream()
                .anyMatch(a -> a.getEvidenceBundle() != null && a.getEvidenceBundle().get("targetUserId") != null);

            // Check if this is role owner approval
            boolean isRoleOwnerApproval = allApprovalsInGroup.stream()
                .anyMatch(a -> a.getApproverType() == WorkflowApproval.ApproverType.ROLE_OWNER);

            // Check completion based on approval policy
            boolean groupComplete;
            if (isPerUserApproval || isRoleOwnerApproval) {
                // For per-user approvals OR role owner approvals, always wait for ALL decisions regardless of policy
                groupComplete = (pendingCount == 0);
                String approvalType = isPerUserApproval ? "per-user" : "role owner";
                logger.info()
                    .message(approvalType + " approval check")
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("isComplete", groupComplete)
                    .detail("pendingCount", pendingCount)
                    .log();
            } else {
                // Standard multi-approval - use policy-based completion logic
                groupComplete = switch (policy) {
                    case ALL -> {
                        // ALL: All approvers must approve
                        boolean allApproved = allApprovalsInGroup.stream()
                            .allMatch(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED);
                        yield allApproved;
                    }
                    case ANY -> {
                        // ANY: First approval wins
                        boolean anyApproved = allApprovalsInGroup.stream()
                            .anyMatch(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED);
                        yield anyApproved;
                    }
                    case THRESHOLD -> {
                        // THRESHOLD: N of M must approve
                        Integer required = firstApproval.getRequiredApprovals();
                        if (required == null) {
                            logger.warn()
                                .message(ERROR_THRESHOLD_REQUIRED_APPROVALS_MISSING)
                                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                                .detail("approvalGroupId", approval.getApprovalGroupId().toString())
                                .log();
                            yield approvedCount >= totalApprovals; // Fallback to ALL
                        }
                        yield approvedCount >= required;
                    }
                    case FAIL_FAST -> {
                        // FAIL_FAST: First rejection immediately blocks, OR all approved
                        boolean anyRejected = allApprovalsInGroup.stream()
                            .anyMatch(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED);
                        boolean allApproved = allApprovalsInGroup.stream()
                            .allMatch(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED);
                        yield anyRejected || allApproved; // Complete on first rejection OR all approved
                    }
                };
            }

            // Determine final decision based on policy
            boolean finalApproved = switch (policy) {
                case ALL -> approvedCount == totalApprovals && rejectedCount == 0;
                case ANY -> approvedCount > 0;
                case THRESHOLD -> approvedCount >= (firstApproval.getRequiredApprovals() != null ? firstApproval.getRequiredApprovals() : totalApprovals);
                case FAIL_FAST -> rejectedCount == 0 && approvedCount == totalApprovals; // Only approved if no rejections AND all approved
            };

            if (groupComplete) {
                // Group approval condition met, resume workflow
                String completionReason = switch (policy) {
                    case ALL -> "All approvers approved";
                    case ANY -> "At least one approver approved";
                    case THRESHOLD -> String.format("%d of %d required approvals received",
                        approvedCount, firstApproval.getRequiredApprovals());
                    case FAIL_FAST -> rejectedCount > 0 ? "Request rejected (FAIL_FAST policy triggered)" : "All approvers approved";
                };

                logger.info()
                    .message(LOG_MULTI_APPROVAL_COMPLETE)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("executionId", approval.getExecutionId().toString())
                    .detail("policy", policy.name())
                    .detail("reason", completionReason)
                    .detail("finalApproved", finalApproved)
                    .log();

                // Update node execution with POLICY-AWARE approval output data
                updateNodeExecutionWithMultiApprovalData(
                    approval, approverId, comments, policy,
                    finalApproved, approvedCount, rejectedCount, pendingCount,
                    totalApprovals, firstApproval.getRequiredApprovals(), completionReason,
                    allApprovalsInGroup
                );

                executionIdToResume = approval.getExecutionId();
            } else {
                // Still waiting for other approvers - update with interim status (no resume)
                logger.info()
                    .message(LOG_MULTI_APPROVAL_INCOMPLETE)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvedCount", approvedCount)
                    .detail("totalApprovals", totalApprovals)
                    .log();
            }
        } else {
            // Single approval - update output and resume immediately
            updateNodeExecutionWithApprovalData(approval, approverId, comments);

            logger.info()
                .message(LOG_SINGLE_APPROVAL_APPROVED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("executionId", approval.getExecutionId().toString())
                .log();
            executionIdToResume = approval.getExecutionId();
        }

        // CRITICAL: Force flush to ensure updates are persisted before async resume
        if (executionIdToResume != null) {
            nodeExecutionRepository.flush();
            logger.info()
                .message(LOG_FLUSHED_NODE_EXECUTION_UPDATES)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .log();
        }

        // Store execution ID to resume after transaction commits
        final UUID finalExecutionIdToResume = executionIdToResume;

        // Register post-commit callback to resume workflow AFTER transaction commits
        // This ensures the approval output data is visible to the resumed workflow
        if (finalExecutionIdToResume != null) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        logger.info()
                            .message(LOG_TRANSACTION_COMMITTED_RESUMING_WORKFLOW)
                            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                            .detail("executionId", finalExecutionIdToResume.toString())
                            .log();
                        asyncExecutionDispatcher.resume(finalExecutionIdToResume);
                    }
                }
            );
        }

        // Send notification to workflow requester (they need to know their request was approved)
        try {
            WorkflowExecution execution = executionRepository.findById(approval.getExecutionId())
                .orElse(null);
            Workflow workflow = execution != null && execution.getWorkflowId() != null
                ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                : null;

            if (execution != null && execution.getTriggeredBy() != null) {
                String workflowName = workflow != null ? workflow.getName() : "Workflow";
                UUID requesterId = execution.getTriggeredBy();

                // TODO: Fetch real requester and approver details from identity-service
                notificationClient.sendApprovalDecisionNotification(
                    getCurrentTenantId(),
                    requesterId.toString(), // Send to REQUESTER, not approver
                    DEFAULT_REQUESTER_EMAIL, // TODO: Get from identity-service
                    DEFAULT_REQUESTER_NAME, // TODO: Get from identity-service
                    workflowName,
                    approval.getExecutionId().toString(),
                    approverId.toString(),
                    true, // approved
                    comments != null ? comments : DEFAULT_APPROVAL_COMMENT_APPROVED
                );

                logger.info()
                    .message(LOG_APPROVAL_DECISION_NOTIFICATION_SENT)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("requesterId", requesterId.toString())
                    .log();
            }
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_APPROVAL_DECISION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
            // Don't fail the approval if notification fails
        }

        return savedApproval;
    }

    /**
     * Reject an approval request
     * Industry Standard: Rejection completes the node with rejection data and allows workflow to continue
     */
    @Transactional
    public WorkflowApproval reject(UUID approvalId, String comments, UUID approverId) {
        logger.info()
            .message(LOG_REJECTING_REQUEST)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
            .objectId(approvalId.toString())
            .detail("approverId", approverId.toString())
            .log();

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_APPROVAL_NOT_FOUND + approvalId));

        // Validate approver
        if (!approval.getApproverId().equals(approverId) &&
            (approval.getDelegatedTo() == null || !approval.getDelegatedTo().equals(approverId))) {
            throw new IllegalArgumentException(ERROR_APPROVAL_NOT_AUTHORIZED_REJECT);
        }

        // Check if already decided
        if (approval.getDecision() != WorkflowApproval.ApprovalDecision.PENDING) {
            throw new IllegalStateException(ERROR_APPROVAL_ALREADY_DECIDED + approval.getDecision());
        }

        approval.setDecision(WorkflowApproval.ApprovalDecision.REJECTED);
        approval.setComments(comments);
        approval.setRespondedAt(OffsetDateTime.now());

        // Store approver username in approvalContext for timeline display
        String approverUsername = getCurrentUsername();
        if (approverUsername != null && !approverUsername.trim().isEmpty()) {
            Map<String, Object> context = approval.getApprovalContext();
            if (context == null) {
                context = new HashMap<>();
                approval.setApprovalContext(context);
            }
            context.put("respondedBy", approverUsername);
        }

        WorkflowApproval savedApproval = approvalRepository.save(approval);

        //  Publish approval responded event for rejection  -- Access Portal Event Publisher
        try {
            accessPortalEventPublisher.publishApprovalResponded(
                approvalId,
                approverId,
                approverUsername,
                "REJECTED",
                comments,
                approval.getApprovalContext(),
               approval.getEvidenceBundle(),
               approval.getApproverType()
            );
        } catch (Exception e) {
            logger.debug()
                .message("Event publishing skipped or failed")
                .detail("error", e.getMessage())
                .log();
        }

        // Check if execution is still in PAUSED state before resuming
        WorkflowExecution currentExecution = executionRepository.findById(approval.getExecutionId())
            .orElse(null);

        if (currentExecution == null) {
            logger.warn()
                .message(LOG_CANNOT_RESUME_EXECUTION_NOT_FOUND)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("executionId", approval.getExecutionId().toString())
                .log();
            return savedApproval;
        }

        if (currentExecution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
            logger.warn()
                .message(LOG_CANNOT_RESUME_EXECUTION_NOT_PAUSED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("currentStatus", currentExecution.getStatus().toString())
                .detail("executionId", approval.getExecutionId().toString())
                .log();
            return savedApproval;
        }

        // CRITICAL FIX: For per-user approvals (structured approvers), don't resume immediately on rejection
        // Wait for ALL approvals to be decided, then resume with collected approvedUserIds/rejectedUserIds
        UUID executionIdToResume = null;

        if (approval.getApprovalGroupId() != null) {
            // This is a multi-approval - check if we should resume
            List<WorkflowApproval> allApprovals = approvalRepository.findByApprovalGroupId(approval.getApprovalGroupId());
            long totalApprovals = allApprovals.size();
            long pendingCount = allApprovals.stream()
                .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.PENDING)
                .count();
            long rejectedCount = allApprovals.stream()
                .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED)
                .count();
            long approvedCount = allApprovals.stream()
                .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED)
                .count();

            // Check if this is per-user approval (has targetUserId in evidence bundle)
            boolean isPerUserApproval = allApprovals.stream()
                .anyMatch(a -> a.getEvidenceBundle() != null && a.getEvidenceBundle().get("targetUserId") != null);

            // Check if this is role owner approval
            boolean isRoleOwnerApproval = allApprovals.stream()
                .anyMatch(a -> a.getApproverType() == WorkflowApproval.ApproverType.ROLE_OWNER);

            if (isPerUserApproval || isRoleOwnerApproval) {
                // For per-user approvals OR role owner approvals, wait for ALL to be decided before resuming
                if (pendingCount == 0) {
                    String approvalType = isPerUserApproval ? "per-user" : "role owner";
                    logger.info()
                        .message("All " + approvalType + " approvals decided - resuming workflow")
                        .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                        .detail("approvedCount", approvedCount)
                        .detail("rejectedCount", rejectedCount)
                        .detail("totalApprovals", totalApprovals)
                        .log();

                    // Update with multi-approval data including user/owner ID lists
                    updateNodeExecutionWithMultiApprovalData(
                        approval,
                        approverId,
                        comments,
                        approval.getApprovalPolicy(),
                        approvedCount >= totalApprovals, // finalApproved - doesn't matter for per-user, we have the lists
                        approvedCount,
                        rejectedCount,
                        pendingCount,
                        totalApprovals,
                        approval.getRequiredApprovals(),
                        "All " + approvalType + " approvals completed",
                        allApprovals
                    );

                    executionIdToResume = approval.getExecutionId();
                } else {
                    String approvalType = isPerUserApproval ? "per-user" : "role owner";
                    logger.info()
                        .message("Waiting for remaining " + approvalType + " approvals")
                        .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                        .detail("pendingCount", pendingCount)
                        .detail("rejectedCount", rejectedCount)
                        .detail("approvedCount", approvedCount)
                        .detail("totalApprovals", totalApprovals)
                        .log();
                }
            } else {
                // Standard multi-approval - use original ALL policy behavior (resume immediately on rejection)
                logger.info()
                    .message(LOG_APPROVAL_REJECTED_RESUMING)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("executionId", approval.getExecutionId().toString())
                    .log();

                updateNodeExecutionWithRejectionData(approval, approverId, comments);
                executionIdToResume = approval.getExecutionId();
            }
        } else {
            // Single approval - resume immediately with rejection
            logger.info()
                .message(LOG_APPROVAL_REJECTED_RESUMING)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("executionId", approval.getExecutionId().toString())
                .log();

            updateNodeExecutionWithRejectionData(approval, approverId, comments);
            executionIdToResume = approval.getExecutionId();
        }

        // CRITICAL: Force flush to ensure node execution update is persisted before async resume
        // This prevents race condition where resume() runs before output_data is saved
        if (executionIdToResume != null) {
            nodeExecutionRepository.flush();
            logger.debug()
                .message(LOG_FLUSHED_NODE_EXECUTION_UPDATES)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .log();
        }

        // Register post-commit callback to resume workflow AFTER transaction commits
        // This ensures the rejection output data is visible to the resumed workflow
        final UUID finalExecutionIdToResume = executionIdToResume;
        if (finalExecutionIdToResume != null) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        logger.info()
                            .message(LOG_TRANSACTION_COMMITTED_RESUMING_REJECTION)
                            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                            .detail("executionId", finalExecutionIdToResume.toString())
                            .log();
                        asyncExecutionDispatcher.resume(finalExecutionIdToResume);
                    }
                }
            );
            logger.info()
                .message(LOG_WORKFLOW_RESUME_AFTER_REJECTION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("executionId", approval.getExecutionId().toString())
                .log();
        }

        // Send notification to workflow requester (they need to know their request was rejected)
        try {
            WorkflowExecution execution = executionRepository.findById(approval.getExecutionId())
                .orElse(null);
            Workflow workflow = execution != null && execution.getWorkflowId() != null
                ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                : null;

            if (execution != null && execution.getTriggeredBy() != null) {
                String workflowName = workflow != null ? workflow.getName() : "Workflow";
                UUID requesterId = execution.getTriggeredBy();

                // TODO: Fetch real requester and approver details from identity-service
                notificationClient.sendApprovalDecisionNotification(
                    getCurrentTenantId(),
                    requesterId.toString(), // Send to REQUESTER, not approver
                    DEFAULT_REQUESTER_EMAIL, // TODO: Get from identity-service
                    DEFAULT_REQUESTER_NAME, // TODO: Get from identity-service
                    workflowName,
                    approval.getExecutionId().toString(),
                    approverId.toString(),
                    false, // rejected
                    comments != null ? comments : DEFAULT_APPROVAL_COMMENT_REJECTED
                );

                logger.info()
                    .message(LOG_REJECTION_NOTIFICATION_SENT)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("requesterId", requesterId.toString())
                    .log();
            }
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_REJECTION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
            // Don't fail the rejection if notification fails
        }

        return savedApproval;
    }

    /**
     * Update node execution with approval data
     * Industry Standard: Node completes with approval output
     */
    private void updateNodeExecutionWithApprovalData(WorkflowApproval approval, UUID approvedBy, String comments) {
        try {
            // Find the PAUSED node execution for this approval
            // Note: nodeExecutionId might be null in approval, so we find by execution + status PAUSED
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository
                .findByExecutionId(approval.getExecutionId()).stream()
                .filter(ne -> ne.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED)
                .toList();

            if (nodeExecutions.isEmpty()) {
                logger.warn()
                    .message(LOG_NO_PAUSED_NODE_EXECUTION_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvalId", approval.getId().toString())
                    .detail("executionId", approval.getExecutionId().toString())
                    .log();
                return;
            }

            WorkflowNodeExecution nodeExecution = nodeExecutions.get(0);
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("approved", true);
            outputData.put("rejected", false);
            outputData.put("decision", "APPROVED");
            outputData.put("approvalId", approval.getId().toString());
            outputData.put("approverId", approval.getApproverId().toString());
            outputData.put("approvedBy", approvedBy.toString());
            outputData.put("comments", comments != null ? comments : "No comments provided");
            outputData.put("respondedAt", approval.getRespondedAt().toString());

            nodeExecution.setOutputData(outputData);
            nodeExecutionRepository.save(nodeExecution);

            logger.info()
                .message(LOG_UPDATED_NODE_WITH_APPROVAL_DATA)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("nodeExecutionId", nodeExecution.getId().toString())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_UPDATE_NODE_APPROVAL_DATA)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Update node execution with multi-approval POLICY-AWARE output data
     * This provides downstream nodes with complete policy evaluation results
     * for conditional branching based on approval outcomes
     */
    private void updateNodeExecutionWithMultiApprovalData(
            WorkflowApproval triggeringApproval,
            UUID lastRespondedBy,
            String lastComments,
            com.aidentx.workflowsservice.enums.ApprovalPolicy policy,
            boolean finalApproved,
            long approvedCount,
            long rejectedCount,
            long pendingCount,
            long totalApprovals,
            Integer requiredApprovals,
            String completionReason,
            List<WorkflowApproval> allApprovals) {
        try {
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository
                .findByExecutionId(triggeringApproval.getExecutionId()).stream()
                .filter(ne -> ne.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED)
                .toList();

            if (nodeExecutions.isEmpty()) {
                logger.warn()
                    .message(LOG_NO_PAUSED_NODE_EXECUTION_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvalId", triggeringApproval.getId().toString())
                    .detail("executionId", triggeringApproval.getExecutionId().toString())
                    .log();
                return;
            }

            WorkflowNodeExecution nodeExecution = nodeExecutions.get(0);
            Map<String, Object> outputData = new HashMap<>();

            // Core decision fields - these are the primary fields for conditional branching
            outputData.put("approved", finalApproved);
            outputData.put("rejected", !finalApproved);
            outputData.put("decision", finalApproved ? "APPROVED" : "REJECTED");

            // Policy information - allows downstream nodes to understand HOW the decision was made
            outputData.put("policy", policy.name());
            outputData.put("policyDescription", getPolicyDescription(policy));
            outputData.put("completionReason", completionReason);

            // Vote counts - useful for audit trails and conditional logic
            outputData.put("approvedCount", approvedCount);
            outputData.put("rejectedCount", rejectedCount);
            outputData.put("pendingCount", pendingCount);
            outputData.put("totalApprovers", totalApprovals);

            // Threshold-specific (if applicable)
            if (policy == com.aidentx.workflowsservice.enums.ApprovalPolicy.THRESHOLD && requiredApprovals != null) {
                outputData.put("requiredApprovals", requiredApprovals);
                outputData.put("thresholdMet", approvedCount >= requiredApprovals);
                outputData.put("thresholdPercentage", Math.round((double) approvedCount / requiredApprovals * 100));
            }

            // Last response details
            outputData.put("lastRespondedBy", lastRespondedBy.toString());
            outputData.put("lastComments", lastComments != null ? lastComments : "No comments provided");
            outputData.put("respondedAt", triggeringApproval.getRespondedAt() != null
                ? triggeringApproval.getRespondedAt().toString()
                : OffsetDateTime.now().toString());

            // Detailed approver breakdown - for audit and detailed reporting
            List<Map<String, Object>> approverDetails = allApprovals.stream()
                .map(a -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("approverId", a.getApproverId().toString());
                    detail.put("decision", a.getDecision() != null ? a.getDecision().name() : "PENDING");
                    detail.put("comments", a.getComments());
                    detail.put("respondedAt", a.getRespondedAt() != null ? a.getRespondedAt().toString() : null);
                    // Include target user info if available (for structured approvers)
                    if (a.getEvidenceBundle() != null) {
                        if (a.getEvidenceBundle().get("targetUserId") != null) {
                            detail.put("targetUserId", a.getEvidenceBundle().get("targetUserId"));
                        }
                        if (a.getEvidenceBundle().get("targetUserName") != null) {
                            detail.put("targetUserName", a.getEvidenceBundle().get("targetUserName"));
                        }
                        if (a.getEvidenceBundle().get("targetUserEmail") != null) {
                            detail.put("targetUserEmail", a.getEvidenceBundle().get("targetUserEmail"));
                        }
                        if (a.getEvidenceBundle().get("targetUserFirstName") != null) {
                            detail.put("targetUserFirstName", a.getEvidenceBundle().get("targetUserFirstName"));
                        }
                        if (a.getEvidenceBundle().get("targetUserLastName") != null) {
                            detail.put("targetUserLastName", a.getEvidenceBundle().get("targetUserLastName"));
                        }
                        if (a.getEvidenceBundle().get("targetUserDisplayName") != null) {
                            detail.put("targetUserDisplayName", a.getEvidenceBundle().get("targetUserDisplayName"));
                        }
                    }
                    return detail;
                })
                .toList();
            outputData.put("approverDetails", approverDetails);

            // Collect approved/rejected user IDs for downstream workflow use
            // CRITICAL FIX: Apply policy logic PER USER to handle multi-role-owner scenarios correctly
            // ONLY set these if this approval node used structured approvers (user-manager pairs)
            // This prevents overwriting previous node's approvedUserIds with empty arrays
            List<String> approvedUserIds = new ArrayList<>();
            List<String> rejectedUserIds = new ArrayList<>();
            List<String> approvedOwnerIds = new ArrayList<>();
            List<String> rejectedOwnerIds = new ArrayList<>();
            boolean hasStructuredApprovers = false;
            boolean hasRoleOwnerApprovers = false;

            // Check if any approval is from a role owner
            for (WorkflowApproval approval : allApprovals) {
                if (approval.getApproverType() == WorkflowApproval.ApproverType.ROLE_OWNER) {
                    hasRoleOwnerApprovers = true;
                    break;
                }
            }

            // Group decisions by targetUserId to count approvals/rejections per user
            Map<String, List<WorkflowApproval>> decisionsByUser = new HashMap<>();

            for (WorkflowApproval approval : allApprovals) {
                if (approval.getEvidenceBundle() != null && approval.getEvidenceBundle().get("targetUserId") != null) {
                    hasStructuredApprovers = true;
                    String targetUserId = approval.getEvidenceBundle().get("targetUserId").toString();
                    decisionsByUser.computeIfAbsent(targetUserId, k -> new ArrayList<>()).add(approval);
                }
            }

            // Apply policy logic per user to determine if they're approved or rejected
            if (hasStructuredApprovers) {
                for (Map.Entry<String, List<WorkflowApproval>> entry : decisionsByUser.entrySet()) {
                    String targetUserId = entry.getKey();
                    List<WorkflowApproval> userApprovals = entry.getValue();

                    long userApprovedCount = userApprovals.stream()
                        .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED)
                        .count();
                    long userRejectedCount = userApprovals.stream()
                        .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED)
                        .count();
                    long userTotalDecisions = userApprovedCount + userRejectedCount;

                    // Apply the same policy logic as overall approval decision, but per user
                    boolean userApproved = switch (policy) {
                        case ALL -> userApprovedCount == userApprovals.size() && userRejectedCount == 0;
                        case ANY -> userApprovedCount > 0;
                        case THRESHOLD -> userApprovedCount >= (requiredApprovals != null ? requiredApprovals : userApprovals.size());
                        case FAIL_FAST -> userRejectedCount == 0 && userApprovedCount == userApprovals.size();
                    };

                    if (userApproved) {
                        approvedUserIds.add(targetUserId);
                    } else {
                        rejectedUserIds.add(targetUserId);
                    }
                }
            }

            // ONLY output these lists if this node used structured approvers
            // If not, don't touch these keys to preserve any values from previous nodes
            if (hasStructuredApprovers) {
                outputData.put("approvedUserIds", approvedUserIds);
                outputData.put("rejectedUserIds", rejectedUserIds);

                logger.info()
                    .message("Collected policy-aware user approval results from structured approvers")
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("policy", policy.name())
                    .detail("approvedUserIds", approvedUserIds)
                    .detail("rejectedUserIds", rejectedUserIds)
                    .detail("totalUsers", decisionsByUser.size())
                    .log();
            }

            // Collect role owner approvals separately for clear separation of concerns
            // approvedOwnerIds/rejectedOwnerIds are specifically for role owner approvals
            // This prevents confusion with approvedUserIds (used for user-manager approvals)
            if (hasRoleOwnerApprovers) {
                for (WorkflowApproval approval : allApprovals) {
                    if (approval.getApproverType() == WorkflowApproval.ApproverType.ROLE_OWNER) {
                        String ownerId = approval.getApproverId().toString();
                        if (approval.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED) {
                            approvedOwnerIds.add(ownerId);
                        } else if (approval.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED) {
                            rejectedOwnerIds.add(ownerId);
                        }
                    }
                }

                outputData.put("approvedOwnerIds", approvedOwnerIds);
                outputData.put("rejectedOwnerIds", rejectedOwnerIds);

                logger.info()
                    .message("Collected role owner approval results")
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("policy", policy.name())
                    .detail("approvedOwnerIds", approvedOwnerIds)
                    .detail("rejectedOwnerIds", rejectedOwnerIds)
                    .detail("totalOwners", allApprovals.size())
                    .log();
            }

            // Approval group metadata
            outputData.put("approvalGroupId", triggeringApproval.getApprovalGroupId().toString());
            outputData.put("isMultiApproval", true);

            nodeExecution.setOutputData(outputData);
            nodeExecutionRepository.save(nodeExecution);

            logger.info()
                .message("Updated node with multi-approval policy-aware data")
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("nodeExecutionId", nodeExecution.getId().toString())
                .detail("policy", policy.name())
                .detail("finalApproved", finalApproved)
                .detail("approvedCount", approvedCount)
                .detail("rejectedCount", rejectedCount)
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to update node with multi-approval data")
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Get human-readable description of approval policy
     */
    private String getPolicyDescription(com.aidentx.workflowsservice.enums.ApprovalPolicy policy) {
        return switch (policy) {
            case ALL -> "All approvers must approve";
            case ANY -> "Any single approver can approve";
            case THRESHOLD -> "Required number of approvals must be met";
            case FAIL_FAST -> "First rejection immediately rejects";
        };
    }

    /**
     * Update node execution with rejection data
     * Industry Standard: Node completes with rejection output, workflow continues
     */
    private void updateNodeExecutionWithRejectionData(WorkflowApproval approval, UUID rejectedBy, String comments) {
        try {
            // Find the PAUSED node execution for this approval
            // Note: nodeExecutionId might be null in approval, so we find by execution + status PAUSED
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository
                .findByExecutionId(approval.getExecutionId()).stream()
                .filter(ne -> ne.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED)
                .toList();

            if (nodeExecutions.isEmpty()) {
                logger.warn()
                    .message(LOG_NO_PAUSED_NODE_EXECUTION_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvalId", approval.getId().toString())
                    .detail("executionId", approval.getExecutionId().toString())
                    .log();
                return;
            }

            WorkflowNodeExecution nodeExecution = nodeExecutions.get(0);

            // Update output data with rejection information
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("approved", false);
            outputData.put("rejected", true);
            outputData.put("decision", "REJECTED");
            outputData.put("approvalId", approval.getId().toString());
            outputData.put("approverId", approval.getApproverId().toString());
            outputData.put("rejectedBy", rejectedBy.toString());
            outputData.put("comments", comments != null ? comments : "No comments provided");
            outputData.put("respondedAt", approval.getRespondedAt().toString());

            // CRITICAL FIX: Collect approved/rejected user IDs for multi-approval with structured approvers
            // Apply policy logic PER USER to handle multi-role-owner scenarios correctly
            // This is needed even when rejecting early, to support chained approval workflows
            if (approval.getApprovalGroupId() != null) {
                List<WorkflowApproval> allApprovals = approvalRepository.findByApprovalGroupId(approval.getApprovalGroupId());

                // Get approval policy from first approval in group
                WorkflowApproval firstApproval = allApprovals.get(0);
                com.aidentx.workflowsservice.enums.ApprovalPolicy policy =
                    firstApproval.getApprovalPolicy() != null
                        ? firstApproval.getApprovalPolicy()
                        : com.aidentx.workflowsservice.enums.ApprovalPolicy.ALL;
                Integer requiredApprovals = firstApproval.getRequiredApprovals();

                List<String> approvedUserIds = new ArrayList<>();
                List<String> rejectedUserIds = new ArrayList<>();
                boolean hasStructuredApprovers = false;

                // Group decisions by targetUserId to count approvals/rejections per user
                Map<String, List<WorkflowApproval>> decisionsByUser = new HashMap<>();

                for (WorkflowApproval groupApproval : allApprovals) {
                    if (groupApproval.getEvidenceBundle() != null && groupApproval.getEvidenceBundle().get("targetUserId") != null) {
                        hasStructuredApprovers = true;
                        String targetUserId = groupApproval.getEvidenceBundle().get("targetUserId").toString();
                        decisionsByUser.computeIfAbsent(targetUserId, k -> new ArrayList<>()).add(groupApproval);
                    }
                }

                // Apply policy logic per user
                if (hasStructuredApprovers) {
                    for (Map.Entry<String, List<WorkflowApproval>> entry : decisionsByUser.entrySet()) {
                        String targetUserId = entry.getKey();
                        List<WorkflowApproval> userApprovals = entry.getValue();

                        long userApprovedCount = userApprovals.stream()
                            .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED)
                            .count();
                        long userRejectedCount = userApprovals.stream()
                            .filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED)
                            .count();

                        // Apply the same policy logic as overall approval decision, but per user
                        boolean userApproved = switch (policy) {
                            case ALL -> userApprovedCount == userApprovals.size() && userRejectedCount == 0;
                            case ANY -> userApprovedCount > 0;
                            case THRESHOLD -> userApprovedCount >= (requiredApprovals != null ? requiredApprovals : userApprovals.size());
                            case FAIL_FAST -> userRejectedCount == 0 && userApprovedCount == userApprovals.size();
                        };

                        if (userApproved) {
                            approvedUserIds.add(targetUserId);
                        } else {
                            rejectedUserIds.add(targetUserId);
                        }
                    }
                }

                if (hasStructuredApprovers) {
                    outputData.put("approvedUserIds", approvedUserIds);
                    outputData.put("rejectedUserIds", rejectedUserIds);

                    logger.info()
                        .message("Collected policy-aware user approval results on early rejection")
                        .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                        .detail("policy", policy.name())
                        .detail("approvedUserIds", approvedUserIds)
                        .detail("rejectedUserIds", rejectedUserIds)
                        .log();
                }
            }

            nodeExecution.setOutputData(outputData);
            nodeExecutionRepository.save(nodeExecution);

            logger.info()
                .message(LOG_UPDATED_NODE_WITH_REJECTION_DATA)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("nodeExecutionId", nodeExecution.getId().toString())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_UPDATE_NODE_REJECTION_DATA)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Delegate an approval to another user
     */
    @Transactional
    public WorkflowApproval delegate(UUID approvalId, UUID delegateToUserId, UUID currentApproverId) {
        logger.info()
            .message(LOG_DELEGATING_APPROVAL)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
            .objectId(approvalId.toString())
            .detail("currentApproverId", currentApproverId.toString())
            .detail("delegateToUserId", delegateToUserId.toString())
            .log();

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_APPROVAL_NOT_FOUND + approvalId));

        // Validate current approver
        if (!approval.getApproverId().equals(currentApproverId)) {
            throw new IllegalArgumentException(ERROR_APPROVAL_NOT_AUTHORIZED_DELEGATE);
        }

        // Check if already decided
        if (approval.getDecision() != WorkflowApproval.ApprovalDecision.PENDING &&
            approval.getDecision() != WorkflowApproval.ApprovalDecision.DELEGATED) {
            throw new IllegalStateException(ERROR_APPROVAL_CANNOT_DELEGATE_DECIDED);
        }

        // Initialize delegation chain if this is the first delegation
        if (approval.getDelegationChain() == null) {
            approval.setDelegationChain(new ArrayList<>());
        }

        // Add delegation record to the chain
        WorkflowApproval.DelegationRecord delegationRecord = WorkflowApproval.DelegationRecord.builder()
                .fromApproverId(currentApproverId)
                .toApproverId(delegateToUserId)
                .delegatedAt(OffsetDateTime.now())
                .reason(DEFAULT_DELEGATION_REASON)  // Could be added as a parameter if needed
                .sequenceNumber(approval.getDelegationChain().size() + 1)
                .build();

        approval.getDelegationChain().add(delegationRecord);

        // Update approver to the delegated user
        approval.setApproverId(delegateToUserId);
        approval.setDelegatedTo(delegateToUserId);
        // Keep status as PENDING so it can be delegated again if needed

        WorkflowApproval savedApproval = approvalRepository.save(approval);

        // Send notification to the new delegated approver
        try {
            WorkflowExecution execution = executionRepository.findById(approval.getExecutionId())
                .orElse(null);
            Workflow workflow = execution != null && execution.getWorkflowId() != null
                ? workflowRepository.findById(execution.getWorkflowId()).orElse(null)
                : null;

            String workflowName = workflow != null ? workflow.getName() : "Workflow";
            String requestedBy = execution != null && execution.getTriggeredBy() != null
                ? execution.getTriggeredBy().toString()
                : "System";

            // Fetch delegatee details from user-service via gRPC
            String delegateeEmail = DEFAULT_APPROVER_EMAIL;
            String delegateeName = DEFAULT_APPROVER_NAME;

            try {
                var userResponse = userClient.getUserById(delegateToUserId.toString(), getCurrentTenantId());
                if (userResponse != null && userResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userResponse;
                    delegateeEmail = userMap.get("email") != null ? (String) userMap.get("email") : delegateeEmail;
                    delegateeName = userMap.get("fullName") != null ? (String) userMap.get("fullName") : delegateeName;
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_DELEGATEE_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("error", e.getMessage())
                    .log();
            }

            // Send notification to the new approver
            notificationClient.sendApprovalRequestNotification(
                getCurrentTenantId(),
                delegateToUserId.toString(),
                delegateeEmail,
                delegateeName,
                workflowName,
                approval.getExecutionId().toString(),
                savedApproval.getId().toString(),
                requestedBy
            );

            logger.info()
                .message(LOG_DELEGATION_NOTIFICATION_SENT)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("delegateToUserId", delegateToUserId.toString())
                .log();
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_DELEGATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail("error", e.getMessage())
                .log();
            // Don't fail the delegation if notification fails
        }

        return savedApproval;
    }

    /**
     * Get approval by ID
     * @throws ResourceNotFoundException if approval not found (returns 404)
     */
    public WorkflowApproval getApproval(UUID approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", approvalId));
    }

    /**
     * Get all approvals for an execution
     */
    public List<WorkflowApproval> getApprovalsForExecution(UUID executionId) {
        return approvalRepository.findByExecutionId(executionId);
    }

    /**
     * Get pending approvals for a user (including delegated)
     */
    public List<WorkflowApproval> getPendingApprovalsForUser(UUID userId, String tenantId) {
        return approvalRepository.findAllPendingApprovalsForUser(userId, tenantId);
    }

    /**
     * Get processed (non-pending) approvals for a user.
     * Returns approval history including APPROVED, REJECTED, TIMEOUT decisions.
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param decision Optional filter by specific decision (APPROVED, REJECTED, TIMEOUT)
     * @return List of processed approvals ordered by most recent first
     */
    public List<WorkflowApproval> getProcessedApprovalsForUser(UUID userId, String tenantId, String decision) {
        if (decision != null && !decision.isEmpty() && !"all".equalsIgnoreCase(decision)) {
            try {
                WorkflowApproval.ApprovalDecision decisionEnum = WorkflowApproval.ApprovalDecision.valueOf(decision.toUpperCase());
                return approvalRepository.findProcessedApprovalsForUserByDecision(userId, tenantId, decisionEnum);
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message("Invalid decision filter, returning all processed approvals")
                    .detail("decision", decision)
                    .log();
            }
        }
        return approvalRepository.findProcessedApprovalsForUser(userId, tenantId);
    }

    /**
     * Get approvals by approver
     */
    public List<WorkflowApproval> getApprovalsByApprover(UUID approverId, String tenantId) {
        return approvalRepository.findByApproverIdAndTenantId(approverId, tenantId);
    }

    /**
     * Process timed-out approvals
     * Should be called by a scheduled job
     */
    @Transactional
    public void processTimeouts() {
        logger.info()
            .message(LOG_PROCESSING_TIMED_OUT_APPROVALS)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .log();

        List<WorkflowApproval> timedOut = approvalRepository.findTimedOutApprovals(OffsetDateTime.now());

        for (WorkflowApproval approval : timedOut) {
            if (approval.getDecision() == WorkflowApproval.ApprovalDecision.PENDING) {
                approval.setDecision(WorkflowApproval.ApprovalDecision.TIMEOUT);
                approval.setRespondedAt(OffsetDateTime.now());
                approvalRepository.save(approval);

                logger.warn()
                    .message(LOG_APPROVAL_TIMED_OUT)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail("approvalId", approval.getId().toString())
                    .detail("executionId", approval.getExecutionId().toString())
                    .log();
            }
        }

        logger.info()
            .message(LOG_PROCESSED_TIMED_OUT_APPROVALS)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .detail("processedCount", String.valueOf(timedOut.size()))
            .log();
    }

    /**
     * Check if all approvals for a node execution are complete
     */
    public boolean areAllApprovalsComplete(UUID executionId, UUID nodeExecutionId) {
        List<WorkflowApproval> approvals = approvalRepository.findByExecutionAndNode(executionId, nodeExecutionId);

        if (approvals.isEmpty()) {
            return false;
        }

        return approvals.stream()
                .allMatch(a -> a.getDecision() != WorkflowApproval.ApprovalDecision.PENDING &&
                              a.getDecision() != WorkflowApproval.ApprovalDecision.DELEGATED);
    }

    /**
     * Check if any approval for a node execution was rejected
     */
    public boolean isAnyApprovalRejected(UUID executionId, UUID nodeExecutionId) {
        List<WorkflowApproval> approvals = approvalRepository.findByExecutionAndNode(executionId, nodeExecutionId);

        return approvals.stream()
                .anyMatch(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED ||
                              a.getDecision() == WorkflowApproval.ApprovalDecision.TIMEOUT);
    }

    /**
     * Get approval statistics for a node execution
     */
    public Map<String, Long> getApprovalStats(UUID executionId, UUID nodeExecutionId) {
        List<WorkflowApproval> approvals = approvalRepository.findByExecutionAndNode(executionId, nodeExecutionId);

        long pending = approvals.stream().filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.PENDING).count();
        long approved = approvals.stream().filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.APPROVED).count();
        long rejected = approvals.stream().filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.REJECTED).count();
        long timeout = approvals.stream().filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.TIMEOUT).count();
        long delegated = approvals.stream().filter(a -> a.getDecision() == WorkflowApproval.ApprovalDecision.DELEGATED).count();

        return Map.of(
                STATS_KEY_TOTAL, (long) approvals.size(),
                STATS_KEY_PENDING, pending,
                STATS_KEY_APPROVED, approved,
                STATS_KEY_REJECTED, rejected,
                STATS_KEY_TIMEOUT, timeout,
                STATS_KEY_DELEGATED, delegated
        );
    }

    /**
     * Build rich notification context for enterprise-grade approval notifications.
     * Extracts meaningful information from approval context for non-technical users.
     *
     * @param approvalContext the approval context map containing business data
     * @param execution the workflow execution
     * @param workflow the workflow definition
     * @param timeoutMinutes approval timeout in minutes
     * @param requestedBy who triggered the workflow
     * @param tenantId tenant ID for user lookups
     * @return populated ApprovalNotificationContext
     */
    private com.aidentx.workflowsservice.dto.notification.ApprovalNotificationContext buildApprovalNotificationContext(
            Map<String, Object> approvalContext,
            WorkflowExecution execution,
            Workflow workflow,
            Integer timeoutMinutes,
            String requestedBy,
            String tenantId) {

        var builder = com.aidentx.workflowsservice.dto.notification.ApprovalNotificationContext.builder();

        if (approvalContext == null) {
            return builder.requestType("Access Request").build();
        }

        // DEBUG: Log what keys are available in context for notification building
        Object itemObj = approvalContext.get("item");
        String itemUserName = null;
        if (itemObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) itemObj;
            itemUserName = (String) itemMap.get("userName");
        }
        logger.info()
            .message("Building approval notification context")
            .detail("contextKeys", approvalContext.keySet().toString())
            .detail("tileTitle", approvalContext.get("tileTitle"))
            .detail("roleName", approvalContext.get("roleName"))
            .detail("currentRoleName", approvalContext.get("currentRoleName"))
            .detail("createdBy", approvalContext.get("createdBy"))
            .detail("item", approvalContext.get("item"))
            .detail("item.userName", itemUserName)
            .detail("roleJustification", approvalContext.get("roleJustification"))
            .log();

        // === REQUESTER INFO (from context) ===
        String requesterName = getStringFromContext(approvalContext, "createdBy", "triggeredByName", "requesterName");
        if (requesterName != null) builder.requesterFullName(requesterName);

        String department = getStringFromContext(approvalContext, "department", "requesterDepartment");
        if (department != null) builder.requesterDepartment(department);

        // === TILE NAME (from context - access portal tile) ===
        String tileName = getStringFromContext(approvalContext, "tileTitle", "tileName");
        if (tileName != null) builder.tileName(tileName);

        // === ROLE/PERMISSION INFO (from context) ===
        // currentRoleName is set in forEach loop when iterating over roles
        // roleName comes from item in forEach loop (the actual role being assigned)
        String roleName = getStringFromContext(approvalContext, "currentRoleName", "roleName");

        // Also check item object from forEach loop for roleName
        if (roleName == null) {
            Object item = approvalContext.get("item");
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) item;
                roleName = getStringFromContext(itemMap, "roleName", "name");
            }
        }
        if (roleName != null) builder.roleName(roleName);

        String roleDesc = getStringFromContext(approvalContext, "roleDescription", "description", "tileDescription");
        if (roleDesc != null) builder.roleDescription(roleDesc);

        String risk = getStringFromContext(approvalContext, "riskLevel", "risk");
        if (risk != null) builder.riskLevel(risk.toUpperCase());

        String appName = getStringFromContext(approvalContext, "applicationName", "appName");
        if (appName != null) builder.applicationName(appName);

        // === BUSINESS JUSTIFICATION (from context) ===
        String justification = getStringFromContext(approvalContext, "justification", "roleJustification",
            "overallJustification", "businessJustification", "reason");
        if (justification != null) builder.justification(justification);

        // === TARGET USER (from forEach loop context) ===
        // First check 'item' object from forEach loop - it now contains userName
        String targetUser = null;
        Object item = approvalContext.get("item");
        if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) item;
            // userName is now included in the item from access-portal-service
            targetUser = getStringFromContext(itemMap, "userName", "userFullName", "displayName");
        }

        // Fallback to context-level keys
        if (targetUser == null) {
            targetUser = getStringFromContext(approvalContext, "currentUserName", "currentUserFullName",
                "userName", "userFullName", "targetUserName");
        }

        if (targetUser != null) {
            builder.targetUsers(targetUser);
            builder.targetUserCount(1);
        } else {
            // Count from userIds if available
            Object userIds = approvalContext.get("userIds");
            if (userIds instanceof List) {
                int count = ((List<?>) userIds).size();
                builder.targetUsers(count + " user" + (count > 1 ? "s" : ""));
                builder.targetUserCount(count);
            }
        }

        // === REQUEST TYPE ===
        String actionName = getStringFromContext(approvalContext, "actionName", "requestType");
        if (actionName != null) {
            builder.requestType(actionName);
        } else {
            String wfName = workflow != null ? workflow.getName() : "";
            if (wfName.toLowerCase().contains("permission")) {
                builder.requestType("Permission Assignment");
            } else if (wfName.toLowerCase().contains("role")) {
                builder.requestType("Role Assignment");
            } else {
                builder.requestType("Access Request");
            }
        }

        // === TIMESTAMP (now) ===
        builder.requestedAt(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")
            .format(java.time.OffsetDateTime.now()));

        // === DEADLINE (from timeout) ===
        if (timeoutMinutes != null && timeoutMinutes > 0) {
            if (timeoutMinutes >= 1440) {
                int days = timeoutMinutes / 1440;
                builder.deadlineAt("Respond within " + days + " day" + (days > 1 ? "s" : ""));
            } else if (timeoutMinutes >= 60) {
                int hours = timeoutMinutes / 60;
                builder.deadlineAt("Respond within " + hours + " hour" + (hours > 1 ? "s" : ""));
            } else {
                builder.deadlineAt("Respond within " + timeoutMinutes + " minutes");
            }
        }

        return builder.build();
    }

    /** Simple helper to get first non-null string from context */
    private String getStringFromContext(Map<String, Object> ctx, String... keys) {
        for (String key : keys) {
            Object val = ctx.get(key);
            if (val != null && !val.toString().trim().isEmpty()) {
                return val.toString();
            }
        }
        return null;
    }

    /**
     * Extract target users from context - uses names directly without fetching
     */
    @SuppressWarnings("unchecked")
    private String extractTargetUsersFromContext(Map<String, Object> context) {
        // First try to get user names directly (preferred)
        String userNames = extractStringValue(context, "targetUserNames", "userNames", "selectedUserNames");
        if (userNames != null && !userNames.isEmpty()) {
            return userNames;
        }

        // Try to get from cart items with user names
        Object cartItems = context.get("cartItems");
        if (cartItems instanceof List) {
            List<String> names = new ArrayList<>();
            for (Object item : (List<?>) cartItems) {
                if (item instanceof Map) {
                    Map<String, Object> cartItem = (Map<String, Object>) item;
                    String userName = (String) cartItem.get("userName");
                    if (userName == null) userName = (String) cartItem.get("userFullName");
                    if (userName == null) userName = (String) cartItem.get("displayName");
                    if (userName != null) names.add(userName);
                }
            }
            if (!names.isEmpty()) {
                return formatUserNames(names);
            }
        }

        // Try single user context (forEach loop)
        String currentUserName = extractStringValue(context, "currentUserName", "userName", "userFullName");
        if (currentUserName != null) {
            return currentUserName;
        }

        // Last resort: if we only have IDs, just show count
        Object userIds = context.get("userIds");
        if (userIds instanceof List) {
            int count = ((List<?>) userIds).size();
            return count + " user" + (count > 1 ? "s" : "");
        }

        return null;
    }

    /**
     * Format list of user names nicely
     */
    private String formatUserNames(List<String> names) {
        if (names.isEmpty()) return null;
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        if (names.size() <= 3) {
            return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
        }
        return names.get(0) + ", " + names.get(1) + " (+" + (names.size() - 2) + " others)";
    }

    /**
     * Extract a string value from approval context, trying multiple possible keys.
     */
    private String extractStringValue(Map<String, Object> context, String... keys) {
        for (String key : keys) {
            Object value = getNestedValue(context, key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Get a potentially nested value from a map using dot notation.
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> context, String key) {
        if (context == null || key == null) return null;

        // Try direct key first
        if (context.containsKey(key)) {
            return context.get(key);
        }

        // Try nested key (e.g., "role.name")
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nested = context.get(parts[0]);
            if (nested instanceof Map) {
                return getNestedValue((Map<String, Object>) nested, parts[1]);
            }
        }

        return null;
    }

    /**
     * Extract target users from approval context and resolve names.
     */
    @SuppressWarnings("unchecked")
    private String extractTargetUsers(Map<String, Object> context, String tenantId) {
        // Try different keys for target users
        Object usersObj = context.get("targetUsers");
        if (usersObj == null) usersObj = context.get("users");
        if (usersObj == null) usersObj = context.get("selectedUsers");
        if (usersObj == null) usersObj = context.get("userIds");

        // Also check for single user in forEach context
        if (usersObj == null) {
            Object currentUserId = context.get("currentUserId");
            if (currentUserId == null) currentUserId = context.get("userId");
            if (currentUserId != null) {
                usersObj = List.of(currentUserId.toString());
            }
        }

        if (usersObj == null) return null;

        List<String> userIds = new ArrayList<>();
        if (usersObj instanceof List) {
            for (Object item : (List<?>) usersObj) {
                if (item instanceof String) {
                    userIds.add((String) item);
                } else if (item instanceof Map) {
                    Map<String, Object> userMap = (Map<String, Object>) item;
                    if (userMap.containsKey("id")) userIds.add(userMap.get("id").toString());
                    else if (userMap.containsKey("userId")) userIds.add(userMap.get("userId").toString());
                }
            }
        } else if (usersObj instanceof String) {
            userIds.add((String) usersObj);
        }

        // Resolve user IDs to names
        List<String> userNames = new ArrayList<>();
        for (String userId : userIds) {
            try {
                var userResponse = userClient.getUserById(userId, tenantId);
                if (userResponse instanceof Map) {
                    Map<String, Object> userMap = (Map<String, Object>) userResponse;
                    String fullName = (String) userMap.get("fullName");
                    if (fullName != null) {
                        userNames.add(fullName);
                    } else {
                        String displayName = (String) userMap.getOrDefault("displayName",
                                             userMap.getOrDefault("username", userId));
                        userNames.add(displayName);
                    }
                }
            } catch (Exception e) {
                // If we can't resolve, use the first 8 chars of UUID to make it shorter
                userNames.add("User-" + userId.substring(0, 8));
            }
        }

        if (userNames.isEmpty()) return null;

        // Format user list nicely
        if (userNames.size() == 1) {
            return userNames.get(0);
        } else if (userNames.size() == 2) {
            return userNames.get(0) + " and " + userNames.get(1);
        } else if (userNames.size() <= 3) {
            return String.join(", ", userNames.subList(0, userNames.size() - 1)) +
                   " and " + userNames.get(userNames.size() - 1);
        } else {
            // More than 3 users - show first 2 and count
            return userNames.get(0) + ", " + userNames.get(1) + " (+" + (userNames.size() - 2) + " others)";
        }
    }
}