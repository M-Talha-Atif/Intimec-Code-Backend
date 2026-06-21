package com.aidentx.access_portal_service.event.consumer;


import com.aidentx.access_portal_service.entity.access_request.*;
import com.aidentx.access_portal_service.factory.AccessRequestEntityFactory;
import com.aidentx.access_portal_service.repository.access_request.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Consumes access request lifecycle events from workflow service.
 * Updates approval steps, provisioning status, and audit logs.
 */
@Service
public class AccessRequestEventConsumer {

    private final AccessRequestRepository accessRequestRepository;
    private final RequestApprovalStepRepository approvalStepRepository;
    private final RequestProvisioningRepository provisioningRepository;
    private final RequestAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final AccessRequestEntityFactory entityFactory;
    private final IUnifiedLogger logger;

    public AccessRequestEventConsumer(
            AccessRequestRepository accessRequestRepository,
            RequestApprovalStepRepository approvalStepRepository,
            RequestProvisioningRepository provisioningRepository,
            RequestAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            AccessRequestEntityFactory entityFactory) {
        this.accessRequestRepository = accessRequestRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.provisioningRepository = provisioningRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.entityFactory = entityFactory;
        this.logger = loggerFactory.getLogger(getClass(), "ACCESS_REQUEST_EVENTS");
    }

