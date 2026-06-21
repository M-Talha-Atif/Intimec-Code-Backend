package com.aidentx.workflowsservice.client;

import com.aidentx.workflowsservice.dto.notification.ApprovalNotificationContext;
import com.aidentx.workflowsservice.dto.notification.NotificationEventData;
import com.aidentx.workflowsservice.dto.notification.OperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka client for sending workflow notification events to core-service.
 * This client publishes notification events to Kafka topic which core-service consumes
 * and processes (sends emails, creates in-app notifications, etc.).
 * 
 * Follows team pattern from auth-service: KafkaTemplate<String, String> + manual JSON serialization
 *
 * @author aidentx Development Team
 * @version 1.0
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class NotificationKafkaClient {

    private final IUnifiedLogger logger;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${notification.kafka.topic:user-notifications}")
    private String notificationTopic;

    public NotificationKafkaClient(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(NotificationKafkaClient.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Sends a workflow approval notification to approvers.
     *
     * @param tenantId the tenant ID
     * @param approverUserId the approver's user ID
     * @param approverEmail the approver's email
     * @param approverName the approver's name
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     * @param approvalId the approval request ID
     * @param requestedBy the user who initiated the workflow
     */
    public void sendApprovalRequestNotification(
            String tenantId,
            String approverUserId,
            String approverEmail,
            String approverName,
            String workflowName,
            String workflowInstanceId,
            String approvalId,
            String requestedBy) {
        // Delegate to enhanced method with null context
        sendApprovalRequestNotification(tenantId, approverUserId, approverEmail, approverName,
            workflowName, workflowInstanceId, approvalId, requestedBy, null);
    }

    /**
     * Sends a rich workflow approval notification with full enterprise context.
     *
     * @param tenantId the tenant ID
     * @param approverUserId the approver's user ID
     * @param approverEmail the approver's email
     * @param approverName the approver's name
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     * @param approvalId the approval request ID
     * @param requestedBy the user who initiated the workflow
     * @param approvalContext rich context including role info, users, justification etc.
     */
    public void sendApprovalRequestNotification(
            String tenantId,
            String approverUserId,
            String approverEmail,
            String approverName,
            String workflowName,
            String workflowInstanceId,
            String approvalId,
            String requestedBy,
            ApprovalNotificationContext approvalContext) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(approverName);
            variables.setEmail(approverEmail);
            variables.setWorkflowName(workflowName);
            variables.setWorkflowInstanceId(workflowInstanceId);
            variables.setApprovalId(approvalId);
            variables.setRequestedBy(requestedBy);
            variables.setApprovalUrl("/workflows/approvals/" + approvalId);
            
            // Populate rich enterprise context if available
            if (approvalContext != null) {
                variables.setRequesterFullName(approvalContext.getRequesterFullName());
                variables.setRequesterDepartment(approvalContext.getRequesterDepartment());
                variables.setTileName(approvalContext.getTileName());
                variables.setRoleName(approvalContext.getRoleName());
                variables.setRoleDescription(approvalContext.getRoleDescription());
                variables.setRiskLevel(approvalContext.getRiskLevel());
                variables.setTargetUsers(approvalContext.getTargetUsers());
                variables.setTargetUserCount(approvalContext.getTargetUserCount());
                variables.setJustification(approvalContext.getJustification());
                variables.setRequestedAt(approvalContext.getRequestedAt());
                variables.setDeadlineAt(approvalContext.getDeadlineAt());
                variables.setApplicationName(approvalContext.getApplicationName());
                variables.setRequestType(approvalContext.getRequestType());
            }

            NotificationEventData.UserData userData = new NotificationEventData.UserData(approverEmail, approverName);
            
            // Create action buttons for approve/reject/delegate
            List<NotificationEventData.NotificationAction> actions = List.of(
                new NotificationEventData.NotificationAction(
                    "approve",
                    "Approve",
                    "APPROVE",
                    "/api/v1/workflows/approvals/" + approvalId + "/approve",
                    "POST",
                    "primary",
                    false
                ),
                new NotificationEventData.NotificationAction(
                    "reject",
                    "Reject",
                    "REJECT",
                    "/api/v1/workflows/approvals/" + approvalId + "/reject",
                    "POST",
                    "danger",
                    true
                ),
                new NotificationEventData.NotificationAction(
                    "delegate",
                    "Delegate",
                    "DELEGATE",
                    "/api/v1/workflows/approvals/" + approvalId + "/delegate",
                    "POST",
                    "secondary",
                    false
                )
            );
            
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(approverUserId, userData, variables, actions);

            NotificationEventData eventData = new NotificationEventData(
                OperationType.WORKFLOW_APPROVAL_REQUESTED,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowInstanceId, eventData);
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_APPROVAL_REQUEST_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Sends a workflow completion notification to the requester.
     *
     * @param tenantId the tenant ID
     * @param requesterUserId the requester's user ID
     * @param requesterEmail the requester's email
     * @param requesterName the requester's name
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     */
    public void sendWorkflowCompletedNotification(
            String tenantId,
            String requesterUserId,
            String requesterEmail,
            String requesterName,
            String workflowName,
            String workflowInstanceId) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(requesterName);
            variables.setEmail(requesterEmail);
            variables.setWorkflowName(workflowName);
            variables.setWorkflowInstanceId(workflowInstanceId);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(requesterEmail, requesterName);
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(requesterUserId, userData, variables);

            NotificationEventData eventData = new NotificationEventData(
                OperationType.WORKFLOW_COMPLETED,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowInstanceId, eventData);
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_WORKFLOW_COMPLETED_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Sends an approval decision notification (approved/rejected).
     *
     * @param tenantId the tenant ID
     * @param requesterUserId the requester's user ID
     * @param requesterEmail the requester's email
     * @param requesterName the requester's name
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     * @param approverName the approver's name
     * @param approved whether the approval was approved or rejected
     * @param comment the approval comment
     */
    public void sendApprovalDecisionNotification(
            String tenantId,
            String requesterUserId,
            String requesterEmail,
            String requesterName,
            String workflowName,
            String workflowInstanceId,
            String approverName,
            boolean approved,
            String comment) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(requesterName);
            variables.setEmail(requesterEmail);
            variables.setWorkflowName(workflowName);
            variables.setWorkflowInstanceId(workflowInstanceId);
            variables.setApproverName(approverName);
            variables.setComment(comment);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(requesterEmail, requesterName);
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(requesterUserId, userData, variables);

            OperationType operationType = approved ? 
                OperationType.WORKFLOW_APPROVAL_APPROVED : 
                OperationType.WORKFLOW_APPROVAL_REJECTED;

            NotificationEventData eventData = new NotificationEventData(
                operationType,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowInstanceId, eventData);
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_APPROVAL_DECISION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Sends a notification when a delegation rule is created.
     * Phase 3: Delegation Enhancement
     *
     * @param tenantId the tenant ID
     * @param delegateUserId the delegate's user ID (who will receive delegated approvals)
     * @param delegateEmail the delegate's email
     * @param delegateName the delegate's name
     * @param delegatorName the name of the user who is delegating
     * @param startDate the delegation start date
     * @param endDate the delegation end date
     * @param reason the reason for delegation
     */
    public void sendDelegationCreatedNotification(
            String tenantId,
            String delegateUserId,
            String delegateEmail,
            String delegateName,
            String delegatorName,
            String startDate,
            String endDate,
            String reason) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(delegateName);
            variables.setEmail(delegateEmail);
            variables.setDelegatorName(delegatorName);
            variables.setStartDate(startDate);
            variables.setEndDate(endDate);
            variables.setReason(reason);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(delegateEmail, delegateName);
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(delegateUserId, userData, variables);

            NotificationEventData eventData = new NotificationEventData(
                OperationType.DELEGATION_CREATED,
                tenantId,
                List.of(userNotification)
            );

            sendNotification("delegation-" + delegateUserId, eventData);
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_DELEGATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Sends a notification when an approval is automatically delegated.
     * Phase 3: Delegation Enhancement
     *
     * @param tenantId the tenant ID
     * @param delegateUserId the delegate's user ID
     * @param delegateEmail the delegate's email
     * @param delegateName the delegate's name
     * @param delegatorName the name of the original approver
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     * @param approvalId the approval ID
     */
    public void sendApprovalAutoDelegatedNotification(
            String tenantId,
            String delegateUserId,
            String delegateEmail,
            String delegateName,
            String delegatorName,
            String workflowName,
            String workflowInstanceId,
            String approvalId) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(delegateName);
            variables.setEmail(delegateEmail);
            variables.setDelegatorName(delegatorName);
            variables.setWorkflowName(workflowName);
            variables.setWorkflowInstanceId(workflowInstanceId);
            variables.setApprovalId(approvalId);
            variables.setApprovalUrl("/workflows/approvals/" + approvalId);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(delegateEmail, delegateName);
            
            // Create action buttons for approve/reject/delegate
            List<NotificationEventData.NotificationAction> actions = List.of(
                new NotificationEventData.NotificationAction(
                    "approve",
                    "Approve",
                    "APPROVE",
                    "/api/v1/workflows/approvals/" + approvalId + "/approve",
                    "POST",
                    "primary",
                    false
                ),
                new NotificationEventData.NotificationAction(
                    "reject",
                    "Reject",
                    "REJECT",
                    "/api/v1/workflows/approvals/" + approvalId + "/reject",
                    "POST",
                    "danger",
                    true
                ),
                new NotificationEventData.NotificationAction(
                    "delegate",
                    "Delegate",
                    "DELEGATE",
                    "/api/v1/workflows/approvals/" + approvalId + "/delegate",
                    "POST",
                    "secondary",
                    false
                )
            );
            
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(delegateUserId, userData, variables, actions);

            NotificationEventData eventData = new NotificationEventData(
                OperationType.APPROVAL_AUTO_DELEGATED,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowInstanceId, eventData);
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_DELEGATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    // ==================== Phase 4: Escalation Notifications ====================

    /**
     * Send notification to an escalated approver that they have received an escalated approval.
     * 
     * @param tenantId the tenant ID
     * @param approverUserId the escalated approver's user ID
     * @param approverEmail the escalated approver's email
     * @param approverName the escalated approver's name
     * @param originalApproverName the name of the original approver
     * @param workflowName the workflow name
     * @param workflowInstanceId the workflow instance ID
     * @param approvalId the approval ID
     * @param escalationLevel the current escalation level
     */
    public void sendEscalationNotification(
            String tenantId,
            String approverUserId,
            String approverEmail,
            String approverName,
            String originalApproverName,
            String workflowName,
            String workflowInstanceId,
            String approvalId,
            int escalationLevel) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(approverName);
            variables.setEmail(approverEmail);
            variables.setWorkflowName(workflowName);
            variables.setWorkflowInstanceId(workflowInstanceId);
            variables.setApprovalId(approvalId);
            variables.setOriginalApproverName(originalApproverName);
            variables.setEscalationLevel(escalationLevel);
            variables.setApprovalUrl("/workflows/approvals/" + approvalId);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(approverEmail, approverName);
            
            // Create action buttons for approve/reject/delegate
            List<NotificationEventData.NotificationAction> actions = List.of(
                new NotificationEventData.NotificationAction(
                    "approve",
                    "Approve",
                    "APPROVE",
                    "/api/v1/workflows/approvals/" + approvalId + "/approve",
                    "POST",
                    "primary",
                    false
                ),
                new NotificationEventData.NotificationAction(
                    "reject",
                    "Reject",
                    "REJECT",
                    "/api/v1/workflows/approvals/" + approvalId + "/reject",
                    "POST",
                    "danger",
                    true
                ),
                new NotificationEventData.NotificationAction(
                    "delegate",
                    "Delegate",
                    "DELEGATE",
                    "/api/v1/workflows/approvals/" + approvalId + "/delegate",
                    "POST",
                    "secondary",
                    false
                )
            );
            
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(approverUserId, userData, variables, actions);

            NotificationEventData eventData = new NotificationEventData(
                OperationType.APPROVAL_ESCALATED,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowInstanceId, eventData);

            logger.info()
                .message(LOG_ESCALATION_NOTIFICATION_SENT)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("approverUserId", approverUserId)
                .detail("escalationLevel", String.valueOf(escalationLevel))
                .log();
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_ESCALATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Send notification to the original approver that their approval request has been escalated.
     * 
     * @param tenantId the tenant ID
     * @param originalApproverUserId the original approver's user ID
     * @param originalApproverEmail the original approver's email
     * @param originalApproverName the original approver's name
     * @param workflowName the workflow name
     * @param escalatedToCount number of approvers it was escalated to
     */
    public void sendEscalationNotificationToOriginalApprover(
            String tenantId,
            String originalApproverUserId,
            String originalApproverEmail,
            String originalApproverName,
            String workflowName,
            int escalatedToCount) {
        
        try {
            NotificationEventData.TemplateVariables variables = new NotificationEventData.TemplateVariables();
            variables.setUsername(originalApproverName);
            variables.setEmail(originalApproverEmail);
            variables.setWorkflowName(workflowName);
            variables.setEscalatedToCount(escalatedToCount);

            NotificationEventData.UserData userData = new NotificationEventData.UserData(
                originalApproverEmail, originalApproverName);
            
            // No action buttons for this notification (informational only)
            NotificationEventData.UserNotificationData userNotification = 
                new NotificationEventData.UserNotificationData(originalApproverUserId, userData, variables, List.of());

            NotificationEventData eventData = new NotificationEventData(
                OperationType.APPROVAL_ESCALATED_FROM_YOU,
                tenantId,
                List.of(userNotification)
            );

            sendNotification(workflowName + "-escalated", eventData);

            logger.info()
                .message(LOG_ESCALATION_ORIGINAL_APPROVER_NOTIFIED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("originalApproverUserId", originalApproverUserId)
                .log();
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_ESCALATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Generic method to send notification event to Kafka.
     * Follows team pattern: serialize to JSON string before sending (matches auth-service).
     *
     * @param key the Kafka message key (workflow instance ID)
     * @param eventData the notification event data
     */
    /**
     * Internal method to send notification to Kafka
     * Phase 7: Protected with circuit breaker and retry
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationFallback")
    @Retry(name = "notificationService")
    @Bulkhead(name = "notificationService")
    private void sendNotification(String key, NotificationEventData eventData) {
        try {
            // Serialize to JSON string (team pattern from auth-service)
            String eventJson = objectMapper.writeValueAsString(eventData);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(notificationTopic, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info()
                        .message(LOG_NOTIFICATION_SENT_TO_KAFKA)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("topic", notificationTopic)
                        .detail("partition", String.valueOf(result.getRecordMetadata().partition()))
                        .detail("offset", String.valueOf(result.getRecordMetadata().offset()))
                        .log();
                } else {
                    logger.error()
                        .message(LOG_FAILED_SEND_NOTIFICATION_TO_KAFKA)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("topic", notificationTopic)
                        .detail("error", ex.getMessage())
                        .log();
                }
            });
        } catch (JsonProcessingException e) {
            logger.error()
                .message(LOG_FAILED_SERIALIZE_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Health check method to verify Kafka connectivity.
     *
     * @return true if Kafka is reachable
     */
    public boolean isKafkaReachable() {
        try {
            // Simple check by attempting to get cluster metadata
            kafkaTemplate.partitionsFor(notificationTopic);
            return true;
        } catch (Exception e) {
            logger.error()
                .message(LOG_KAFKA_HEALTH_CHECK_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }

    // ============ Phase 7: Fallback Methods for Resilience ============

    /**
     * Fallback for sendNotification when Kafka is unavailable
     */
    private void sendNotificationFallback(String key, NotificationEventData eventData, Exception e) {
        logger.error()
            .message(LOG_KAFKA_NOTIFICATION_UNAVAILABLE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("eventType", eventData.getOperationType().toString())
            .detail("key", key)
            .detail("error", e.getMessage())
            .log();
        logger.warn()
            .message(LOG_NOTIFICATION_NOT_SENT_KAFKA_UNAVAILABLE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("operationType", eventData.getOperationType().toString())
            .log();
    }
}
