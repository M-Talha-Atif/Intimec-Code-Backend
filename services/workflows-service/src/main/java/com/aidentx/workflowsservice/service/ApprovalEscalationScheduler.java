package com.aidentx.workflowsservice.service;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.client.UserGrpcClient;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.aidentx.workflowsservice.entity.WorkflowApproval.ApprovalDecision;
import com.aidentx.workflowsservice.entity.WorkflowApproval.ApproverType;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.repository.WorkflowApprovalRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * ApprovalEscalationScheduler - Handles automatic escalation of timed-out approvals
 *
 * Phase 4 (Week 5) implementation for the multi-approval enhancement.
 *
 * This scheduler runs periodically to:
 * 1. Find pending approvals that have passed their timeout
 * 2. Check if escalation is configured for the approval node
 * 3. Create new approval(s) for escalation approvers
 * 4. Mark original approval as escalated
 * 5. Send notifications to escalated approvers
 *
 * Configuration Example (in node data):
 * {
 *   "escalation": {
 *     "enabled": true,
 *     "afterMinutes": 720,         // 12 hours
 *     "strategy": "hierarchy",     // hierarchy | role | static
 *     "config": {
 *       "hierarchyType": "manager",
 *       "levels": 1
 *     },
 *     "maxEscalationLevels": 3,    // Maximum escalation depth
 *     "notifyOriginalApprover": true
 *   }
 * }
 *
 * @author AidentX Development Team
 * @since 2.0 (Phase 4: Escalation Enhancement)
 */
@Service
public class ApprovalEscalationScheduler {

    private final IUnifiedLogger logger;
    private final WorkflowApprovalRepository approvalRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final ApproverResolutionService approverResolutionService;
    private final NotificationKafkaClient notificationClient;
    private final UserGrpcClient userClient;

    private static final String DEFAULT_APPROVER_EMAIL = "approver@aidentx.com";
    private static final String DEFAULT_APPROVER_NAME = "Approver";
    private static final int DEFAULT_MAX_ESCALATION_LEVELS = 3;
    private static final String CONTEXT_KEY_TENANT_ID = "tenantId";

    public ApprovalEscalationScheduler(
            WorkflowApprovalRepository approvalRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowExecutionRepository executionRepository,
            ApproverResolutionService approverResolutionService,
            NotificationKafkaClient notificationClient,
            UserGrpcClient userClient,
            UnifiedLoggerFactory loggerFactory) {
        this.approvalRepository = approvalRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.executionRepository = executionRepository;
        this.approverResolutionService = approverResolutionService;
        this.notificationClient = notificationClient;
        this.userClient = userClient;
        this.logger = loggerFactory.getLogger(ApprovalEscalationScheduler.class, LOG_CATEGORY_WORKFLOW_APPROVAL);
    }

    /**
     * Extract tenant ID from execution context.
     */
    private String getTenantIdFromExecution(WorkflowExecution execution) {
        if (execution != null && execution.getExecutionContext() != null) {
            Object tenantId = execution.getExecutionContext().get(CONTEXT_KEY_TENANT_ID);
            if (tenantId != null) {
                return tenantId.toString();
            }
        }
        return null;
    }

