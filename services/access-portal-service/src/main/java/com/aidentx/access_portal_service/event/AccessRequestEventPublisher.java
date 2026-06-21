package com.aidentx.access_portal_service.event;

import com.aidentx.access_portal_service.model.RoleAssignmentDetail;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for publishing access request events to Kafka for workflow processing.
 *
 * <p>This service publishes role assignment events that trigger approval workflows
 * in the workflow service. Events are published to the "role-events" topic.</p>
 *
 * @author AidentX Development Team
 * @version 1.0
 */
@Service
public class AccessRequestEventPublisher {

    private static final String ROLE_EVENTS_TOPIC = "role-events";
    private static final String SOURCE_SERVICE = "access-portal-service";
    private static final String EVENT_TYPE_ROLE_ASSIGNED = "role.assigned";
    private static final String HEADER_WORKFLOW_EXECUTION_ID = "X-Workflow-Execution-Id";
    private static final String METADATA_KEY_WORKFLOW_EXECUTION_ID = "workflowExecutionId";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    @Autowired
    public AccessRequestEventPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger("AccessRequestEventPublisher", "ACCESS_REQUEST_EVENTS");
    }

    /**
     * Publishes a single role assignment event with manager information.
     * This method publishes one event per role, including user-manager mappings.
     *
     * @param accessRequestId the access request ID
     * @param roleId the role ID being assigned
     * @param users list of user IDs to assign this role to
     * @param managers list of user-manager mappings [{userId, managerId, userName}]
     * @param roleOwners list of role owner user IDs for approval
     * @param roleName the name of the role being assigned
     * @param roleJustification role-specific justification
     * @param validityDays role-specific validity in days
     * @param overallJustification overall justification for the request
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting assignment
     * @param createdById user ID of the user requesting assignment
     * @param tileId the tile ID from which request originated
     * @param tileTitle the tile title
     * @param shouldSendNotification whether to send notifications
     */
    public void publishSingleRoleAssignmentRequestEvent(
            UUID accessRequestId,
            UUID roleId,
            List<UUID> users,
            List<Map<String, String>> managers,
            List<UUID> roleOwners,
            String roleName,
            String roleJustification,
            Integer validityDays,
            String overallJustification,
            String tenantId,
            String workflowId,
            String createdBy,
            UUID createdById,
            UUID tileId,
            String tileTitle,
            boolean shouldSendNotification) {

        try {
            // Build payload that matches the new per-role structure
            Map<String, Object> payload = new HashMap<>();

            // Core role assignment data
            payload.put("accessRequestId", accessRequestId.toString());
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);  // Add role name to payload
            
            // User Ids array - list of user IDs to assign this role to
            payload.put("userIds", users.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));

            // Managers array - list of {userId, managerId, userName} mappings
            payload.put("managers", managers);

            // Role owners array - approvers for this role
            payload.put("ownerIds", roleOwners.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));

            // Justifications and metadata
            payload.put("roleJustification", roleJustification);
            payload.put("validityDays", validityDays);
            payload.put("overallJustification", overallJustification);
            payload.put("shouldSendNotification", shouldSendNotification);

            // Workflow specific data
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById.toString());
            payload.put("sendNotificationOnCompletion", shouldSendNotification);

            // Access Portal specific context
            payload.put("tileId", tileId.toString());
            payload.put("tileTitle", tileTitle);

            // Extract JWT token and user info from current HTTP request context
            extractRequestContext(payload);

            // Build event
            WorkflowEvent event = new WorkflowEvent(EVENT_TYPE_ROLE_ASSIGNED, SOURCE_SERVICE, tenantId);
            event.setPayload(payload);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "access-portal-single-role");
            metadata.put("version", "3.0"); // New version for per-role events
            metadata.put("accessRequestId", accessRequestId.toString());
            metadata.put("roleId", roleId.toString());
            metadata.put("assignmentType", "single-role");
            metadata.put("userCount", users.size());
            metadata.put("managerCount", managers.size());
            metadata.put("ownerCount", roleOwners.size());
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);

            // Send event - use accessRequestId + roleId as key for partitioning
            String partitionKey = accessRequestId.toString() + "_" + roleId.toString();
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, partitionKey, event);

            logger.info()
                    .message("Published single role assignment request event to Kafka")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("roleId", roleId.toString())
                    .detail("userCount", String.valueOf(users.size()))
                    .detail("managerCount", String.valueOf(managers.size()))
                    .detail("ownerCount", String.valueOf(roleOwners.size()))
                    .detail("workflowId", workflowId)
                    .detail("tenantId", tenantId)
                    .detail("topic", ROLE_EVENTS_TOPIC)
                    .detail("eventId", event.getEventId().toString())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish single role assignment request event")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("roleId", roleId.toString())
                    .detail("error", e.getMessage())
                    .log();
            throw new RuntimeException("Failed to publish workflow event for single role assignment", e);
        }
    }

    /**
     * Publishes role assignment event for cart-based workflow approval.
     * The workflow will call the /assign-from-cart API upon approval.
     *
     * @param accessRequestId the access request ID
     * @param requestItemId the first request item ID (for reference)
     * @param roleUserMap map of role IDs to assignment details (userIds, justification, validity)
     * @param overallJustification overall justification for the cart
     * @param ownerIds list of all role owner user IDs for approval (aggregated from all roles)
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting assignment
     * @param createdById user ID of the user requesting assignment
     * @param tileId the tile ID from which request originated
     * @param tileTitle the tile title
     * @param shouldSendNotification whether to send notifications
     */
    public void publishCartBasedRoleAssignmentRequestEvent(
            UUID accessRequestId,
            UUID requestItemId,
            Map<UUID, RoleAssignmentDetail> roleUserMap,
            String overallJustification,
            List<UUID> ownerIds,  // KEPT: Aggregated owners from all roles
            String tenantId,
            String workflowId,
            String createdBy,
            UUID createdById,
            UUID tileId,
            String tileTitle,
            boolean shouldSendNotification) {

        try {
            // Build payload that matches /assign-from-cart API structure
            Map<String, Object> payload = new HashMap<>();

            // Build roleUserMap for the API - convert to serializable format
            Map<String, Map<String, Object>> serializedRoleUserMap = new HashMap<>();

            // Log owner IDs and user IDs at the beginning
            logger.info()
                    .message("Starting cart-based role assignment request event publishing")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("ownerIdsCount", String.valueOf(ownerIds.size()))
                    .detail("ownerIds", ownerIds.stream()
                            .map(UUID::toString)
                            .collect(Collectors.joining(", ", "[", "]")))
                    .log();

            // Log total user count across all roles
            int totalUserCount = roleUserMap.values().stream()
                    .mapToInt(detail -> detail.userIds.size())
                    .sum();

            logger.info()
                    .message("Processing roleUserMap")
                    .detail("totalRoles", String.valueOf(roleUserMap.size()))
                    .detail("totalUsers", String.valueOf(totalUserCount))
                    .log();


            for (Map.Entry<UUID, RoleAssignmentDetail> entry : roleUserMap.entrySet()) {
                UUID roleId = entry.getKey();
                RoleAssignmentDetail detail = entry.getValue();

                // Log each role's user IDs
                String roleUsers = detail.userIds.stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining(", "));

                logger.info()
                        .message("Processing role")
                        .detail("roleId", roleId.toString())
                        .detail("userCount", String.valueOf(detail.userIds.size()))
                        .detail("userIds", detail.userIds.size() <= 10 ?
                                "[" + roleUsers + "]" :
                                "First 10 users: [" + detail.userIds.stream()
                                        .limit(10)
                                        .map(UUID::toString)
                                        .collect(Collectors.joining(", ")) + "...]")
                        .log();

                Map<String, Object> roleDetail = new HashMap<>();
                roleDetail.put("userIds", detail.userIds.stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()));
                roleDetail.put("justification", detail.justification);
                roleDetail.put("validityDays", detail.validityDays);

                serializedRoleUserMap.put(roleId.toString(), roleDetail);
            }

            payload.put("roleUserMap", serializedRoleUserMap);
            payload.put("overallJustification", overallJustification);
            payload.put("shouldSendNotification", shouldSendNotification);

            // Approval workflow specific data
            payload.put("ownerIds", ownerIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById.toString());
            payload.put("sendNotificationOnCompletion", shouldSendNotification);

            // Access Portal specific context
            payload.put("accessRequestId", accessRequestId.toString());
            payload.put("requestItemId", requestItemId.toString());
            payload.put("tileId", tileId.toString());
            payload.put("tileTitle", tileTitle);

            // Extract JWT token and user info from current HTTP request context
            extractRequestContext(payload);

            // Build event
            WorkflowEvent event = new WorkflowEvent(EVENT_TYPE_ROLE_ASSIGNED, SOURCE_SERVICE, tenantId);
            event.setPayload(payload);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "access-portal-cart-request");
            metadata.put("version", "2.0"); // Changed version for cart-based
            metadata.put("accessRequestId", accessRequestId.toString());
            metadata.put("requestItemId", requestItemId.toString());
            metadata.put("assignmentType", "cart-based");
            metadata.put("roleCount", String.valueOf(roleUserMap.size()));
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);

            // Send event - use accessRequestId as key for partitioning
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, accessRequestId.toString(), event);

            logger.info()
                    .message("Published cart-based role assignment request event to Kafka")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("requestItemId", requestItemId.toString())
                    .detail("roleCount", String.valueOf(roleUserMap.size()))
                    .detail("workflowId", workflowId)
                    .detail("tenantId", tenantId)
                    .detail("topic", ROLE_EVENTS_TOPIC)
                    .detail("eventId", event.getEventId().toString())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish cart-based role assignment request event")
                    .detail("accessRequestId", accessRequestId.toString())
                    .detail("roleCount", String.valueOf(roleUserMap.size()))
                    .detail("error", e.getMessage())
                    .log();
            throw new RuntimeException("Failed to publish workflow event for cart-based role assignment", e);
        }
    }


    /**
     * Extracts JWT token and user info from current HTTP request context.
     */
    private void extractRequestContext(Map<String, Object> payload) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest httpRequest = attrs.getRequest();

                // Extract JWT token
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwtToken = authHeader.substring(7);
                    payload.put("jwtToken", jwtToken);
                    logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .log();
                }

                // Extract user ID from header (required by workflows-service)
                String userId = httpRequest.getHeader("X-User-Id");
                if (userId != null) {
                    payload.put("userId", userId);
                }
            }
        } catch (Exception e) {
            logger.warn()
                    .message("Could not extract JWT token or user ID for workflow execution")
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Adds workflow execution ID from request header to metadata if present.
     * This enables workflow chaining and correlation.
     */
    private void addWorkflowOriginMetadataIfPresent(Map<String, Object> metadata) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String workflowExecutionId = request.getHeader(HEADER_WORKFLOW_EXECUTION_ID);
                if (workflowExecutionId != null && !workflowExecutionId.isEmpty()) {
                    metadata.put(METADATA_KEY_WORKFLOW_EXECUTION_ID, workflowExecutionId);
                    logger.debug()
                            .message("Added workflow execution ID to event metadata")
                            .detail("workflowExecutionId", workflowExecutionId)
                            .log();
                }
            }
        } catch (Exception e) {
            logger.debug()
                    .message("Could not extract workflow execution ID from request")
                    .detail("error", e.getMessage())
                    .log();
        }
    }