    /**
     * Main event consumer - routes events to appropriate handlers
     */
    @KafkaListener(topics = "access-request-events", groupId = "access-portal-service", containerFactory = "accessRequestEventListenerContainerFactory")
    @Transactional
    public void handleAccessRequestEvent(String eventJson, Acknowledgment acknowledgment) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);

            String eventType = (String) event.get("eventType");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");

            if (payload == null) {
                logger.warn()
                        .message("Received event with null payload")
                        .detail("eventType", eventType)
                        .log();
                acknowledgment.acknowledge(); // ACK to prevent reprocessing
                return;
            }

            String accessRequestId = (String) payload.get("accessRequestId");

            if (accessRequestId == null) {
                logger.debug()
                        .message("Event does not contain accessRequestId, skipping")
                        .detail("eventType", eventType)
                        .log();
                acknowledgment.acknowledge(); //  ACK to prevent reprocessing
                return;
            }

            logger.info()
                    .message("Processing access request event")
                    .detail("eventType", eventType)
                    .detail("accessRequestId", accessRequestId)
                    .log();

            switch (eventType) {
                case "approval.created" -> handleApprovalCreated(payload);
                case "approval.responded" -> handleApprovalResponded(payload);
                case "workflow.completed" -> handleWorkflowCompleted(payload);
                case "provisioning.started" -> handleProvisioningStarted(payload);
                case "role.assigned" -> handleRoleAssigned(payload);
                case "role.assignment.failed" -> handleRoleAssignmentFailed(payload);
                default -> logger.debug()
                        .message("Unknown event type, ignoring")
                        .detail("eventType", eventType)
                        .log();
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to process access request event")
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Handle approval.created event
     */
    private void handleApprovalCreated(Map<String, Object> payload) {

        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        UUID approvalId = UUID.fromString((String) payload.get("approvalId"));
        String approverType = (String) payload.get("approverType");
        String roleId = (String) payload.get("roleId");
        String targetUserId = (String) payload.get("targetUserId");
        String targetUserName = (String) payload.get("targetUserName");
        String approverId = (String) payload.get("approverId");
        String approverName = (String) payload.get("approverName");


        logger.info()
                .message("handleApprovalCreated - payload received")
                .detail("accessRequestId", payload.get("accessRequestId"))
                .detail("approvalId", payload.get("approvalId"))
                .detail("approverType", payload.get("approverType"))
                .detail("approverName", payload.get("approverName"))
                .detail("roleId", payload.get("roleId"))
                .detail("targetUserId", payload.get("targetUserId"))
                .detail("payloadKeys", payload.keySet())
                .log();

        // 1. Find the access request
        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        // 2. Find the SPECIFIC ITEM for this role/user
        AccessRequestItem targetItem = request.getItems().stream()
                .filter(item ->
                        item.getItemId().toString().equals(roleId) &&
                                (item.getRequestTarget() == RequestTarget.SELF ||
                                        beneficiaryContains(item.getBeneficiaryUserIds(), targetUserId))
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No access request item found for role: " + roleId + " and user: " + targetUserId));


        // 3. Count existing steps for THIS ITEM to determine stepOrder
        List<RequestApprovalStep> existingSteps = approvalStepRepository
                .findByRequestItemOrderByStepOrder(targetItem);

        int stepOrder = existingSteps.size() + 1;

        // 4. Find existing approval step or create new one
        Optional<RequestApprovalStep> existingStep = approvalStepRepository
                .findByRequestItemAndApproverRoleAndTargetUserIdAndAssignedApproverId(targetItem, approverType, targetUserId, approverId);

        RequestApprovalStep step;
        if (existingStep.isPresent()) {
            step = existingStep.get();
            step.setStatus(ApprovalStepStatus.PENDING_ASSIGNMENT);
            step.setUpdatedAt(OffsetDateTime.now());
        } else {
            // Use updated factory method - NOW takes AccessRequestItem
            step = entityFactory.createApprovalStep(
                    targetItem,
                    request,
                    stepOrder,
                    approverType,
                    approverName, // name of approver like manager name, role owner name
                    approverId,      //  assignedApproverId (the manager)
                    targetUserId,    //  targetUserId (the beneficiary)
                    targetUserName

            );
        }

        approvalStepRepository.save(step);


        if (request.getStatus() == RequestStatus.SUBMITTED) {
            request.setStatus(RequestStatus.PENDING_APPROVAL);
            accessRequestRepository.save(request);

            createAuditLog(
                    request,
                    AuditAction.STATUS_CHANGED,
                    "System",
                    "Request moved to pending approval - approval workflow started"
            );
        }

        // Audit message
        String auditMessage = String.format(
                "Approval assigned: %s (%s) assigned to approve %s '%s' for user '%s'.",
                approverName,              // Approver name
                approverType,              // Approver role/type (e.g., MANAGER, ROLE_OWNER)
                targetItem.getItemType(),  // Item type
                targetItem.getItemName(),  // Item name
                targetUserName             // Target user name
        );


        // Create audit log
        createAuditLog(
                request,
                AuditAction.APPROVAL_ASSIGNED,
                "System",
                auditMessage
        );

        logger.info()
                .message("Created approval step for item")
                .detail("accessRequestId", accessRequestId.toString())
                .detail("itemId", targetItem.getId().toString())
                .detail("approvalId", approvalId.toString())
                .detail("approverType", approverType)
                .log();
    }

    /**
     * Handle approval.responded event (approved/rejected)
     */
    private void handleApprovalResponded(Map<String, Object> payload) {
        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        String decision = (String) payload.get("decision");
        String approverType = (String) payload.get("approverType");
        String roleId = (String) payload.get("roleId");
        String targetUserId = (String) payload.get("targetUserId");
        String targetUserName = (String) payload.get("targetUserName");
        String comments = (String) payload.get("comments");
        String approverName = (String) payload.get("approverName");
        String approverId = (String) payload.get("approverId");

        logger.info()
                .message("handleApprovalResponded - payload received")
                .detail("accessRequestId", payload.get("accessRequestId"))
                .detail("decision", payload.get("decision"))
                .detail("approverType", payload.get("approverType"))
                .detail("roleId", payload.get("roleId"))
                .detail("targetUserId", payload.get("targetUserId"))
                .detail("targetUserName", payload.get("targetUserName"))
                .log();

        // 1. Find the access request
        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        // 2. Find the SPECIFIC ITEM for this role/user
        AccessRequestItem targetItem = request.getItems().stream()
                .filter(item ->
                        item.getItemId().toString().equals(roleId) &&
                                (item.getRequestTarget() == RequestTarget.SELF ||
                                        beneficiaryContains(item.getBeneficiaryUserIds(), targetUserId))
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No access request item found for role: " + roleId + " and user: " + targetUserId));

        // 3. Find the approval step for THIS ITEM and approver type
        Optional<RequestApprovalStep> stepOpt = approvalStepRepository
                .findByRequestItemAndApproverRoleAndTargetUserIdAndAssignedApproverId(targetItem, approverType, targetUserId, approverId);


        // 4. If still not found, log warning and skip
        if (stepOpt.isEmpty()) {
            logger.warn()
                    .message("Approval step not found, skipping update")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("itemId", targetItem.getId().toString())
                    .detail("approverType", approverType)
                    .detail("reason", "Event may have arrived before approval.created")
                    .log();
            return;
        }

        RequestApprovalStep approvalStep = stepOpt.get();

        // Log which step we found
        logger.debug()
                .message("Found approval step to update")
                .detail("stepId", approvalStep.getId().toString())
                .detail("stepOrder", String.valueOf(approvalStep.getStepOrder()))
                .detail("approverRole", approvalStep.getApproverRole())
                .detail("currentStatus", approvalStep.getStatus().name())
                .log();

        // 6. Update status
        ApprovalStepStatus newStatus = "APPROVED".equals(decision) ?
                ApprovalStepStatus.APPROVED : ApprovalStepStatus.REJECTED;

        approvalStep.setStatus(newStatus);
        approvalStep.setDecision(ApprovalDecision.valueOf(decision));
        approvalStep.setComments(comments);
        approvalStep.setRespondedAt(OffsetDateTime.now());

        approvalStepRepository.save(approvalStep);

        //  Check if ALL approvals for THIS ITEM are finalized
        List<RequestApprovalStep> itemApprovalSteps = approvalStepRepository
                .findByRequestItemOrderByStepOrder(targetItem);

        boolean allItemApprovalsFinalized = itemApprovalSteps.stream()
                .allMatch(step -> step.getDecision() == ApprovalDecision.APPROVED ||
                        step.getDecision() == ApprovalDecision.REJECTED);

        if (allItemApprovalsFinalized) {
            // Handle rejections for this item (create CANCELLED provisioning records)
            handleRoleRejections(request, targetItem);
        }

        // 7. Add audit log
        createAuditLog(
                request,
                "APPROVED".equals(decision)
                        ? AuditAction.APPROVAL_APPROVED
                        : AuditAction.APPROVAL_REJECTED,
                approverName,
                String.format(
                        "Approval %s by %s (%s) for %s '%s' requested for user '%s'%s.",
                        decision.toLowerCase(),              // approved / rejected
                        approverName,                        // Approver name
                        approverType,                        // Approver role/type
                        targetItem.getItemType(),            // Item type
                        targetItem.getItemName(),            // Item name
                        targetUserName,                      // Target user name
                        comments != null && !comments.isBlank()
                                ? " with comments: " + comments
                                : ""
                )
        );


        logger.info()
                .message("Updated approval step")
                .detail("accessRequestId", accessRequestId.toString())
                .detail("itemId", targetItem.getId().toString())
                .detail("decision", decision)
                .detail("approverType", approverType)
                .detail("approverName", approverName)
                .detail("roleId", roleId)
                .detail("targetUserId", targetUserId)
                .detail("stepId", approvalStep.getId().toString())
                .detail("stepOrder", String.valueOf(approvalStep.getStepOrder()))
                .log();
    }

    /**
     * Handle workflow.completed event
     */
    private void handleWorkflowCompleted(Map<String, Object> payload) {
        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        String status = (String) payload.get("status");

        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        // Check if all approvals are complete
        List<RequestApprovalStep> allSteps = approvalStepRepository
                .findByAccessRequest(request);

        long approvedCount = allSteps.stream()
                .filter(s -> s.getStatus() == ApprovalStepStatus.APPROVED)
                .count();
        long rejectedCount = allSteps.stream()
                .filter(s -> s.getStatus() == ApprovalStepStatus.REJECTED)
                .count();

        if (rejectedCount > 0) {
            request.setStatus(RequestStatus.PARTIALLY_APPROVED);
            createAuditLog(request, AuditAction.REQUEST_PARTIALLY_APPROVED,
                    "System", "Some approvals were rejected");
        } else if (approvedCount == allSteps.size()) {
            request.setStatus(RequestStatus.APPROVED);
            createAuditLog(request, AuditAction.REQUEST_FULLY_APPROVED,
                    "System", "All approvals completed");
        }

        accessRequestRepository.save(request);

        logger.info()
                .message("Workflow completed")
                .detail("accessRequestId", accessRequestId.toString())
                .detail("status", status)
                .log();
    }

    /**
     * Handle provisioning.started event
     */
    private void handleProvisioningStarted(Map<String, Object> payload) {
        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        String roleId = (String) payload.get("roleId");

        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) payload.get("userIds");

        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        // Find ALL items with this roleId (there may be multiple)
        List<AccessRequestItem> matchingItems = request.getItems().stream()
                .filter(i -> roleId.equals(i.getItemId().toString()))
                .collect(Collectors.toList());

        if (matchingItems.isEmpty()) {
            logger.warn()
                    .message("No items found for roleId")
                    .detail("roleId", roleId)
                    .detail("accessRequestId", accessRequestId.toString())
                    .log();
            return;
        }

        // For EACH item, filter userIds to only those belonging to this item
        for (AccessRequestItem item : matchingItems) {
            // Parse beneficiary IDs for this specific item
            List<String> itemBeneficiaryIds = parseBeneficiaryIds(item.getBeneficiaryUserIds());

            // Filter to only users that belong to THIS item
            List<String> relevantUserIds = userIds.stream()
                    .filter(itemBeneficiaryIds::contains)
                    .collect(Collectors.toList());

            if (relevantUserIds.isEmpty()) {
                logger.debug()
                        .message("No relevant users for this item in provisioning event")
                        .detail("itemId", item.getId().toString())
                        .detail("roleId", roleId)
                        .log();
                continue;
            }

            // Create provisioning records ONLY for users belonging to this item
            for (String userId : relevantUserIds) {
                // Idempotency: skip if provisioning already exists for this user
                Optional<RequestProvisioning> existing =
                        provisioningRepository.findByRequestItemAndTargetUserId(item, userId);

                if (existing.isPresent()) {
                    logger.debug()
                            .message("Provisioning record already exists, skipping")
                            .detail("itemId", item.getId().toString())
                            .detail("userId", userId)
                            .log();
                    continue;
                }

                RequestProvisioning provisioning = entityFactory.createProvisioning(
                        request, item, roleId, userId
                );
                provisioningRepository.save(provisioning);
            }

            // BUILD USER-FRIENDLY AUDIT MESSAGE (only for users in THIS item)
            List<String> userNames = extractUserNamesForIds(
                    item.getBeneficiaryNames(),
                    item.getBeneficiaryUserIds(),
                    relevantUserIds
            );

            String userNamesStr = userNames.isEmpty()
                    ? String.format("%d user(s)", relevantUserIds.size())
                    : userNames.stream().collect(Collectors.joining(", "));

            String auditMessage = String.format(
                    "Provisioning started for %s '%s' for %s",
                    item.getItemType().toLowerCase().replace('_', ' '),
                    item.getItemName(),
                    userNamesStr
            );

            createAuditLog(request, AuditAction.PROVISIONING_STARTED, "System", auditMessage);

            logger.info()
                    .message("Provisioning started for item")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("itemId", item.getId().toString())
                    .detail("roleId", roleId)
                    .detail("itemName", item.getItemName())
                    .detail("userNames", userNamesStr)
                    .detail("userCount", relevantUserIds.size())
                    .log();
        }
    }

    /**
     * Handle role.assigned event (success)
     */
    private void handleRoleAssigned(Map<String, Object> payload) {
        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        String roleId = (String) payload.get("roleId");

        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) payload.get("userIds");

        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        // Find ALL items with this roleId
        List<AccessRequestItem> matchingItems = request.getItems().stream()
                .filter(i -> roleId.equals(i.getItemId().toString()))
                .collect(Collectors.toList());

        if (matchingItems.isEmpty()) {
            logger.warn()
                    .message("No items found for roleId")
                    .detail("roleId", roleId)
                    .detail("accessRequestId", accessRequestId.toString())
                    .log();
            return;
        }

        // For EACH item, filter userIds to only those belonging to this item
        for (AccessRequestItem item : matchingItems) {
            // Parse beneficiary IDs for this specific item
            List<String> itemBeneficiaryIds = parseBeneficiaryIds(item.getBeneficiaryUserIds());

            // Filter to only users that belong to THIS item
            List<String> relevantUserIds = userIds.stream()
                    .filter(itemBeneficiaryIds::contains)
                    .collect(Collectors.toList());

            if (relevantUserIds.isEmpty()) {
                logger.debug()
                        .message("No relevant users for this item in role.assigned event")
                        .detail("itemId", item.getId().toString())
                        .detail("roleId", roleId)
                        .log();
                continue;
            }

            // Update provisioning records ONLY for users belonging to this item
            for (String userId : relevantUserIds) {
                Optional<RequestProvisioning> provisioningOpt = provisioningRepository
                        .findByRequestItemAndTargetUserId(item, userId);

                RequestProvisioning provisioning;
                if (provisioningOpt.isPresent()) {
                    provisioning = provisioningOpt.get();
                } else {
                    logger.warn()
                            .message("Provisioning record not found, creating new one")
                            .detail("accessRequestId", accessRequestId.toString())
                            .detail("itemId", item.getId().toString())
                            .detail("userId", userId)
                            .log();
                    provisioning = entityFactory.createProvisioning(
                            request, item, roleId, userId
                    );
                }

                provisioning.setStatus(ProvisioningStatus.COMPLETED);
                provisioning.setCompletedAt(OffsetDateTime.now());
                provisioningRepository.save(provisioning);
            }

            // BUILD USER-FRIENDLY AUDIT MESSAGE (only for users in THIS item)
            List<String> userNames = extractUserNamesForIds(
                    item.getBeneficiaryNames(),
                    item.getBeneficiaryUserIds(),
                    relevantUserIds
            );

            String userNamesStr = userNames.isEmpty()
                    ? String.format("%d user(s)", relevantUserIds.size())
                    : userNames.stream().collect(Collectors.joining(", "));

            String provisioningAuditMessage = String.format(
                    "%s '%s' successfully assigned to %s",
                    item.getItemType().toLowerCase().replace('_', ' '),
                    item.getItemName(),
                    userNamesStr
            );

            createAuditLog(request, AuditAction.PROVISIONING_COMPLETED, "System", provisioningAuditMessage);

            logger.info()
                    .message("Role assigned successfully for item")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("itemId", item.getId().toString())
                    .detail("roleId", roleId)
                    .detail("itemName", item.getItemName())
                    .detail("userCount", relevantUserIds.size())
                    .log();
        }

        logger.info()
                .message("Triggering overall request completion check")
                .detail("accessRequestId", accessRequestId.toString())
                .detail("trigger", "role.assigned event processed")
                .log();

        checkOverallRequestCompletion(request);
    }

    /**
     * Handle role.assignment.failed event
     */
    private void handleRoleAssignmentFailed(Map<String, Object> payload) {
        UUID accessRequestId = UUID.fromString((String) payload.get("accessRequestId"));
        String roleId = (String) payload.get("roleId");
        String errorMessage = (String) payload.get("errorMessage");

        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) payload.get("userIds");

        AccessRequest request = accessRequestRepository.findById(accessRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found: " + accessRequestId));

        //  Find ALL items with this roleId
        List<AccessRequestItem> matchingItems = request.getItems().stream()
                .filter(i -> roleId.equals(i.getItemId().toString()))
                .collect(Collectors.toList());

        if (matchingItems.isEmpty()) {
            logger.warn()
                    .message("No items found for roleId")
                    .detail("roleId", roleId)
                    .log();
            return;
        }

        // For EACH item, filter userIds to only those belonging to this item
        for (AccessRequestItem item : matchingItems) {
            List<String> itemBeneficiaryIds = parseBeneficiaryIds(item.getBeneficiaryUserIds());

            List<String> relevantUserIds = userIds.stream()
                    .filter(itemBeneficiaryIds::contains)
                    .collect(Collectors.toList());

            if (relevantUserIds.isEmpty()) {
                continue;
            }

            // Update provisioning records
            for (String userId : relevantUserIds) {
                Optional<RequestProvisioning> provisioningOpt = provisioningRepository
                        .findByRequestItemAndTargetUserId(item, userId);

                RequestProvisioning provisioning;
                if (provisioningOpt.isPresent()) {
                    provisioning = provisioningOpt.get();
                } else {
                    logger.warn()
                            .message("Provisioning record not found, creating new one")
                            .detail("itemId", item.getId().toString())
                            .detail("userId", userId)
                            .log();
                    provisioning = entityFactory.createProvisioning(
                            request, item, roleId, userId
                    );
                }

                provisioning.setStatus(ProvisioningStatus.FAILED);
                provisioning.setErrorMessage(errorMessage);
                provisioning.setCompletedAt(OffsetDateTime.now());
                provisioningRepository.save(provisioning);
            }

            // Audit message
            List<String> userNames = extractUserNamesForIds(
                    item.getBeneficiaryNames(),
                    item.getBeneficiaryUserIds(),
                    relevantUserIds
            );

            String userNamesStr = userNames.isEmpty()
                    ? String.format("%d user(s)", relevantUserIds.size())
                    : userNames.stream().collect(Collectors.joining(", "));

            String auditMessage = String.format(
                    "Provisioning failed for %s '%s' for %s: %s",
                    item.getItemType().toLowerCase().replace('_', ' '),
                    item.getItemName(),
                    userNamesStr,
                    errorMessage
            );

            createAuditLog(request, AuditAction.PROVISIONING_FAILED, "System", auditMessage);

            logger.error()
                    .message("Role assignment failed for item")
                    .detail("itemId", item.getId().toString())
                    .detail("itemName", item.getItemName())
                    .detail("userCount", relevantUserIds.size())
                    .detail("error", errorMessage)
                    .log();
        }

        //  Check overall request completion (only ONCE after all items processed)
        checkOverallRequestCompletion(request);
    }

    /**
     * Helper to create audit log entries
     */
    private void createAuditLog(AccessRequest request, AuditAction action,
                                String performedBy, String details) {
        RequestAuditLog log = entityFactory.createAuditLog(
                request, action, performedBy, details
        );
        auditLogRepository.save(log);

    }

    /**
     * Creates provisioning records with CANCELLED status for rejected users
     * when all approvals for a role/item are finalized.
     * <p>
     * This ensures rejected users are reflected in provisioning with proper status.
     */
    private void handleRoleRejections(AccessRequest request, AccessRequestItem item) {
        // Find all approval steps for this item
        List<RequestApprovalStep> itemApprovalSteps = approvalStepRepository
                .findByRequestItemOrderByStepOrder(item);

        // Filter rejected approvals
        List<RequestApprovalStep> rejectedSteps = itemApprovalSteps.stream()
                .filter(step -> step.getDecision() == ApprovalDecision.REJECTED)
                .collect(Collectors.toList());

        if (rejectedSteps.isEmpty()) {
            return; // No rejections for this item
        }

        // Extract rejected user IDs and names
        List<String> rejectedUserIds = rejectedSteps.stream()
                .map(RequestApprovalStep::getTargetUserId)
                .distinct()
                .collect(Collectors.toList());

        List<String> rejectedUserNames = rejectedSteps.stream()
                .map(RequestApprovalStep::getTargetUserName)
                .distinct()
                .collect(Collectors.toList());

        // Create provisioning records for rejected users
        for (String userId : rejectedUserIds) {
            // Check if provisioning record already exists
            Optional<RequestProvisioning> existing = provisioningRepository
                    .findByRequestItemAndTargetUserId(item, userId);

            if (existing.isEmpty()) {
                // Create CANCELLED provisioning record
                RequestProvisioning provisioning = entityFactory.createProvisioning(
                        request, item, item.getItemId().toString(), userId
                );
                provisioning.setStatus(ProvisioningStatus.CANCELLED);
                provisioning.setCompletedAt(OffsetDateTime.now());
                provisioning.setErrorMessage("Request rejected during approval workflow");
                provisioningRepository.save(provisioning);

                logger.info()
                        .message("Created CANCELLED provisioning record for rejected user")
                        .detail("accessRequestId", request.getId().toString())
                        .detail("itemId", item.getId().toString())
                        .detail("userId", userId)
                        .log();
            }
        }

        // Create audit log for rejected users
        String userNamesStr = rejectedUserNames.isEmpty()
                ? String.format("%d user(s)", rejectedUserIds.size())
                : rejectedUserNames.stream().collect(Collectors.joining(", "));

        String auditMessage = String.format(
                "Provisioning cancelled for %s '%s' for %s due to approval rejection",
                item.getItemType().toLowerCase().replace('_', ' '),
                item.getItemName(),
                userNamesStr
        );

        createAuditLog(request, AuditAction.PROVISIONING_FAILED, "System", auditMessage);


        // If all rejected (no approved users), there won't be any provisioning events
        // So we need to check completion here
        List<RequestApprovalStep> allApprovalSteps = approvalStepRepository
                .findByAccessRequest(request);

        long totalApprovals = allApprovalSteps.size();
        long finalizedApprovals = allApprovalSteps.stream()
                .filter(step -> step.getDecision() == ApprovalDecision.APPROVED ||
                        step.getDecision() == ApprovalDecision.REJECTED)
                .count();

        logger.debug()
                .message("Checking if all approvals finalized after rejection handling")
                .detail("accessRequestId", request.getId().toString())
                .detail("totalApprovals", totalApprovals)
                .detail("finalizedApprovals", finalizedApprovals)
                .log();

        // Only check completion if ALL approvals are finalized
        if (totalApprovals > 0 && finalizedApprovals == totalApprovals) {
            logger.info()
                    .message("All approvals finalized - triggering completion check from rejection handler")
                    .detail("accessRequestId", request.getId().toString())
                    .log();

            checkOverallRequestCompletion(request);
        }
    }


    /**
     * Checks if all approval steps and provisioning are complete for the entire request.
     * Updates request status accordingly.
     * <p>
     * Simple logic:
     * - All approved → PROVISIONED
     * - All rejected → REJECTED
     * - Mix → PARTIALLY_PROVISIONED
     */
    private void checkOverallRequestCompletion(AccessRequest request) {
        // Get all approval steps
        List<RequestApprovalStep> allApprovalSteps = approvalStepRepository
                .findByAccessRequest(request);

        // Group by user to get their final decision
        Map<String, List<RequestApprovalStep>> stepsByUser = allApprovalSteps.stream()
                .collect(Collectors.groupingBy(RequestApprovalStep::getTargetUserId));

        // Count users by their FINAL approval decision (last step)
        long approvedUsers = stepsByUser.entrySet().stream()
                .filter(entry -> {
                    RequestApprovalStep lastStep = entry.getValue().stream()
                            .max(Comparator.comparing(RequestApprovalStep::getStepOrder))
                            .orElse(null);
                    return lastStep != null && lastStep.getDecision() == ApprovalDecision.APPROVED;
                })
                .count();

        long rejectedUsers = stepsByUser.entrySet().stream()
                .filter(entry -> {
                    RequestApprovalStep lastStep = entry.getValue().stream()
                            .max(Comparator.comparing(RequestApprovalStep::getStepOrder))
                            .orElse(null);
                    return lastStep != null && lastStep.getDecision() == ApprovalDecision.REJECTED;
                })
                .count();

        long totalUsers = stepsByUser.size();
        long pendingUsers = totalUsers - approvedUsers - rejectedUsers;

        logger.info()
                .message("Checking overall request completion")
                .detail("accessRequestId", request.getId().toString())
                .detail("totalUsers", totalUsers)
                .detail("approvedUsers", approvedUsers)
                .detail("rejectedUsers", rejectedUsers)
                .detail("pendingUsers", pendingUsers)
                .log();

        // Skip if any users still pending
        if (pendingUsers > 0) {
            logger.info()
                    .message("Request completion check - approvals still pending")
                    .detail("accessRequestId", request.getId().toString())
                    .detail("pendingUsers", pendingUsers)
                    .log();
            return;
        }

        // Get provisioning records
        List<RequestProvisioning> allProvisioning = provisioningRepository
                .findByAccessRequest(request);

        // Active provisioning (exclude CANCELLED)
        List<RequestProvisioning> activeProvisioning = allProvisioning.stream()
                .filter(p -> p.getStatus() != ProvisioningStatus.CANCELLED)
                .collect(Collectors.toList());

        // Check if provisioning is done
        boolean allProvisioningComplete = activeProvisioning.stream()
                .allMatch(p -> p.getStatus() == ProvisioningStatus.COMPLETED ||
                        p.getStatus() == ProvisioningStatus.FAILED);

        boolean allApprovedProvisioned = (activeProvisioning.size() == approvedUsers);

        logger.info()
                .message("Provisioning status check")
                .detail("activeProvisioningRecords", activeProvisioning.size())
                .detail("approvedUsers", approvedUsers)
                .detail("allProvisioningComplete", allProvisioningComplete)
                .detail("allApprovedProvisioned", allApprovedProvisioned)
                .log();

        // Wait for provisioning if not ready
        if (!allProvisioningComplete || !allApprovedProvisioned) {
            logger.debug()
                    .message("Request completion check - waiting for provisioning")
                    .detail("accessRequestId", request.getId().toString())
                    .log();
            return;
        }

        // SIMPLE STATUS LOGIC
        long provisionedCount = activeProvisioning.stream()
                .filter(p -> p.getStatus() == ProvisioningStatus.COMPLETED)
                .count();

        if (rejectedUsers == totalUsers) {
            // All rejected
            request.setStatus(RequestStatus.REJECTED);
            createAuditLog(request, AuditAction.REQUEST_REJECTED,
                    "System", String.format("All %d user(s) rejected during approval", rejectedUsers));
        } else if (provisionedCount > 0) {
            // At least one provisioned
            if (approvedUsers == totalUsers && provisionedCount == approvedUsers) {
                // All approved and all provisioned
                request.setStatus(RequestStatus.PROVISIONED);
                createAuditLog(request, AuditAction.PROVISIONING_COMPLETED,
                        "System", "All users successfully provisioned");
            } else {
                // Mixed results - collect detailed information
                StringBuilder detailedMessage = new StringBuilder();
                detailedMessage.append(String.format("%d user(s) provisioned successfully, %d rejected. Details: ",
                        provisionedCount, rejectedUsers));

                // Create a map of user ID to their provisioning status
                Map<String, ProvisioningStatus> provisioningStatusByUser = allProvisioning.stream()
                        .collect(Collectors.toMap(
                                RequestProvisioning::getTargetUserId,
                                RequestProvisioning::getStatus,
                                (existing, replacement) -> existing
                        ));

                // Create a map to store user names (will populate from items)
                Map<String, String> userNamesById = new HashMap<>();

                // Build user names map from request items
                for (AccessRequestItem item : request.getItems()) {
                    if (item.getBeneficiaryNames() != null && item.getBeneficiaryUserIds() != null) {
                        String[] userIds = item.getBeneficiaryUserIds().split(",");
                        String[] userNames = item.getBeneficiaryNames().split(",");
                        for (int i = 0; i < userIds.length; i++) {
                            userNamesById.put(userIds[i].trim(), userNames[i].trim());
                        }
                    }
                }

                // Process each user's final status
                for (Map.Entry<String, List<RequestApprovalStep>> entry : stepsByUser.entrySet()) {
                    String userId = entry.getKey();
                    List<RequestApprovalStep> steps = entry.getValue();

                    // Get the last step for final decision
                    RequestApprovalStep lastStep = steps.stream()
                            .max(Comparator.comparing(RequestApprovalStep::getStepOrder))
                            .orElse(null);

                    if (lastStep != null) {
                        String userName = userNamesById.getOrDefault(userId, lastStep.getTargetUserName());
                        String roleName = lastStep.getRequestItem().getItemName(); // Get role name from item
                        ApprovalDecision decision = lastStep.getDecision();

                        if (decision == ApprovalDecision.APPROVED) {
                            ProvisioningStatus provStatus = provisioningStatusByUser.get(userId);
                            if (provStatus == ProvisioningStatus.COMPLETED) {
                                detailedMessage.append(String.format("[%s - PROVISIONED (Role: %s)] ",
                                        userName, roleName));
                            } else if (provStatus == ProvisioningStatus.FAILED) {
                                detailedMessage.append(String.format("[%s - PROVISIONING_FAILED (Role: %s)] ",
                                        userName, roleName));
                            } else {
                                detailedMessage.append(String.format("[%s - APPROVED (Role: %s)] ",
                                        userName, roleName));
                            }
                        } else if (decision == ApprovalDecision.REJECTED) {
                            detailedMessage.append(String.format("[%s - REJECTED (Role: %s)] ",
                                    userName, roleName));
                        }
                    }
                }

                request.setStatus(RequestStatus.PARTIALLY_PROVISIONED);
                createAuditLog(request, AuditAction.PARTIALLY_PROVISIONED,
                        "System", detailedMessage.toString().trim());
            }
        } else {
            // No one provisioned (all provisioning failed)
            request.setStatus(RequestStatus.REJECTED);
            createAuditLog(request, AuditAction.REQUEST_REJECTED,
                    "System", "All provisioning attempts failed");
        }

        accessRequestRepository.save(request);

        logger.info()
                .message("Request completion check - status updated")
                .detail("accessRequestId", request.getId().toString())
                .detail("finalStatus", request.getStatus().name())
                .detail("totalUsers", totalUsers)
                .detail("approvedUsers", approvedUsers)
                .detail("rejectedUsers", rejectedUsers)
                .detail("provisionedCount", provisionedCount)
                .log();
    }

    /**
     * Extracts user names for the given user IDs from the comma-separated beneficiaryNames string.
     * Matches names by position in the parallel arrays.
     *
     * @param beneficiaryNames   comma-separated names (e.g., "Alice,Bob,Charlie")
     * @param beneficiaryUserIds comma-separated UUIDs (parallel to names)
     * @param targetUserIds      list of user IDs to extract names for
     * @return list of user names (empty list if names unavailable)
     */
    private List<String> extractUserNamesForIds(
            String beneficiaryNames,
            String beneficiaryUserIds,
            List<String> targetUserIds
    ) {
        if (beneficiaryNames == null || beneficiaryNames.isBlank() ||
                beneficiaryUserIds == null || beneficiaryUserIds.isBlank()) {
            return Collections.emptyList();
        }

        List<String> names = Arrays.asList(beneficiaryNames.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        List<String> ids = Arrays.asList(beneficiaryUserIds.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        if (names.size() != ids.size()) {
            logger.warn()
                    .message("Mismatch between beneficiary names and IDs count")
                    .detail("namesCount", names.size())
                    .detail("idsCount", ids.size())
                    .log();
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String targetUserId : targetUserIds) {
            int index = ids.indexOf(targetUserId);
            if (index >= 0 && index < names.size()) {
                result.add(names.get(index));
            }
        }

        return result;
    }

    // helper to check if the targetUserId is in the list of beneficiaryUserIds
    private boolean beneficiaryContains(String beneficiaryUserIds, String targetUserId) {
        if (beneficiaryUserIds == null || beneficiaryUserIds.isBlank()) return false;
        return Arrays.asList(beneficiaryUserIds.split(","))
                .stream()
                .map(String::trim)
                .anyMatch(id -> id.equals(targetUserId));
    }

    /**
     * Parses comma-separated beneficiary user IDs into a list.
     */
    private List<String> parseBeneficiaryIds(String beneficiaryUserIds) {
        if (beneficiaryUserIds == null || beneficiaryUserIds.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.asList(beneficiaryUserIds.split(","))
                .stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());
    }
}