    /**
     * Scheduled job to check for timed-out approvals and escalate them.
     * Runs every 15 minutes.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void checkApprovalTimeouts() {
        logger.info()
            .message(LOG_ESCALATION_CHECK_STARTED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .log();

        OffsetDateTime now = OffsetDateTime.now();
        List<WorkflowApproval> timedOutApprovals = approvalRepository.findPendingApprovalsPassedTimeout(now);

        logger.info()
            .message(LOG_ESCALATION_APPROVALS_FOUND)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .detail(DETAIL_KEY_COUNT, String.valueOf(timedOutApprovals.size()))
            .log();

        int escalatedCount = 0;
        int skippedCount = 0;

        for (WorkflowApproval approval : timedOutApprovals) {
            try {
                boolean escalated = escalateApproval(approval);
                if (escalated) {
                    escalatedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                logger.error()
                    .message(LOG_ESCALATION_FAILED)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                    .objectId(approval.getId().toString())
                    .detail(DETAIL_KEY_ERROR, e.getMessage())
                    .log();
            }
        }

        logger.info()
            .message(LOG_ESCALATION_CHECK_COMPLETED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .detail("escalatedCount", String.valueOf(escalatedCount))
            .detail("skippedCount", String.valueOf(skippedCount))
            .log();
    }

    /**
     * Escalate a single approval to the next level approver.
     *
     * @param approval The approval that has timed out
     * @return true if escalation was performed, false if skipped
     */
    @SuppressWarnings("unchecked")
    private boolean escalateApproval(WorkflowApproval approval) {
        // Get node configuration
        Optional<WorkflowNodeExecution> nodeExecOpt = Optional.empty();
        if (approval.getNodeExecutionId() != null) {
            nodeExecOpt = nodeExecutionRepository.findById(approval.getNodeExecutionId());
        }

        if (nodeExecOpt.isEmpty()) {
            logger.warn()
                .message(LOG_ESCALATION_NO_NODE_CONFIG)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                .objectId(approval.getId().toString())
                .log();

            // Mark as timed out without escalation
            markAsTimedOut(approval);
            return false;
        }

        WorkflowNodeExecution nodeExec = nodeExecOpt.get();
        Map<String, Object> nodeData = nodeExec.getInputData();

        if (nodeData == null) {
            markAsTimedOut(approval);
            return false;
        }

        // Check if escalation is configured
        Map<String, Object> escalationConfig = (Map<String, Object>) nodeData.get(CONFIG_KEY_ESCALATION);

        if (escalationConfig == null || !Boolean.TRUE.equals(escalationConfig.get(CONFIG_KEY_ENABLED))) {
            logger.debug()
                .message(LOG_ESCALATION_NOT_CONFIGURED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                .objectId(approval.getId().toString())
                .log();

            markAsTimedOut(approval);
            return false;
        }

        // Check max escalation levels
        int maxLevels = escalationConfig.containsKey(CONFIG_KEY_MAX_ESCALATION_LEVELS)
            ? ((Number) escalationConfig.get(CONFIG_KEY_MAX_ESCALATION_LEVELS)).intValue()
            : DEFAULT_MAX_ESCALATION_LEVELS;

        int currentLevel = approval.getEscalationLevel() != null ? approval.getEscalationLevel() : 0;

        if (currentLevel >= maxLevels) {
            logger.warn()
                .message(LOG_ESCALATION_MAX_LEVEL_REACHED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                .objectId(approval.getId().toString())
                .detail("currentLevel", String.valueOf(currentLevel))
                .detail("maxLevels", String.valueOf(maxLevels))
                .log();

            markAsTimedOut(approval);
            return false;
        }

        // Get execution for context
        Optional<WorkflowExecution> executionOpt = executionRepository.findById(approval.getExecutionId());
        if (executionOpt.isEmpty()) {
            markAsTimedOut(approval);
            return false;
        }

        WorkflowExecution execution = executionOpt.get();

        // Resolve escalation approvers using the configured strategy
        List<String> escalatedApproverIds;
        try {
            // Prepare context with original approver info for hierarchy resolution
            Map<String, Object> escalationContext = approval.getApprovalContext() != null
                ? new java.util.HashMap<>(approval.getApprovalContext())
                : new java.util.HashMap<>();
            escalationContext.put(CONTEXT_KEY_USER_ID, approval.getApproverId().toString());
            escalationContext.put("originalApproverId", approval.getApproverId().toString());

            escalatedApproverIds = approverResolutionService.resolveApprovers(
                escalationConfig,
                escalationContext,
                execution
            );
        } catch (Exception e) {
            logger.error()
                .message(LOG_ESCALATION_RESOLUTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                .objectId(approval.getId().toString())
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();

            markAsTimedOut(approval);
            return false;
        }

        if (escalatedApproverIds == null || escalatedApproverIds.isEmpty()) {
            logger.warn()
                .message(LOG_ESCALATION_NO_APPROVERS_FOUND)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
                .objectId(approval.getId().toString())
                .log();

            markAsTimedOut(approval);
            return false;
        }

        // Create escalated approval(s)
        String workflowName = nodeData.containsKey("label")
            ? (String) nodeData.get("label")
            : "Approval Request";

        Integer escalationTimeoutMinutes = escalationConfig.containsKey(CONFIG_KEY_TIMEOUT_MINUTES)
            ? ((Number) escalationConfig.get(CONFIG_KEY_TIMEOUT_MINUTES)).intValue()
            : null;

        for (String escalatedApproverId : escalatedApproverIds) {
            createEscalatedApproval(
                approval,
                UUID.fromString(escalatedApproverId),
                currentLevel + 1,
                escalationTimeoutMinutes,
                workflowName,
                execution
            );
        }

        // Mark original approval as escalated
        approval.setDecision(ApprovalDecision.ESCALATED_TIMEOUT);
        approval.setEscalatedAt(OffsetDateTime.now());
        approval.setEscalationReason("Timed out after " +
            (approval.getTimeoutAt() != null ? "timeout period" : "escalation period"));
        approvalRepository.save(approval);

        // Optionally notify original approver
        if (Boolean.TRUE.equals(escalationConfig.get(CONFIG_KEY_NOTIFY_ORIGINAL_APPROVER))) {
            sendEscalationNotificationToOriginalApprover(approval, escalatedApproverIds, workflowName);
        }

        logger.info()
            .message(LOG_ESCALATION_CREATED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
            .objectId(approval.getId().toString())
            .detail("newApproverCount", String.valueOf(escalatedApproverIds.size()))
            .detail("escalationLevel", String.valueOf(currentLevel + 1))
            .log();

        return true;
    }

    /**
     * Create a new escalated approval for the specified approver.
     */
    private void createEscalatedApproval(
            WorkflowApproval originalApproval,
            UUID escalatedApproverId,
            int escalationLevel,
            Integer timeoutMinutes,
            String workflowName,
            WorkflowExecution execution) {

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime timeoutAt = timeoutMinutes != null
            ? now.plusMinutes(timeoutMinutes)
            : null;

        WorkflowApproval escalatedApproval = WorkflowApproval.builder()
            .executionId(originalApproval.getExecutionId())
            .nodeExecutionId(originalApproval.getNodeExecutionId())
            .approvalGroupId(originalApproval.getApprovalGroupId())
            .approvalPolicy(originalApproval.getApprovalPolicy())
            .requiredApprovals(originalApproval.getRequiredApprovals())
            .approverId(escalatedApproverId)
            .approverType(ApproverType.ESCALATED)
            .decision(ApprovalDecision.PENDING)
            .requestedAt(now)
            .timeoutAt(timeoutAt)
            .approvalContext(originalApproval.getApprovalContext())
            .evidenceBundle(originalApproval.getEvidenceBundle())
            .escalatedFromApprovalId(originalApproval.getId())
            .escalationLevel(escalationLevel)
            .escalationReason("Escalated from approval " + originalApproval.getId() +
                " (approver: " + originalApproval.getApproverId() + ")")
            .build();

        WorkflowApproval savedApproval = approvalRepository.save(escalatedApproval);

        // Send notification to escalated approver
        sendEscalationNotificationToNewApprover(
            savedApproval,
            originalApproval,
            workflowName,
            execution
        );
    }

    /**
     * Mark an approval as timed out without escalation.
     */
    private void markAsTimedOut(WorkflowApproval approval) {
        approval.setDecision(ApprovalDecision.TIMEOUT);
        approval.setRespondedAt(OffsetDateTime.now());
        approvalRepository.save(approval);

        logger.info()
            .message(LOG_APPROVAL_TIMED_OUT_NO_ESCALATION)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_WORKFLOW_APPROVAL)
            .objectId(approval.getId().toString())
            .log();
    }

    /**
     * Send notification to the new escalated approver.
     */
    private void sendEscalationNotificationToNewApprover(
            WorkflowApproval escalatedApproval,
            WorkflowApproval originalApproval,
            String workflowName,
            WorkflowExecution execution) {

        try {
            String tenantId = getTenantIdFromExecution(execution);
            String approverEmail = DEFAULT_APPROVER_EMAIL;
            String approverName = DEFAULT_APPROVER_NAME;
            String originalApproverName = DEFAULT_APPROVER_NAME;

            // Fetch approver details
            try {
                var userResponse = userClient.getUserById(
                    escalatedApproval.getApproverId().toString(),
                    tenantId
                );
                if (userResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userResponse;
                    approverEmail = (String) userMap.getOrDefault("email", approverEmail);
                    approverName = (String) userMap.getOrDefault("fullName", approverName);
                }

                var origUserResponse = userClient.getUserById(
                    originalApproval.getApproverId().toString(),
                    tenantId
                );
                if (origUserResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) origUserResponse;
                    originalApproverName = (String) userMap.getOrDefault("fullName", originalApproverName);
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_APPROVER_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail(DETAIL_KEY_ERROR, e.getMessage())
                    .log();
            }

            notificationClient.sendEscalationNotification(
                tenantId,
                escalatedApproval.getApproverId().toString(),
                approverEmail,
                approverName,
                originalApproverName,
                workflowName,
                execution.getId().toString(),
                escalatedApproval.getId().toString(),
                escalatedApproval.getEscalationLevel()
            );

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_ESCALATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();
        }
    }

    /**
     * Send notification to the original approver that their approval was escalated.
     */
    private void sendEscalationNotificationToOriginalApprover(
            WorkflowApproval originalApproval,
            List<String> escalatedApproverIds,
            String workflowName) {

        try {
            // Get tenant from execution
            Optional<WorkflowExecution> executionOpt = executionRepository.findById(originalApproval.getExecutionId());
            if (executionOpt.isEmpty()) {
                return;
            }

            String tenantId = getTenantIdFromExecution(executionOpt.get());
            String approverEmail = DEFAULT_APPROVER_EMAIL;
            String approverName = DEFAULT_APPROVER_NAME;

            try {
                var userResponse = userClient.getUserById(
                    originalApproval.getApproverId().toString(),
                    tenantId
                );
                if (userResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userResponse;
                    approverEmail = (String) userMap.getOrDefault("email", approverEmail);
                    approverName = (String) userMap.getOrDefault("fullName", approverName);
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_APPROVER_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail(DETAIL_KEY_ERROR, e.getMessage())
                    .log();
            }

            notificationClient.sendEscalationNotificationToOriginalApprover(
                tenantId,
                originalApproval.getApproverId().toString(),
                approverEmail,
                approverName,
                workflowName,
                escalatedApproverIds.size()
            );

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_ESCALATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();
        }
    }
}