//    /**
//     * Data class for batch role assignment requests.
//     * Supports multiple roles and multiple users per request.
//     */
//    public static class RoleAssignmentRequestData {
//        public final UUID accessRequestId;
//        public final UUID requestItemId;
//        public final List<UUID> roleIds;
//        public final List<String> roleNames;
//        public final List<UUID> userIds; // Requester + all beneficiaries
//        public final String justification;
//        public final Integer validityDays;
//        public final String urgency;
//        public final List<UUID> ownerIds;
//        public final String tenantId;
//        public final String workflowId;
//        public final String createdBy;
//        public final UUID createdById;
//        public final UUID tileId;
//        public final String tileTitle;
//        public final String requestType; // SELF or OTHERS
//
//        public RoleAssignmentRequestData(
//                UUID accessRequestId, UUID requestItemId,
//                List<UUID> roleIds, List<String> roleNames,
//                List<UUID> userIds,
//                String justification, Integer validityDays, String urgency,
//                List<UUID> ownerIds, String tenantId, String workflowId,
//                String createdBy, UUID createdById, UUID tileId, String tileTitle,
//                String requestType) {
//            this.accessRequestId = accessRequestId;
//            this.requestItemId = requestItemId;
//            this.roleIds = roleIds;
//            this.roleNames = roleNames;
//            this.userIds = userIds;
//            this.justification = justification;
//            this.validityDays = validityDays;
//            this.urgency = urgency;
//            this.ownerIds = ownerIds;
//            this.tenantId = tenantId;
//            this.workflowId = workflowId;
//            this.createdBy = createdBy;
//            this.createdById = createdById;
//            this.tileId = tileId;
//            this.tileTitle = tileTitle;
//            this.requestType = requestType;
//        }
//    }


