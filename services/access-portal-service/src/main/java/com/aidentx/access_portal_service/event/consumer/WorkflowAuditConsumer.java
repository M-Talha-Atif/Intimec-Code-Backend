//package com.aidentx.access_portal_service.event.consumer;
//
//import com.aidentx.access_portal_service.entity.access_request.*;
//import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
//import com.aidentx.access_portal_service.repository.access_request.RequestProvisioningRepository;
//import com.aidentx.auditlogdto.AuditLogKafkaEvent;
//import com.aidentx.logging.service.IUnifiedLogger;
//import com.aidentx.logging.service.UnifiedLoggerFactory;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.OffsetDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Kafka Consumer for processing workflow audit events.
// * <p>
// * This service listens to the audit-log Kafka topic and processes workflow-related events
// * to update access request entities with approval steps, provisioning status, and audit logs.
// * It maps workflow events to the access portal's entity model for complete request tracking.
// * </p>
// *
// * @author aidentx
// * @since 2026-01-23
// */
//@Service
//@Transactional
//public class WorkflowAuditConsumer {
//
//    private static final String WORKFLOW_SERVICE_METADATA = "workflows-service";
//    private static final String SYSTEM_ACTOR_ID = "00000000-0000-0000-0000-000000000000";
//    private static final String SYSTEM_ACTOR_NAME = "System";
//
//    // Event action constants based on your JSON examples
//    private static final String APPROVAL_CREATED = "approval-created";
//    private static final String APPROVAL_ASSIGNED = "approval-assigned";
//    private static final String APPROVAL_APPROVED = "approval-approved";
//    private static final String APPROVAL_REJECTED = "approval-rejected";
//    private static final String WORKFLOW_EXECUTED = "workflow-executed";
//    private static final String WORKFLOW_EXECUTION_COMPLETED = "workflow-execution-completed";
//    private static final String WORKFLOW_EXECUTION_FAILED = "workflow-execution-failed";
//    private static final String WORKFLOW_EXECUTION_CANCELLED = "workflow-execution-cancelled";
//
//    private final AccessRequestRepository requestRepository;
//    private final RequestProvisioningRepository provisioningRepository;
//    private final IUnifiedLogger logger;
//    private final ObjectMapper objectMapper;
//
//    /**
//     * Constructor for WorkflowAuditConsumer.
//     *
//     * @param requestRepository Repository for accessing AccessRequest entities
//     * @param provisioningRepository Repository for accessing RequestProvisioning entities
//     * @param loggerFactory Factory for creating unified logger instances
//     * @param objectMapper Jackson ObjectMapper for JSON processing
//     */
//    public WorkflowAuditConsumer(
//            AccessRequestRepository requestRepository,
//            RequestProvisioningRepository provisioningRepository,
//            UnifiedLoggerFactory loggerFactory,
//            ObjectMapper objectMapper
//    ) {
//        this.requestRepository = requestRepository;
//        this.provisioningRepository = provisioningRepository;
//        this.objectMapper = objectMapper;
//        this.logger = loggerFactory.getLogger(
//                "WorkflowAuditConsumer",
//                "workflow-audit-consumer"
//        );
//    }
//
//    /**
//     * Kafka listener method for consuming workflow audit events.
//     * <p>
//     * Processes audit events from the workflows-service and updates corresponding
//     * access request entities with workflow progress, approvals, and provisioning status.
//     * </p>
//     *
//     * @param event The audit log event from Kafka
//     * @param ack Kafka acknowledgment for manual commit
//     */
//    @KafkaListener(
//            topics = "audit-log",
//            groupId = "access-portal-workflow-consumer",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consumeWorkflowAudit(AuditLogKafkaEvent event, Acknowledgment ack) {
//        try {
//            logger.info()
//                    .message("Received workflow audit event")
//                    .detail("action", event.getEvent())
//                    .detail("entityId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                    .detail("entityType", event.getObjectType())
//                    .log();
//
//            // Filter for workflow service events
//            JsonNode metadata = event.getMetadata();
//            if (metadata == null || !WORKFLOW_SERVICE_METADATA.equals(metadata.get("service").asText())) {
//                logger.debug()
//                        .message("Ignoring non-workflow service event")
//                        .detail("service", metadata != null ? metadata.get("service").asText() : "null")
//                        .log();
//                ack.acknowledge();
//                return;
//            }
//
//            // Extract request ID from event
//            UUID requestId = extractRequestId(event);
//            if (requestId == null) {
//                logger.warn()
//                        .message("Cannot extract requestId from event")
//                        .detail("eventType", event.getObjectType())
//                        .detail("entityId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                        .log();
//                ack.acknowledge();
//                return;
//            }
//
//            // Fetch the access request with items
//            AccessRequest request = requestRepository.findByIdWithItems(requestId, event.getTenantId())
//                    .orElseThrow(() -> {
//                        logger.error()
//                                .message("Access request not found for workflow event")
//                                .detail("requestId", requestId.toString())
//                                .detail("workflowEventId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                                .detail("tenantId", event.getTenantId())
//                                .log();
//                        return new IllegalArgumentException("Access request not found: " + requestId);
//                    });
//
//            // Process based on action
//            String action = event.getEvent();
//            switch (action) {
//                case APPROVAL_CREATED -> handleApprovalCreated(event, request);
//                case APPROVAL_ASSIGNED -> handleApprovalAssigned(event, request);
//                case APPROVAL_APPROVED -> handleApprovalApproved(event, request);
//                case APPROVAL_REJECTED -> handleApprovalRejected(event, request);
//                case "approval-delegated" -> handleApprovalDelegated(event, request);
//                case "approval-escalated" -> handleApprovalEscalated(event, request);
//                case "approval-info-requested" -> handleApprovalInfoRequested(event, request);
//                case WORKFLOW_EXECUTED -> handleWorkflowExecuted(event, request);
//                case WORKFLOW_EXECUTION_COMPLETED -> handleWorkflowCompleted(event, request);
//                case WORKFLOW_EXECUTION_FAILED -> handleWorkflowFailed(event, request);
//                case WORKFLOW_EXECUTION_CANCELLED -> handleWorkflowCancelled(event, request);
//                default -> logger.warn()
//                        .message("Unhandled workflow action")
//                        .detail("action", action)
//                        .detail("requestId", requestId.toString())
//                        .log();
//            }
//
//            // Save the updated request
//            requestRepository.save(request);
//
//            logger.info()
//                    .message("Successfully processed workflow event")
//                    .detail("action", action)
//                    .detail("requestId", requestId.toString())
//                    .detail("requestStatus", request.getStatus().name())
//                    .log();
//
//            ack.acknowledge();
//
//        } catch (Exception e) {
//            logger.error(e)
//                    .message("Failed to process workflow audit event")
//                    .detail("event", event.toString())
//                    .detail("error", e.getMessage())
//                    .log();
//            // Don't acknowledge on error for retry
//        }
//    }
//
//    /**
//     * Handles approval created event when workflow starts an approval process.
//     * <p>
//     * Creates initial approval step and updates request status to PENDING_APPROVAL.
//     * </p>
//     *
//     * @param event The audit event containing approval creation details
//     * @param request The access request to update
//     */
//    private void handleApprovalCreated(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval created event")
//                .detail("requestId", request.getId().toString())
//                .detail("workflowId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                .log();
//
//        // Create initial approval step from workflow event
//        RequestApprovalStep approvalStep = new RequestApprovalStep();
//        approvalStep.setAccessRequest(request);
//        approvalStep.setStepOrder(1); // First approval step
//
//        // Extract approver info from event
//        JsonNode stateAfter = event.getStateAfter();
//        if (stateAfter != null) {
//            // Extract approver type from evidenceBundle or approverType field
//            if (stateAfter.has("approverType")) {
//                approvalStep.setApproverRole(stateAfter.get("approverType").asText());
//            } else if (stateAfter.has("evidenceBundle") && stateAfter.get("evidenceBundle").has("nodeName")) {
//                approvalStep.setApproverRole(stateAfter.get("evidenceBundle").get("nodeName").asText());
//            }
//        }
//
//        approvalStep.setStatus(ApprovalStepStatus.PENDING_ASSIGNMENT);
//        approvalStep.setDecision(ApprovalDecision.PENDING);
//        approvalStep.setTenantId(request.getTenantId());
//
//        request.getApprovalSteps().add(approvalStep);
//
//        // Create audit log
//        createAuditLog(
//                request,
//                AuditAction.ASSIGNED_TO_APPROVER,
//                event.getActorId(),
//                event.getActorName(),
//                "Approval workflow initiated for request. Waiting for approver assignment."
//        );
//
//        // Update request status
//        request.setStatus(RequestStatus.PENDING_APPROVAL);
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles approval assigned event when an approver is assigned to the request.
//     *
//     * @param event The audit event containing assignment details
//     * @param request The access request to update
//     */
//    private void handleApprovalAssigned(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval assigned event")
//                .detail("requestId", request.getId().toString())
//                .detail("approverId", event.getActorId() != null ? event.getActorId().toString() : "null")
//                .detail("approverName", event.getActorName())
//                .log();
//
//        // Update the latest pending approval step
//        RequestApprovalStep pendingStep = findPendingApprovalStep(request);
//        if (pendingStep != null) {
//            pendingStep.setStatus(ApprovalStepStatus.ASSIGNED);
//            pendingStep.setAssignedApproverId(event.getActorId());
//            pendingStep.setAssignedApproverName(event.getActorName());
//            pendingStep.setAssignedApproverEmail(extractEmail(event));
//            pendingStep.setAssignedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//        }
//
//        // Create audit log
//        createAuditLog(
//                request,
//                AuditAction.ASSIGNED_TO_APPROVER,
//                event.getActorId(),
//                event.getActorName(),
//                "Approval assigned to " + event.getActorName()
//        );
//
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles approval approved event when an approver approves the request.
//     *
//     * @param event The audit event containing approval details
//     * @param request The access request to update
//     */
//    private void handleApprovalApproved(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval approved event")
//                .detail("requestId", request.getId().toString())
//                .detail("approver", event.getActorName())
//                .log();
//
//        // Update approval step
//        RequestApprovalStep approvalStep = findLatestApprovalStep(request);
//        if (approvalStep != null) {
//            approvalStep.setStatus(ApprovalStepStatus.COMPLETED);
//            approvalStep.setDecision(ApprovalDecision.APPROVED);
//            approvalStep.setRespondedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//            approvalStep.setComments(extractComments(event));
//        }
//
//        // Create audit log
//        createAuditLog(
//                request,
//                AuditAction.APPROVAL_STEP_COMPLETED,
//                event.getActorId(),
//                event.getActorName(),
//                "Approval step approved by " + event.getActorName()
//        );
//
//        // Check if all approvals are complete based on evidenceBundle.requiredApprovals
//        if (allApprovalsCompleted(request, event)) {
//            request.setStatus(RequestStatus.APPROVED);
//            request.setUpdatedAt(OffsetDateTime.now());
//
//            logger.info()
//                    .message("All approvals completed, request approved")
//                    .detail("requestId", request.getId().toString())
//                    .log();
//        }
//    }
//
//    /**
//     * Handles approval rejected event when an approver rejects the request.
//     *
//     * @param event The audit event containing rejection details
//     * @param request The access request to update
//     */
//    private void handleApprovalRejected(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval rejected event")
//                .detail("requestId", request.getId().toString())
//                .detail("approver", event.getActorName())
//                .log();
//
//        // Update approval step
//        RequestApprovalStep approvalStep = findLatestApprovalStep(request);
//        if (approvalStep != null) {
//            approvalStep.setStatus(ApprovalStepStatus.COMPLETED);
//            approvalStep.setDecision(ApprovalDecision.REJECTED);
//            approvalStep.setRespondedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//            approvalStep.setComments(extractComments(event));
//        }
//
//        // Create audit log
//        createAuditLog(
//                request,
//                AuditAction.REQUEST_REJECTED,
//                event.getActorId(),
//                event.getActorName(),
//                "Request rejected by " + event.getActorName() + ". Reason: " + extractRejectionReason(event)
//        );
//
//        // Update request status
//        request.setStatus(RequestStatus.REJECTED);
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles approval delegated event when an approver delegates the approval.
//     *
//     * @param event The audit event containing delegation details
//     * @param request The access request to update
//     */
//    private void handleApprovalDelegated(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval delegated event")
//                .detail("requestId", request.getId().toString())
//                .log();
//
//        // Update current step
//        RequestApprovalStep currentStep = findLatestApprovalStep(request);
//        if (currentStep != null) {
//            currentStep.setStatus(ApprovalStepStatus.DELEGATED);
//            currentStep.setDecision(ApprovalDecision.DELEGATED);
//            currentStep.setDelegatedToUserId(extractDelegatedToUserId(event));
//            currentStep.setDelegatedToUserName(extractDelegatedToUserName(event));
//            currentStep.setDelegatedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//            currentStep.setComments(extractComments(event));
//        }
//
//        // Create new delegated step
//        RequestApprovalStep delegatedStep = new RequestApprovalStep();
//        delegatedStep.setAccessRequest(request);
//        delegatedStep.setStepOrder((currentStep != null ? currentStep.getStepOrder() : 0) + 1);
//        delegatedStep.setApproverRole("Delegated Approver");
//        delegatedStep.setStatus(ApprovalStepStatus.PENDING_ASSIGNMENT);
//        delegatedStep.setDecision(ApprovalDecision.PENDING);
//        delegatedStep.setTenantId(request.getTenantId());
//
//        request.getApprovalSteps().add(delegatedStep);
//
//        // Create audit log
//        createAuditLog(
//                request,
//                AuditAction.REQUEST_DELEGATED,
//                event.getActorId(),
//                event.getActorName(),
//                "Approval delegated by " + event.getActorName()
//        );
//
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles approval escalated event when an approval is escalated.
//     *
//     * @param event The audit event containing escalation details
//     * @param request The access request to update
//     */
//    private void handleApprovalEscalated(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling approval escalated event")
//                .detail("requestId", request.getId().toString())
//                .log();
//
//        RequestApprovalStep currentStep = findLatestApprovalStep(request);
//        if (currentStep != null) {
//            currentStep.setStatus(ApprovalStepStatus.COMPLETED);
//            currentStep.setDecision(ApprovalDecision.ESCALATED);
//            currentStep.setComments(extractComments(event));
//        }
//
//        // Create new escalated step
//        RequestApprovalStep escalatedStep = new RequestApprovalStep();
//        escalatedStep.setAccessRequest(request);
//        escalatedStep.setStepOrder((currentStep != null ? currentStep.getStepOrder() : 0) + 1);
//        escalatedStep.setApproverRole("Escalated Approver");
//        escalatedStep.setStatus(ApprovalStepStatus.PENDING_ASSIGNMENT);
//        escalatedStep.setDecision(ApprovalDecision.PENDING);
//        escalatedStep.setTenantId(request.getTenantId());
//
//        request.getApprovalSteps().add(escalatedStep);
//
//        createAuditLog(
//                request,
//                AuditAction.REQUEST_ESCALATED,
//                event.getActorId(),
//                event.getActorName(),
//                "Request escalated by " + event.getActorName()
//        );
//
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles info requested event when an approver requests more information.
//     *
//     * @param event The audit event containing information request details
//     * @param request The access request to update
//     */
//    private void handleApprovalInfoRequested(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling info requested event")
//                .detail("requestId", request.getId().toString())
//                .log();
//
//        RequestApprovalStep approvalStep = findLatestApprovalStep(request);
//        if (approvalStep != null) {
//            approvalStep.setDecision(ApprovalDecision.INFO_REQUESTED);
//            approvalStep.setComments(extractComments(event));
//        }
//
//        createAuditLog(
//                request,
//                AuditAction.COMMENT_ADDED,
//                event.getActorId(),
//                event.getActorName(),
//                "Additional information requested by " + event.getActorName()
//        );
//
//        // Add as comment
//        RequestComment comment = new RequestComment();
//        comment.setAccessRequest(request);
//        comment.setCommentedByUserId(event.getActorId());
//        comment.setCommentedByUserName(event.getActorName());
//        comment.setCommentText("Information requested: " + extractComments(event));
//        comment.setCommentType(CommentType.INFO_REQUEST);
//        comment.setCommentedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//        comment.setIsInternal(false);
//        comment.setTenantId(request.getTenantId());
//
//        request.getComments().add(comment);
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles workflow execution started event.
//     *
//     * @param event The audit event containing execution details
//     * @param request The access request to update
//     */
//    private void handleWorkflowExecuted(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling workflow execution started")
//                .detail("requestId", request.getId().toString())
//                .detail("executionId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                .log();
//
//        createAuditLog(
//                request,
//                AuditAction.STATUS_CHANGED,
//                UUID.fromString(SYSTEM_ACTOR_ID),
//                SYSTEM_ACTOR_NAME,
//                "Workflow execution started for provisioning. Execution ID: " +
//                        (event.getObjectId() != null ? event.getObjectId().toString() : "unknown")
//        );
//
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles workflow completed event when workflow execution finishes successfully.
//     *
//     * @param event The audit event containing completion details
//     * @param request The access request to update
//     */
//    private void handleWorkflowCompleted(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.info()
//                .message("Handling workflow completed event")
//                .detail("requestId", request.getId().toString())
//                .detail("executionId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                .log();
//
//        // Create provisioning records for all items
//        for (AccessRequestItem item : request.getItems()) {
//            RequestProvisioning provisioning = new RequestProvisioning();
//            provisioning.setRequestItem(item);
//            provisioning.setStatus(ProvisioningStatus.COMPLETED);
//            provisioning.setProvisioningStartedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//            provisioning.setProvisioningCompletedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//            provisioning.setTargetSystem("application-permissions-service");
//            provisioning.setProvisioningDetails("Access provisioned via workflow execution. Execution ID: " +
//                    (event.getObjectId() != null ? event.getObjectId().toString() : "unknown"));
//            provisioning.setAccessExpiresAt(calculateExpiryDate(item.getValidityDays()));
//            provisioning.setTenantId(request.getTenantId());
//
//            item.setProvisioning(provisioning);
//        }
//
//        createAuditLog(
//                request,
//                AuditAction.ACCESS_PROVISIONED,
//                UUID.fromString(SYSTEM_ACTOR_ID),
//                SYSTEM_ACTOR_NAME,
//                "Access provisioned successfully via workflow. Execution ID: " +
//                        (event.getObjectId() != null ? event.getObjectId().toString() : "unknown")
//        );
//
//        // Update request status
//        request.setStatus(RequestStatus.PROVISIONED);
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles workflow failed event when workflow execution fails.
//     *
//     * @param event The audit event containing failure details
//     * @param request The access request to update
//     */
//    private void handleWorkflowFailed(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.error()
//                .message("Handling workflow failed event")
//                .detail("requestId", request.getId().toString())
//                .detail("executionId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                .log();
//
//        // Create failed provisioning records
//        for (AccessRequestItem item : request.getItems()) {
//            if (item.getProvisioning() == null) {
//                RequestProvisioning provisioning = new RequestProvisioning();
//                provisioning.setRequestItem(item);
//                provisioning.setStatus(ProvisioningStatus.FAILED);
//                provisioning.setProvisioningStartedAt(event.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
//                provisioning.setTargetSystem("application-permissions-service");
//                provisioning.setErrorMessage(extractErrorMessage(event));
//                provisioning.setTenantId(request.getTenantId());
//                item.setProvisioning(provisioning);
//            } else {
//                item.getProvisioning().setStatus(ProvisioningStatus.FAILED);
//                item.getProvisioning().setErrorMessage(extractErrorMessage(event));
//            }
//        }
//
//        createAuditLog(
//                request,
//                AuditAction.PROVISIONING_FAILED,
//                UUID.fromString(SYSTEM_ACTOR_ID),
//                SYSTEM_ACTOR_NAME,
//                "Workflow execution failed: " + extractErrorMessage(event) +
//                        ". Execution ID: " + (event.getObjectId() != null ? event.getObjectId().toString() : "unknown")
//        );
//
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    /**
//     * Handles workflow cancelled event when workflow execution is cancelled.
//     *
//     * @param event The audit event containing cancellation details
//     * @param request The access request to update
//     */
//    private void handleWorkflowCancelled(AuditLogKafkaEvent event, AccessRequest request) {
//        logger.warn()
//                .message("Handling workflow cancelled event")
//                .detail("requestId", request.getId().toString())
//                .detail("executionId", event.getObjectId() != null ? event.getObjectId().toString() : "null")
//                .log();
//
//        createAuditLog(
//                request,
//                AuditAction.STATUS_CHANGED,
//                UUID.fromString(SYSTEM_ACTOR_ID),
//                SYSTEM_ACTOR_NAME,
//                "Workflow execution cancelled. Execution ID: " +
//                        (event.getObjectId() != null ? event.getObjectId().toString() : "unknown")
//        );
//
//        // Update provisioning status to CANCELLED if exists
//        for (AccessRequestItem item : request.getItems()) {
//            if (item.getProvisioning() != null) {
//                item.getProvisioning().setStatus(ProvisioningStatus.CANCELLED);
//            }
//        }
//
//        request.setStatus(RequestStatus.CANCELLED);
//        request.setUpdatedAt(OffsetDateTime.now());
//    }
//
//    // ========== HELPER METHODS ==========
//
//    /**
//     * Extracts the access request ID from the audit event.
//     * <p>
//     * The request ID can be found in multiple locations:
//     * 1. approvalContext.accessRequestId (for approval events)
//     * 2. executionContext.accessRequestId (for execution events)
//     * 3. metadata.requestId (fallback)
//     * </p>
//     *
//     * @param event The audit log event
//     * @return UUID of the access request, or null if not found
//     */
//    private UUID extractRequestId(AuditLogKafkaEvent event) {
//        try {
//            // Check stateAfter first (contains approval/execution details)
//            JsonNode stateAfter = event.getStateAfter();
//            if (stateAfter != null) {
//                // For approval events: check approvalContext.accessRequestId
//                if (stateAfter.has("approvalContext") && stateAfter.get("approvalContext").has("accessRequestId")) {
//                    String requestIdStr = stateAfter.get("approvalContext").get("accessRequestId").asText();
//                    return UUID.fromString(requestIdStr);
//                }
//
//                // For execution events: check executionContext.accessRequestId
//                if (stateAfter.has("executionContext") && stateAfter.get("executionContext").has("accessRequestId")) {
//                    String requestIdStr = stateAfter.get("executionContext").get("accessRequestId").asText();
//                    return UUID.fromString(requestIdStr);
//                }
//
//                // Direct accessRequestId field (if present at root)
//                if (stateAfter.has("accessRequestId")) {
//                    return UUID.fromString(stateAfter.get("accessRequestId").asText());
//                }
//            }
//
//            // Check details field
//            JsonNode details = event.getDetails();
//            if (details != null) {
//                if (details.has("accessRequestId")) {
//                    return UUID.fromString(details.get("accessRequestId").asText());
//                }
//                if (details.has("requestId")) {
//                    return UUID.fromString(details.get("requestId").asText());
//                }
//            }
//
//            // Check metadata
//            JsonNode metadata = event.getMetadata();
//            if (metadata != null && metadata.has("requestId")) {
//                return UUID.fromString(metadata.get("requestId").asText());
//            }
//
//            logger.warn()
//                    .message("Cannot find accessRequestId in event")
//                    .detail("eventId", event.getId() != null ? event.getId().toString() : "null")
//                    .detail("eventType", event.getObjectType())
//                    .log();
//            return null;
//
//        } catch (Exception e) {
//            logger.error(e)
//                    .message("Error extracting requestId from event")
//                    .detail("eventId", event.getId() != null ? event.getId().toString() : "null")
//                    .detail("error", e.getMessage())
//                    .log();
//            return null;
//        }
//    }
//
//    /**
//     * Finds the pending approval step for the request.
//     *
//     * @param request The access request
//     * @return The pending approval step, or null if not found
//     */
//    private RequestApprovalStep findPendingApprovalStep(AccessRequest request) {
//        return request.getApprovalSteps().stream()
//                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING_ASSIGNMENT)
//                .findFirst()
//                .orElse(null);
//    }
//
//    /**
//     * Finds the latest approval step for the request.
//     *
//     * @param request The access request
//     * @return The latest approval step (by stepOrder), or null if no steps exist
//     */
//    private RequestApprovalStep findLatestApprovalStep(AccessRequest request) {
//        if (request.getApprovalSteps().isEmpty()) {
//            return null;
//        }
//        return request.getApprovalSteps().stream()
//                .max(Comparator.comparing(RequestApprovalStep::getStepOrder))
//                .orElse(null);
//    }
//
//    /**
//     * Checks if all approvals are completed for the request.
//     * <p>
//     * Uses evidenceBundle.requiredApprovals from the event to determine if all
//     * required approvals have been completed.
//     * </p>
//     *
//     * @param request The access request
//     * @param event The audit event containing evidenceBundle information
//     * @return true if all required approvals are completed, false otherwise
//     */
//    private boolean allApprovalsCompleted(AccessRequest request, AuditLogKafkaEvent event) {
//        try {
//            JsonNode stateAfter = event.getStateAfter();
//            if (stateAfter != null && stateAfter.has("evidenceBundle")) {
//                JsonNode evidenceBundle = stateAfter.get("evidenceBundle");
//                if (evidenceBundle.has("requiredApprovals")) {
//                    int requiredApprovals = evidenceBundle.get("requiredApprovals").asInt();
//
//                    // Count completed approval steps
//                    long completedSteps = request.getApprovalSteps().stream()
//                            .filter(step -> step.getStatus() == ApprovalStepStatus.COMPLETED)
//                            .count();
//
//                    boolean allCompleted = completedSteps >= requiredApprovals;
//
//                    logger.debug()
//                            .message("Checking approval completion")
//                            .detail("requestId", request.getId().toString())
//                            .detail("completedSteps", String.valueOf(completedSteps))
//                            .detail("requiredApprovals", String.valueOf(requiredApprovals))
//                            .detail("allCompleted", String.valueOf(allCompleted))
//                            .log();
//
//                    return allCompleted;
//                }
//            }
//
//            // Fallback: check if any steps are pending
//            return request.getApprovalSteps().stream()
//                    .noneMatch(step -> step.getStatus() == ApprovalStepStatus.PENDING_ASSIGNMENT ||
//                            step.getStatus() == ApprovalStepStatus.ASSIGNED ||
//                            step.getDecision() == ApprovalDecision.PENDING);
//
//        } catch (Exception e) {
//            logger.error(e)
//                    .message("Error checking approval completion")
//                    .detail("requestId", request.getId().toString())
//                    .log();
//            return false;
//        }
//    }
//
//    /**
//     * Creates an audit log entry for the access request.
//     *
//     * @param request The access request
//     * @param action The audit action
//     * @param performedByUserId ID of the user who performed the action
//     * @param performedByUserName Name of the user who performed the action
//     * @param details Description of the action
//     */
//    private void createAuditLog(AccessRequest request, AuditAction action,
//                                UUID performedByUserId, String performedByUserName,
//                                String details) {
//        RequestAuditLog auditLog = new RequestAuditLog();
//        auditLog.setAccessRequest(request);
//        auditLog.setAction(action);
//        auditLog.setPerformedByUserId(performedByUserId);
//        auditLog.setPerformedByUserName(performedByUserName);
//        auditLog.setPerformedAt(OffsetDateTime.now());
//        auditLog.setPreviousStatus(request.getStatus().name());
//        auditLog.setNewStatus(request.getStatus().name());
//        auditLog.setDetails(details);
//        auditLog.setTenantId(request.getTenantId());
//
//        request.getAuditLogs().add(auditLog);
//    }
//
//    /**
//     * Extracts email from the audit event.
//     *
//     * @param event The audit event
//     * @return Email address, or null if not found
//     */
//    private String extractEmail(AuditLogKafkaEvent event) {
//        JsonNode details = event.getDetails();
//        if (details != null && details.has("email")) {
//            return details.get("email").asText();
//        }
//        JsonNode stateAfter = event.getStateAfter();
//        if (stateAfter != null && stateAfter.has("email")) {
//            return stateAfter.get("email").asText();
//        }
//        return null;
//    }
//
//    /**
//     * Extracts comments from the audit event.
//     * <p>
//     * Comments can be in the "comments" field at root level or in details.
//     * </p>
//     *
//     * @param event The audit event
//     * @return Comments string, or empty string if not found
//     */
//    private String extractComments(AuditLogKafkaEvent event) {
//        JsonNode stateAfter = event.getStateAfter();
//        if (stateAfter != null && stateAfter.has("comments")) {
//            return stateAfter.get("comments").asText();
//        }
//        JsonNode details = event.getDetails();
//        if (details != null && details.has("comments")) {
//            return details.get("comments").asText();
//        }
//        return "";
//    }
//
//    /**
//     * Extracts rejection reason from the audit event.
//     *
//     * @param event The audit event
//     * @return Rejection reason, or comments if no specific reason found
//     */
//    private String extractRejectionReason(AuditLogKafkaEvent event) {
//        JsonNode details = event.getDetails();
//        if (details != null && details.has("rejectionReason")) {
//            return details.get("rejectionReason").asText();
//        }
//        JsonNode stateAfter = event.getStateAfter();
//        if (stateAfter != null && stateAfter.has("rejectionReason")) {
//            return stateAfter.get("rejectionReason").asText();
//        }
//        return extractComments(event);
//    }
//
//    /**
//     * Extracts error message from the audit event.
//     *
//     * @param event The audit event
//     * @return Error message, or "Unknown error" if not found
//     */
//    private String extractErrorMessage(AuditLogKafkaEvent event) {
//        JsonNode details = event.getDetails();
//        if (details != null && details.has("errorMessage")) {
//            return details.get("errorMessage").asText();
//        }
//        if (event.getErrorDetails() != null) {
//            return event.getErrorDetails();
//        }
//        return "Unknown error";
//    }
//
//    /**
//     * Extracts delegated to user ID from the audit event.
//     *
//     * @param event The audit event
//     * @return Delegated to user ID, or null if not found
//     */
//    private UUID extractDelegatedToUserId(AuditLogKafkaEvent event) {
//        try {
//            JsonNode stateAfter = event.getStateAfter();
//            if (stateAfter != null && stateAfter.has("delegatedToUserId")) {
//                return UUID.fromString(stateAfter.get("delegatedToUserId").asText());
//            }
//        } catch (Exception e) {
//            logger.error(e)
//                    .message("Cannot extract delegatedToUserId")
//                    .detail("error", e.getMessage())
//                    .log();
//        }
//        return null;
//    }
//
//    /**
//     * Extracts delegated to user name from the audit event.
//     *
//     * @param event The audit event
//     * @return Delegated to user name, or null if not found
//     */
//    private String extractDelegatedToUserName(AuditLogKafkaEvent event) {
//        JsonNode stateAfter = event.getStateAfter();
//        if (stateAfter != null && stateAfter.has("delegatedToUserName")) {
//            return stateAfter.get("delegatedToUserName").asText();
//        }
//        return null;
//    }
//
//    /**
//     * Calculates expiry date based on validity days.
//     *
//     * @param validityDays Number of days the access is valid for
//     * @return Expiry date, or null for permanent access
//     */
//    private OffsetDateTime calculateExpiryDate(Integer validityDays) {
//        if (validityDays == null || validityDays <= 0) {
//            return null; // Permanent access
//        }
//        return OffsetDateTime.now().plusDays(validityDays);
//    }
//}