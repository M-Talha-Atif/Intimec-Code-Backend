package com.aidentx.applicationpermissions.roles_management.event;

import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.repository.ActionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PageRepository;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.auth.grpc.UserInfo;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service for publishing role lifecycle events to Kafka.
 * 
 * <p>This service handles the publishing of role-related events that trigger
 * workflows in the workflow service. Events are published to the "role-events" topic.</p>
 * 
 * <p>This service is only active when Kafka is configured and available.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class RoleEventPublisher {
    
    private static final String ROLE_EVENTS_TOPIC = "role-events";
    private static final String SOURCE_SERVICE = "application-permissions-service";

    private static final String HEADER_WORKFLOW_EXECUTION_ID = "X-Workflow-Execution-Id";
    private static final String METADATA_KEY_WORKFLOW_EXECUTION_ID = "workflowExecutionId";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final ActionRepository actionRepository;
    private final PageRepository pageRepository;
    private final UserGrpcClient userGrpcClient;
    
    @Autowired
    public RoleEventPublisher(@Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper,
                             UnifiedLoggerFactory loggerFactory,
                             ActionRepository actionRepository,
                             PageRepository pageRepository,
                             UserGrpcClient userGrpcClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger("RoleEventPublisher", "ROLE_EVENTS");
        this.actionRepository = actionRepository;
        this.pageRepository = pageRepository;
        this.userGrpcClient = userGrpcClient;
    }
    
    /**
     * Fetches the page name for a given action ID.
     * This resolves the Action -> Page relationship to get the page name.
     * 
     * @param actionId the action ID
     * @return the page name, or null if not found
     */
    private String fetchPageNameForAction(UUID actionId) {
        try {
            Optional<Action> actionOpt = actionRepository.findById(actionId);
            if (actionOpt.isPresent()) {
                Action action = actionOpt.get();
                UUID pageId = action.getPageId();
                if (pageId != null) {
                    Optional<Page> pageOpt = pageRepository.findById(pageId);
                    if (pageOpt.isPresent()) {
                        return pageOpt.get().getPageName();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn()
                .message("Could not fetch page name for action")
                .detail("actionId", actionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }
    
    /**
     * Fetches user details for a list of user IDs.
     * Returns a map of userId -> UserInfo for quick lookup.
     * 
     * @param userIds list of user IDs
     * @param tenantId tenant ID
     * @return map of userId to UserInfo
     */
    private Map<String, UserInfo> fetchUserDetails(List<UUID> userIds, String tenantId) {
        Map<String, UserInfo> userDetailsMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return userDetailsMap;
        }
        
        try {
            List<UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create a map for quick lookup
            Map<String, UserInfo> allUsersMap = new HashMap<>();
            for (UserInfo user : allUsers) {
                allUsersMap.put(user.getId(), user);
            }
            
            // Fetch details for requested user IDs
            for (UUID userId : userIds) {
                UserInfo userInfo = allUsersMap.get(userId.toString());
                if (userInfo != null) {
                    userDetailsMap.put(userId.toString(), userInfo);
                }
            }
            
            logger.debug()
                .message("Fetched user details for event payload")
                .detail("requestedCount", userIds.size())
                .detail("foundCount", userDetailsMap.size())
                .log();
                
        } catch (Exception e) {
            logger.warn()
                .message("Could not fetch user details, proceeding without user info")
                .detail("tenantId", tenantId)
                .detail("error", e.getMessage())
                .log();
        }
        
        return userDetailsMap;
    }
    
    /**
     * Publishes a role.created event when a new role is created.
     * 
     * @param role the created role
     * @param ownerIds list of owner user IDs
     * @param tenantId tenant identifier
     * @param createdBy username of the creator
     * @param createdById user ID of the creator
     * @param workflowId the workflow ID to execute (if specified)
     */
    public void publishRoleCreatedEvent(Role role, List<UUID> ownerIds, 
                                       String tenantId, String createdBy, 
                                       UUID createdById, String workflowId) {
        try {
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", role.getId().toString());
            payload.put("roleName", role.getRoleName());
            payload.put("roleType", role.getRoleType().name());
            payload.put("riskLevel", role.getRiskLevel().name());
            payload.put("description", role.getDescription());
            payload.put("businessJustification", role.getBusinessJustification());
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById.toString());
            payload.put("requireOwnerApproval", role.getRequireOwnerApproval());
            payload.put("tags", role.getTags());
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId); // Add tenantId to payload for workflow context
            
            // Extract JWT token from current HTTP request context
            // This allows workflows triggered by events to make authenticated HTTP calls
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7); // Remove "Bearer " prefix
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", role.getId().toString())
                            .log();
                    }
                }
            } catch (Exception e) {
                // If JWT extraction fails, log warning but continue (workflow might not need it)
                logger.warn()
                    .message("Could not extract JWT token for workflow execution")
                    .detail("roleId", role.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("role.created", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "role-creation-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event (will be automatically serialized to JSON by KafkaTemplate)
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, role.getId().toString(), event);
            
            logger.info()
                .message("Published role.created event to Kafka")
                .detail("roleId", role.getId().toString())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            // Log error but don't fail role creation
            // The role is created successfully; event publishing is best-effort
            logger.error(e)
                .message("Failed to publish role.created event")
                .detail("roleId", role.getId().toString())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a role.activated event when a role is activated after approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param tenantId tenant identifier
     * @param activatedBy username of who activated the role
     */
    public void publishRoleActivatedEvent(UUID roleId, String roleName, 
                                         String tenantId, String activatedBy) {
        try {
            // Build payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("activatedBy", activatedBy);
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("role.activated", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "role-activation-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event (will be automatically serialized to JSON by KafkaTemplate)
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published role.activated event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("tenantId", tenantId)
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish role.activated event")
                .detail("roleId", roleId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a permission.assigned event when a permission assignment requires approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param actionId the action ID
     * @param actionName the action name
     * @param justification the justification for assignment
     * @param conditionScript the conditional script (optional)
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     */
    public void publishPermissionAssignedEvent(UUID roleId, String roleName,
                                              UUID actionId, String actionName,
                                              String justification, String conditionScript,
                                              List<UUID> ownerIds, String tenantId,
                                              String workflowId, String createdBy, String createdById) {
        try {
            // Fetch page name for the action
            String pageName = fetchPageNameForAction(actionId);
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("actionId", actionId.toString());
            payload.put("actionName", actionName);
            payload.put("pageName", pageName); // Add page name to payload
            payload.put("justification", justification);
            payload.put("conditionScript", conditionScript);
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", roleId.toString())
                            .detail("actionId", actionId.toString())
                            .log();
                    }
                    
                    // Extract user info from request for userId field required by workflows-service
                    String userId = httpRequest.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token or user ID for workflow execution")
                    .detail("roleId", roleId.toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("permission.assigned", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "permission-assignment-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published permission.assigned event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("actionId", actionId.toString())
                .detail("actionName", actionName)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish permission.assigned event")
                .detail("roleId", roleId.toString())
                .detail("actionId", actionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a permission.revoked event when a permission revocation requires approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param actionId the action ID
     * @param actionName the action name
     * @param justification the justification for revocation
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting revocation
     * @param createdById user ID of the user requesting revocation
     */
    public void publishPermissionRevokedEvent(UUID roleId, String roleName,
                                             UUID actionId, String actionName,
                                             String justification, List<UUID> ownerIds,
                                             String tenantId, String workflowId,
                                             String createdBy, String createdById) {
        try {
            // Fetch page name for the action
            String pageName = fetchPageNameForAction(actionId);
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("actionId", actionId.toString());
            payload.put("actionName", actionName);
            payload.put("pageName", pageName); // Add page name to payload
            payload.put("justification", justification);
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", roleId.toString())
                            .detail("actionId", actionId.toString())
                            .log();
                    }
                    
                    // Extract user info from request for userId field required by workflows-service
                    String userId = httpRequest.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token or user ID for workflow execution")
                    .detail("roleId", roleId.toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("permission.revoked", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "permission-revocation-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published permission.revoked event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("actionId", actionId.toString())
                .detail("actionName", actionName)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish permission.revoked event")
                .detail("roleId", roleId.toString())
                .detail("actionId", actionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a role.deleted event when a role deletion is requested.
     * 
     * @param role the role to be deleted
     * @param ownerIds list of owner user IDs (for approval workflow)
     * @param tenantId tenant identifier
     * @param deletedBy username of the user requesting deletion
     * @param deletedById user ID of the user requesting deletion
     * @param workflowId the workflow ID to execute (if specified)
     */
    public void publishRoleDeletedEvent(Role role, List<UUID> ownerIds, 
                                       String tenantId, String deletedBy, 
                                       UUID deletedById, String workflowId) {
        try {
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", role.getId().toString());
            payload.put("roleName", role.getRoleName());
            payload.put("roleType", role.getRoleType().name());
            payload.put("riskLevel", role.getRiskLevel().name());
            payload.put("description", role.getDescription());
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("deletedBy", deletedBy);
            payload.put("deletedById", deletedById.toString());
            payload.put("userId", deletedById.toString()); // Add userId for workflows-service
            payload.put("requireOwnerApproval", role.getRequireOwnerApproval());
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            
            logger.info()
                .message("Building role.deleted event payload")
                .detail("payloadKeys", payload.keySet().toString())
                .detail("userId", payload.get("userId"))
                .detail("deletedById", payload.get("deletedById"))
                .log();
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", role.getId().toString())
                            .log();
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token for workflow execution")
                    .detail("roleId", role.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("role.deleted", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "role-deletion-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, role.getId().toString(), event);
            
            logger.info()
                .message("Published role.deleted event to Kafka")
                .detail("roleId", role.getId().toString())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish role.deleted event")
                .detail("roleId", role.getId().toString())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .detail("error", e.getMessage())
                .log();
        }
    }

    private void addWorkflowOriginMetadataIfPresent(Map<String, Object> metadata) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }

            HttpServletRequest httpRequest = attrs.getRequest();
            if (httpRequest == null) {
                return;
            }

            String workflowExecutionId = httpRequest.getHeader(HEADER_WORKFLOW_EXECUTION_ID);
            if (workflowExecutionId != null && !workflowExecutionId.trim().isEmpty()) {
                metadata.put(METADATA_KEY_WORKFLOW_EXECUTION_ID, workflowExecutionId.trim());
            }
        } catch (Exception e) {
            logger.warn()
                .message("Could not extract workflow execution id header for event metadata")
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a permission.bulk.assigned event when bulk permission assignments require approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param assignments list of permission assignments
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting bulk assignment
     * @param createdById user ID of the user requesting bulk assignment
     */
    public void publishBulkPermissionAssignedEvent(UUID roleId, String roleName,
                                                   List<Map<String, Object>> assignments,
                                                   List<UUID> ownerIds, String tenantId,
                                                   String workflowId, String createdBy, String createdById) {
        try {
            // Enrich each assignment with page name
            List<Map<String, Object>> enrichedAssignments = assignments.stream()
                .map(assignment -> {
                    Map<String, Object> enriched = new HashMap<>(assignment);
                    Object actionIdObj = assignment.get("actionId");
                    if (actionIdObj != null) {
                        UUID actionId = actionIdObj instanceof UUID ? 
                            (UUID) actionIdObj : UUID.fromString(actionIdObj.toString());
                        String pageName = fetchPageNameForAction(actionId);
                        enriched.put("pageName", pageName);
                    }
                    return enriched;
                })
                .collect(Collectors.toList());
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("assignments", enrichedAssignments); // Use enriched assignments
            payload.put("assignmentCount", enrichedAssignments.size());
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        payload.put("jwtToken", token);
                    }
                    
                    // Also extract X-User-Id if present
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token for bulk permission assignment event")
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event with proper structure for idempotency
            WorkflowEvent event = new WorkflowEvent("permission.bulk.assigned", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "bulk-permission-assignment-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Publish event to Kafka
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published permission.bulk.assigned event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("assignmentCount", String.valueOf(assignments.size()))
                .detail("workflowId", workflowId)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish permission.bulk.assigned event")
                .detail("roleId", roleId.toString())
                .detail("assignmentCount", String.valueOf(assignments.size()))
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a permission.bulk.revoked event when bulk permission revocations require approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param actionIds list of action IDs to revoke
     * @param justification the justification for bulk revocation
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting bulk revocation
     * @param createdById user ID of the user requesting bulk revocation
     */
    public void publishBulkPermissionRevokedEvent(UUID roleId, String roleName,
                                                  List<UUID> actionIds, String justification,
                                                  List<UUID> ownerIds, String tenantId,
                                                  String workflowId, String createdBy, String createdById) {
        try {
            // Build action items with page names
            List<Map<String, Object>> actionItems = actionIds.stream()
                .map(actionId -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("actionId", actionId.toString());
                    
                    // Fetch action details including page name
                    try {
                        Optional<Action> actionOpt = actionRepository.findById(actionId);
                        if (actionOpt.isPresent()) {
                            Action action = actionOpt.get();
                            item.put("actionName", action.getActionName());
                            
                            UUID pageId = action.getPageId();
                            if (pageId != null) {
                                Optional<Page> pageOpt = pageRepository.findById(pageId);
                                if (pageOpt.isPresent()) {
                                    item.put("pageName", pageOpt.get().getPageName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn()
                            .message("Could not fetch action details")
                            .detail("actionId", actionId.toString())
                            .detail("error", e.getMessage())
                            .log();
                    }
                    
                    return item;
                })
                .collect(Collectors.toList());
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("actionIds", actionIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("actionItems", actionItems); // Add enriched action items
            payload.put("revocationCount", actionIds.size());
            payload.put("justification", justification);
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        payload.put("jwtToken", token);
                    }
                    
                    // Also extract X-User-Id if present
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token for bulk permission revocation event")
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event with proper structure for idempotency
            WorkflowEvent event = new WorkflowEvent("permission.bulk.revoked", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "bulk-permission-revocation-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Publish event to Kafka
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published permission.bulk.revoked event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("revocationCount", String.valueOf(actionIds.size()))
                .detail("workflowId", workflowId)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish permission.bulk.revoked event")
                .detail("roleId", roleId.toString())
                .detail("revocationCount", String.valueOf(actionIds.size()))
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a user.role.assignment.pending event when user role assignment requires approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param userIds list of user IDs to assign the role to
     * @param justification the justification for assignment
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting assignment
     * @param createdById user ID of the user requesting assignment
     * @param sendNotificationOnCompletion whether to send notification on completion
     */
    public void publishUserRoleAssignmentPendingEvent(UUID roleId, String roleName,
                                                     List<UUID> userIds, String justification,
                                                     List<UUID> ownerIds, String tenantId,
                                                     String workflowId, String createdBy,
                                                     UUID createdById, boolean sendNotificationOnCompletion) {
        try {
            // Fetch user details for enrichment
            Map<String, UserInfo> userDetailsMap = fetchUserDetails(userIds, tenantId);
            
            // Build user details list with IDs, names, emails, etc.
            List<Map<String, String>> userDetails = new ArrayList<>();
            for (UUID userId : userIds) {
                Map<String, String> detail = new HashMap<>();
                detail.put("userId", userId.toString());
                
                UserInfo userInfo = userDetailsMap.get(userId.toString());
                if (userInfo != null) {
                    detail.put("userName", userInfo.getUsername());
                    detail.put("email", userInfo.getEmail());
                    detail.put("firstName", userInfo.getFirstName());
                    detail.put("lastName", userInfo.getLastName());
                    detail.put("displayName", userInfo.getDisplayName());
                } else {
                    detail.put("userName", "Unknown User");
                    detail.put("email", "");
                    detail.put("firstName", "");
                    detail.put("lastName", "");
                    detail.put("displayName", "Unknown User");
                }
                userDetails.add(detail);
            }
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("userIds", userIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("userDetails", userDetails); // NEW: user details with names
            payload.put("justification", justification != null ? justification : "User role assignment via role management");
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById.toString());
            payload.put("sendNotificationOnCompletion", sendNotificationOnCompletion);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", roleId.toString())
                            .detail("userCount", String.valueOf(userIds.size()))
                            .log();
                    }
                    
                    // Extract user info from request for userId field required by workflows-service
                    String userId = httpRequest.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token or user ID for workflow execution")
                    .detail("roleId", roleId.toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("role.assigned", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "user-role-assignment-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published role.assigned event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("userCount", String.valueOf(userIds.size()))
                .detail("workflowId", workflowId)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish role.assigned event")
                .detail("roleId", roleId.toString())
                .detail("userCount", String.valueOf(userIds.size()))
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Publishes a user.role.revocation.pending event when user role revocation requires approval.
     * 
     * @param roleId the role ID
     * @param roleName the role name
     * @param userIds list of user IDs to revoke the role from
     * @param justification the justification for revocation
     * @param ownerIds list of role owner user IDs
     * @param tenantId tenant identifier
     * @param workflowId the workflow ID to execute
     * @param createdBy username of the user requesting revocation
     * @param createdById user ID of the user requesting revocation
     * @param sendNotificationOnCompletion whether to send notification on completion
     */
    public void publishUserRoleRevocationPendingEvent(UUID roleId, String roleName,
                                                     List<UUID> userIds, String justification,
                                                     List<UUID> ownerIds, String tenantId,
                                                     String workflowId, String createdBy,
                                                     UUID createdById, boolean sendNotificationOnCompletion) {
        try {
            // Fetch user details for enrichment
            Map<String, UserInfo> userDetailsMap = fetchUserDetails(userIds, tenantId);
            
            // Build user details list with IDs, names, emails, etc.
            List<Map<String, String>> userDetails = new ArrayList<>();
            for (UUID userId : userIds) {
                Map<String, String> detail = new HashMap<>();
                detail.put("userId", userId.toString());
                
                UserInfo userInfo = userDetailsMap.get(userId.toString());
                if (userInfo != null) {
                    detail.put("userName", userInfo.getUsername());
                    detail.put("email", userInfo.getEmail());
                    detail.put("firstName", userInfo.getFirstName());
                    detail.put("lastName", userInfo.getLastName());
                    detail.put("displayName", userInfo.getDisplayName());
                } else {
                    detail.put("userName", "Unknown User");
                    detail.put("email", "");
                    detail.put("firstName", "");
                    detail.put("lastName", "");
                    detail.put("displayName", "Unknown User");
                }
                userDetails.add(detail);
            }
            
            // Build payload with all necessary context
            Map<String, Object> payload = new HashMap<>();
            payload.put("roleId", roleId.toString());
            payload.put("roleName", roleName);
            payload.put("userIds", userIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("userDetails", userDetails); // NEW: user details with names
            payload.put("justification", justification != null ? justification : "User role revocation via role management");
            payload.put("ownerIds", ownerIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
            payload.put("workflowId", workflowId);
            payload.put("tenantId", tenantId);
            payload.put("createdBy", createdBy);
            payload.put("createdById", createdById.toString());
            payload.put("sendNotificationOnCompletion", sendNotificationOnCompletion);
            
            // Extract JWT token from current HTTP request context
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        payload.put("jwtToken", jwtToken);
                        logger.debug()
                            .message("Extracted JWT token for workflow execution")
                            .detail("roleId", roleId.toString())
                            .detail("userCount", String.valueOf(userIds.size()))
                            .log();
                    }
                    
                    // Extract user info from request for userId field required by workflows-service
                    String userId = httpRequest.getHeader("X-User-Id");
                    if (userId != null) {
                        payload.put("userId", userId);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message("Could not extract JWT token or user ID for workflow execution")
                    .detail("roleId", roleId.toString())
                    .detail("error", e.getMessage())
                    .log();
            }
            
            // Build event
            WorkflowEvent event = new WorkflowEvent("role.revoked", SOURCE_SERVICE, tenantId);
            event.setPayload(payload);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "user-role-revocation-api");
            metadata.put("version", "1.0");
            addWorkflowOriginMetadataIfPresent(metadata);
            event.setMetadata(metadata);
            
            // Send event
            kafkaTemplate.send(ROLE_EVENTS_TOPIC, roleId.toString(), event);
            
            logger.info()
                .message("Published role.revoked event to Kafka")
                .detail("roleId", roleId.toString())
                .detail("roleName", roleName)
                .detail("userCount", String.valueOf(userIds.size()))
                .detail("workflowId", workflowId)
                .detail("tenantId", tenantId)
                .detail("topic", ROLE_EVENTS_TOPIC)
                .detail("eventId", event.getEventId().toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to publish role.revoked event")
                .detail("roleId", roleId.toString())
                .detail("userCount", String.valueOf(userIds.size()))
                .detail("error", e.getMessage())
                .log();
        }
    }
}