//    /**
//     * Publishes multiple role assignment events for batch processing.
//     * Used when an access request contains multiple role items.
//     *
//     * @param roleAssignmentRequests list of role assignment request data
//     */
//    public void publishBatchRoleAssignmentRequestEvents(List<RoleAssignmentRequestData> roleAssignmentRequests) {
//        logger.info()
//                .message("Publishing batch role assignment events")
//                .detail("batchSize", String.valueOf(roleAssignmentRequests.size()))
//                .log();
//
//        int successCount = 0;
//        int failureCount = 0;
//
//        for (RoleAssignmentRequestData request : roleAssignmentRequests) {
//            try {
//                publishRoleAssignmentRequestEvent(
//                        request.accessRequestId,
//                        request.requestItemId,
//                        request.roleIds,
//                        request.roleNames,
//                        request.userIds,
//                        request.justification,
//                        request.validityDays,
//                        request.urgency,
//                        request.ownerIds,
//                        request.tenantId,
//                        request.workflowId,
//                        request.createdBy,
//                        request.createdById,
//                        request.tileId,
//                        request.tileTitle,
//                        request.requestType
//                );
//                successCount++;
//            } catch (Exception e) {
//                failureCount++;
//                logger.error(e)
//                        .message("Failed to publish event in batch")
//                        .detail("requestItemId", request.requestItemId.toString())
//                        .detail("roleCount", String.valueOf(request.roleIds.size()))
//                        .log();
//            }
//        }
//
//        logger.info()
//                .message("Batch role assignment events published")
//                .detail("totalRequests", String.valueOf(roleAssignmentRequests.size()))
//                .detail("successCount", String.valueOf(successCount))
//                .detail("failureCount", String.valueOf(failureCount))
//                .log();
//
//        if (failureCount > 0) {
//            throw new RuntimeException(
//                    String.format("Failed to publish %d out of %d workflow events",
//                            failureCount, roleAssignmentRequests.size())
//            );
//        }
//    }

}