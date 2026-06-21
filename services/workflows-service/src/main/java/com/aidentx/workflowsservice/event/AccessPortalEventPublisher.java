package com.aidentx.workflowsservice.event;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.entity.WorkflowApproval;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Publishes workflow lifecycle events ONLY for access-portal-originated workflows.
 * Detects accessRequestId in context to determine if events should be published.
 */
@Service
//@RequiredArgsConstructor
public class AccessPortalEventPublisher {

    private static final String ACCESS_REQUEST_EVENTS_TOPIC = "access-request-events";
    private static final String SOURCE_SERVICE = "workflows-service";

//    @Qualifier("accessRequestEventKafkaTemplate") // specified that I need bean fo this class to avoid ambiguity
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final IUnifiedLogger logger;

public AccessPortalEventPublisher(
        @Qualifier("accessRequestEventKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
        UnifiedLoggerFactory loggerFactory) {
    this.kafkaTemplate = kafkaTemplate;
    this.logger = loggerFactory.getLogger(getClass(), "ACCESS_PORTAL_EVENTS");
}

    /**
     * Check if this workflow execution is from Access Portal
     */
    private boolean isAccessPortalWorkflow(Map<String, Object> context) {
        return context != null && context.containsKey("accessRequestId");
    }

    /** Publish approval created event*/
    public void publishApprovalCreated(
        UUID executionId,
        UUID approvalId,
        UUID approverId,
        String approverType,
        String approverName,
        Map<String, Object> context,
        Map<String, Object> evidenceBundle) {

    if (!isAccessPortalWorkflow(context)) return;

    try {
        String accessRequestId = context.get("accessRequestId").toString();
        String roleId = context.getOrDefault("roleId", "").toString();
        String targetUserId = "";

        // approverDetails.targetUserId
        Object approverDetailsObj = context.getOrDefault("approverDetails", null);

        if (approverDetailsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approverDetails = (Map<String, Object>) approverDetailsObj;
            Object targetFromApproverDetails =
                    approverDetails.getOrDefault("targetUserId", "");
            if (!targetFromApproverDetails.toString().isBlank()) {
                targetUserId = targetFromApproverDetails.toString();
            }
        }

        // context.currentUserId (fallback)
        if (targetUserId.isBlank()) {
            Object currentUserIdObj = context.getOrDefault("currentUserId", "");
            if (!currentUserIdObj.toString().isBlank()) {
                targetUserId = currentUserIdObj.toString();
            }
        }

        // evidenceBundle.targetUserId (final fallback)
        if (targetUserId.isBlank() && evidenceBundle != null) {
            Object targetFromEvidence =
                    evidenceBundle.getOrDefault("targetUserId", "");
            if (!targetFromEvidence.toString().isBlank()) {
                targetUserId = targetFromEvidence.toString();
            }
        }

        String targetUserName = "";

        // 1. Try approverDetails.userName or approverDetails.targetUserName
        if (approverDetailsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approverDetails = (Map<String, Object>) approverDetailsObj;

            Object targetNameFromApproverDetails = approverDetails.getOrDefault("targetUserName", "");
            if (!targetNameFromApproverDetails.toString().isBlank()) {
                targetUserName = targetNameFromApproverDetails.toString();
            }

            if (targetUserName.isBlank()) {
                Object userNameFromApproverDetails = approverDetails.getOrDefault("userName", "");
                if (!userNameFromApproverDetails.toString().isBlank()) {
                    targetUserName = userNameFromApproverDetails.toString();
                }
            }
        }

        // 2. Try context.currentUserName
        if (targetUserName.isBlank()) {
            Object currentUserNameObj = context.getOrDefault("currentUserName", "");
            if (!currentUserNameObj.toString().isBlank()) {
                targetUserName = currentUserNameObj.toString();
            }
        }

        // 3. Try context.userName
        if (targetUserName.isBlank()) {
            Object userNameObj = context.getOrDefault("userName", "");
            if (!userNameObj.toString().isBlank()) {
                targetUserName = userNameObj.toString();
            }
        }

        // 4. Try evidenceBundle.targetUserName
        if (targetUserName.isBlank() && evidenceBundle != null) {
            Object targetNameFromEvidence = evidenceBundle.getOrDefault("targetUserName", "");
            if (!targetNameFromEvidence.toString().isBlank()) {
                targetUserName = targetNameFromEvidence.toString();
            }
        }

        // 5. Try evidenceBundle.currentUserName
        if (targetUserName.isBlank() && evidenceBundle != null) {
            Object currentNameFromEvidence = evidenceBundle.getOrDefault("currentUserName", "");
            if (!currentNameFromEvidence.toString().isBlank()) {
                targetUserName = currentNameFromEvidence.toString();
            }
        }

        // 6. Derive from targetUserId
        if (targetUserName.isBlank() && !targetUserId.isBlank()) {
            String shortUserId = targetUserId.length() > 8 ?
                targetUserId.substring(targetUserId.length() - 8) : targetUserId;
            targetUserName = "User " + shortUserId;
        }
        // --- END TARGET USERNAME RESOLUTION ---

        Map<String, Object> event = buildEvent("approval.created", Map.of(
            "accessRequestId", accessRequestId,
            "executionId", executionId.toString(),
            "approvalId", approvalId.toString(),
            "approverId", approverId.toString(),
            "approverName", approverName,
            "approverType", approverType,
            "roleId", roleId,
            "targetUserId", targetUserId,
            "targetUserName", targetUserName,
            "requestedAt", OffsetDateTime.now().toString()
        ));

        kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

        logger.debug()
            .message("Published approval.created event")
            .detail("accessRequestId", accessRequestId)
            .detail("approverName", approverName)
            .detail("approverType", approverType)
            .detail("approvalId", approvalId.toString())
            .detail("targetUserId", targetUserId)
            .detail("targetUserName", targetUserName)
            .log();

    } catch (Exception e) {
        logger.error()
            .message("Failed to publish approval.created event")
            .detail("error", e.getMessage())
            .log();
    }
    }
    /**
     * Publish approval responded event (approved/rejected)
     */
    public void publishApprovalResponded(
            UUID approvalId,
            UUID approverId,
            String approverName,
            String decision,
            String comments,
            Map<String, Object> context,
            Map<String, Object> evidenceBundle,
            WorkflowApproval.ApproverType approverType) {

        if (!isAccessPortalWorkflow(context)) return;

        try {
            String accessRequestId = context.get("accessRequestId").toString();
            String roleId = context.getOrDefault("roleId", "").toString();

            String targetUserId = "";

        // approverDetails.targetUserId
        Object approverDetailsObj = context.getOrDefault("approverDetails", null);
        if (approverDetailsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approverDetails = (Map<String, Object>) approverDetailsObj;

            Object targetFromApproverDetails =
                    approverDetails.getOrDefault("targetUserId", "");

            if (!targetFromApproverDetails.toString().isBlank()) {
                targetUserId = targetFromApproverDetails.toString();
            }
        }

        // context.currentUserId (fallback)
        if (targetUserId.isBlank()) {
            Object currentUserIdObj = context.getOrDefault("currentUserId", "");
            if (!currentUserIdObj.toString().isBlank()) {
                targetUserId = currentUserIdObj.toString();
            }
        }

        //  evidenceBundle.targetUserId (final fallback)
        if (targetUserId.isBlank() && evidenceBundle != null) {
            Object targetFromEvidence =
                    evidenceBundle.getOrDefault("targetUserId", "");

            if (!targetFromEvidence.toString().isBlank()) {
                targetUserId = targetFromEvidence.toString();
            }
        }


        // --- TARGET USERNAME RESOLUTION (same logic as above for username) ---
        String targetUserName = "";

        // 1. Try approverDetails.userName or approverDetails.targetUserName
        if (approverDetailsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approverDetails = (Map<String, Object>) approverDetailsObj;

            Object targetNameFromApproverDetails = approverDetails.getOrDefault("targetUserName", "");
            if (!targetNameFromApproverDetails.toString().isBlank()) {
                targetUserName = targetNameFromApproverDetails.toString();
            }

            if (targetUserName.isBlank()) {
                Object userNameFromApproverDetails = approverDetails.getOrDefault("userName", "");
                if (!userNameFromApproverDetails.toString().isBlank()) {
                    targetUserName = userNameFromApproverDetails.toString();
                }
            }
        }

        // 2. Try context.currentUserName
        if (targetUserName.isBlank()) {
            Object currentUserNameObj = context.getOrDefault("currentUserName", "");
            if (!currentUserNameObj.toString().isBlank()) {
                targetUserName = currentUserNameObj.toString();
            }
        }

        // 3. Try context.userName
        if (targetUserName.isBlank()) {
            Object userNameObj = context.getOrDefault("userName", "");
            if (!userNameObj.toString().isBlank()) {
                targetUserName = userNameObj.toString();
            }
        }

        // 4. Try evidenceBundle.targetUserName
        if (targetUserName.isBlank() && evidenceBundle != null) {
            Object targetNameFromEvidence = evidenceBundle.getOrDefault("targetUserName", "");
            if (!targetNameFromEvidence.toString().isBlank()) {
                targetUserName = targetNameFromEvidence.toString();
            }
        }

        // 5. Try evidenceBundle.currentUserName
        if (targetUserName.isBlank() && evidenceBundle != null) {
            Object currentNameFromEvidence = evidenceBundle.getOrDefault("currentUserName", "");
            if (!currentNameFromEvidence.toString().isBlank()) {
                targetUserName = currentNameFromEvidence.toString();
            }
        }

            Map<String, Object> event = buildEvent("approval.responded",

            Map.ofEntries(
                Map.entry("accessRequestId", accessRequestId),
                Map.entry("approvalId", approvalId.toString()),
                Map.entry("approverId", approverId.toString()),
                Map.entry("approverType", approverType.name()),
                Map.entry("approverName", approverName),
                Map.entry("decision", decision),
                Map.entry("comments", comments != null ? comments : ""),
                Map.entry("roleId", roleId),
                Map.entry("targetUserId", targetUserId),
                Map.entry("targetUserName", targetUserName),
                Map.entry("respondedAt", OffsetDateTime.now().toString())
            )
            );


            kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

            logger.info()
                .message("Published approval.responded event")
                .detail("accessRequestId", accessRequestId)
                .detail("approvalId", approvalId.toString())
                .detail("approverId", approverId.toString())
                .detail("approverType", approverType)
                .detail("decision", decision)
                .detail("comments", comments)
                .detail("roleId", roleId)
                .detail("targetUserId", targetUserId)
                .detail("targetUserName", targetUserName)
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to publish approval.responded event")
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Publish workflow completed event (all approvals done, before provisioning)
     */
    public void publishWorkflowCompleted(
            UUID executionId,
            String status,
            Map<String, Object> context) {

        if (!isAccessPortalWorkflow(context)) return;

        try {
            String accessRequestId = context.get("accessRequestId").toString();

            Map<String, Object> event = buildEvent("workflow.completed", Map.of(
                "accessRequestId", accessRequestId,
                "executionId", executionId.toString(),
                "status", status,
                "completedAt", OffsetDateTime.now().toString()
            ));

            kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

            logger.info()
                .message("Published workflow.completed event")
                .detail("accessRequestId", accessRequestId)
                .detail("status", status)
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to publish workflow.completed event")
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Publish role provisioning started event
     */
    public void publishProvisioningStarted(
            UUID executionId,
            Map<String, Object> context) {

        if (!isAccessPortalWorkflow(context)) return;

        try {
            String accessRequestId = context.get("accessRequestId").toString();
            String roleId = context.getOrDefault("roleId", "").toString();

            // Extract approved user IDs from context
            @SuppressWarnings("unchecked")
            List<String> approvedUserIds = (List<String>) context.getOrDefault("approvedUserIds", List.of());

            Map<String, Object> event = buildEvent("provisioning.started", Map.of(
                "accessRequestId", accessRequestId,
                "executionId", executionId.toString(),
                "roleId", roleId,
                "userIds", approvedUserIds,
                "startedAt", OffsetDateTime.now().toString()
            ));

            kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

            logger.info()
                .message("Published provisioning.started event")
                .detail("accessRequestId", accessRequestId)
                .detail("userCount", approvedUserIds.size())
                .detail("keys", context.keySet())
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to publish provisioning.started event")
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Publish role assignment completed event (success)
     */
    public void publishRoleAssigned(
            UUID executionId,
            String roleId,
            List<String> userIds,
            Map<String, Object> context) {

        if (!isAccessPortalWorkflow(context)) return;

        try {
            String accessRequestId = context.get("accessRequestId").toString();

            Map<String, Object> event = buildEvent("role.assigned", Map.of(
                "accessRequestId", accessRequestId,
                "executionId", executionId.toString(),
                "roleId", roleId,
                "userIds", userIds,
                "assignedAt", OffsetDateTime.now().toString(),
                "status", "SUCCESS"
            ));

            kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

            logger.info()
                .message("Published role.assigned event")
                .detail("accessRequestId", accessRequestId)
                .detail("roleId", roleId)
                .detail("userCount", userIds.size())
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to publish role.assigned event")
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Publish role assignment failed event
     */
    public void publishRoleAssignmentFailed(
            UUID executionId,
            String roleId,
            List<String> userIds,
            String errorMessage,
            Map<String, Object> context) {

        if (!isAccessPortalWorkflow(context)) return;

        try {
            String accessRequestId = context.get("accessRequestId").toString();

            Map<String, Object> event = buildEvent("role.assignment.failed", Map.of(
                "accessRequestId", accessRequestId,
                "executionId", executionId.toString(),
                "roleId", roleId,
                "userIds", userIds,
                "errorMessage", errorMessage,
                "failedAt", OffsetDateTime.now().toString(),
                "status", "FAILED"
            ));

            kafkaTemplate.send(ACCESS_REQUEST_EVENTS_TOPIC, accessRequestId, event);

            logger.warn()
                .message("Published role.assignment.failed event")
                .detail("accessRequestId", accessRequestId)
                .detail("error", errorMessage)
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to publish role.assignment.failed event")
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Build standard event structure
     */
    private Map<String, Object> buildEvent(String eventType, Map<String, Object> payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("sourceService", SOURCE_SERVICE);
        event.put("timestamp", OffsetDateTime.now().toString());
        event.put("payload", payload);
        return event;
    }
}