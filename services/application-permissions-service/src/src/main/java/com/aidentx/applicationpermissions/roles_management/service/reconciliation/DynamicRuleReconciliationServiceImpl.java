package com.aidentx.applicationpermissions.roles_management.service.reconciliation;

import com.aidentx.applicationpermissions.kafka.NotificationKafkaService;
import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.config.audit.RoleAuditEvent;
import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.config.audit.RoleAuditEventPublisher;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of dynamic rule reconciliation service.
 * 
 * <p>This service handles two types of reconciliation:</p>
 * <ul>
 *   <li><strong>Event-Driven (CDC):</strong> Triggered when user data changes, evaluates only IMMEDIATE rules</li>
 *   <li><strong>Scheduled (Cron):</strong> Triggered by scheduler, evaluates DAILY/WEEKLY/MONTHLY rules</li>
 * </ul>
 * 
 * <p>The separation ensures that IMMEDIATE rules respond instantly to changes via PostgreSQL LISTEN/NOTIFY,
 * while scheduled rules continue to run at their configured times.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class DynamicRuleReconciliationServiceImpl implements DynamicRuleReconciliationService {

    private final IUnifiedLogger logger;
    private final DynamicRuleRepository dynamicRuleRepository;
    private final RoleRepository roleRepository;
    private final ConditionalRuleEvaluationService conditionalRuleEvaluationService;
    private final UserRoleServiceImpl userRoleService;
    private final UserGrpcClient userGrpcClient;
    private final ObjectMapper objectMapper;
    private final NotificationKafkaService notificationKafkaService;
    private final RoleAuditEventPublisher roleAuditEventPublisher;

    public DynamicRuleReconciliationServiceImpl(
            UnifiedLoggerFactory loggerFactory,
            DynamicRuleRepository dynamicRuleRepository,
            RoleRepository roleRepository,
            ConditionalRuleEvaluationService conditionalRuleEvaluationService,
            UserRoleServiceImpl userRoleService,
            UserGrpcClient userGrpcClient,
            ObjectMapper objectMapper,
            NotificationKafkaService notificationKafkaService,
            RoleAuditEventPublisher roleAuditEventPublisher) {
        this.logger = loggerFactory.getLogger(DynamicRuleReconciliationServiceImpl.class);
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.roleRepository = roleRepository;
        this.conditionalRuleEvaluationService = conditionalRuleEvaluationService;
        this.userRoleService = userRoleService;
        this.userGrpcClient = userGrpcClient;
        this.objectMapper = objectMapper;
        this.notificationKafkaService = notificationKafkaService;
        this.roleAuditEventPublisher = roleAuditEventPublisher;
    }

    /**
     * Thread-local context to indicate we're in CDC reconciliation execution.
     * Used to prevent duplicate audit events from the aspect.
     */
    private static final ThreadLocal<Boolean> CDC_RECONCILIATION_CONTEXT = new ThreadLocal<>();
    
    /**
     * Checks if the current thread is executing within CDC reconciliation context.
     * Used by audit aspect to skip individual assignment audits during CDC.
     * 
     * @return true if in CDC reconciliation context, false otherwise
     */
    public static boolean isInCdcReconciliationContext() {
        Boolean value = CDC_RECONCILIATION_CONTEXT.get();
        return value != null && value;
    }
    
    /**
     * Reconciles a specific user with all active IMMEDIATE dynamic rules.
     * Called by CDC listener when user data changes.
     * 
     * @param userId the ID of the user to reconcile
     * @param tenantId the tenant ID (String)
     */
    @Override
    @Transactional
    public void reconcileUserWithImmediateRules(UUID userId, String tenantId) {
        // Set context flag to prevent duplicate audit events
        CDC_RECONCILIATION_CONTEXT.set(Boolean.TRUE);
        
        try {
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_STARTED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId)
                .log();
            
            // Get all active IMMEDIATE rules
            List<DynamicRule> immediateRules = dynamicRuleRepository.findAllActiveImmediateRules();
        
        if (immediateRules.isEmpty()) {
            logger.trace()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_SKIPPING_RECONCILIATION_NO_IMMEDIATE_RULES)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .log();
            return;
        }
        
        logger.debug()
            .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
            .message(AppConstants.LOG_CDC_TRIGGERING_RECONCILIATION)
            .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
            .detail(AppConstants.DETAIL_KEY_CDC_IMMEDIATE_RULES_COUNT, String.valueOf(immediateRules.size()))
            .log();
        
        // Fetch user data from auth service via gRPC
        UserDto userData;
        try {
            userData = fetchUserDataViaGrpc(userId, tenantId);
            if (userData == null) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_RECONCILIATION_USER_NOT_FOUND)
                    .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                    .log();
                return;
            }
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .log();
            return;
        }
        
        // Track changes for notifications and audit
        List<UUID> usersAssigned = new ArrayList<>();
        List<UUID> usersUnassigned = new ArrayList<>();
        
        // Evaluate each IMMEDIATE rule for this user
        for (DynamicRule rule : immediateRules) {
            try {
                // Get role entity to check tenant
                Optional<Role> roleOpt = roleRepository.findById(rule.getRoleId());
                if (roleOpt.isEmpty()) {
                    continue;
                }
                
                Role role = roleOpt.get();
                
                // Only process rules for the same tenant
                if (!role.getTenantId().equals(tenantId)) {
                    continue;
                }
                
                boolean shouldHaveRole = conditionalRuleEvaluationService.evaluateCondition(
                    rule.getRuleConditionScript(),
                    userData
                );
                
                boolean currentlyHasRole = userHasRole(userId, rule.getRoleId(), tenantId);
                
                // Track state before reconciliation for audit
                List<UUID> usersWithRoleBefore = currentlyHasRole ? List.of(userId) : new ArrayList<>();
                
                // Assign role if condition met and user doesn't have it
                if (shouldHaveRole && !currentlyHasRole) {
                    logger.debug()
                        .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                        .message(AppConstants.LOG_CDC_ROLE_ASSIGNING)
                        .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, rule.getRoleId().toString())
                        .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, role.getRoleName())
                        .log();
                    
                    AssignRoleRequest assignRequest = new AssignRoleRequest(
                        userId,
                        null,
                        rule.getRoleId(),
                        rule.getSendNotificationOnAssignment(),
                        null
                    );
                    // Use assignRoleWithType to create DYNAMIC assignments
                    userRoleService.assignRoleWithType(
                        assignRequest, 
                        tenantId, 
                        AssignmentType.DYNAMIC
                    );
                    usersAssigned.add(userId);
                    
                    // Send notification if enabled
                    if (rule.getSendNotificationOnAssignment()) {
                        sendCdcRoleAssignmentNotification(rule.getRoleId(), userId, tenantId);
                    }
                    
                    // Publish audit event for role reconciliation
                    List<UUID> usersWithRoleAfter = List.of(userId);
                    publishCdcRoleAuditEvent(role, userId, tenantId, usersWithRoleBefore, usersWithRoleAfter, 
                        new ArrayList<>(), List.of(userId), new ArrayList<>(), 1);
                }
                // Unassign role if condition not met and user has it
                else if (!shouldHaveRole && currentlyHasRole) {
                    logger.debug()
                        .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                        .message(AppConstants.LOG_CDC_ROLE_UNASSIGNING)
                        .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, rule.getRoleId().toString())
                        .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, role.getRoleName())
                        .log();
                    
                    userRoleService.unassignRole(userId, rule.getRoleId(), tenantId);
                    usersUnassigned.add(userId);
                    
                    // Send revocation notification
                    sendCdcRoleRevocationNotification(rule.getRoleId(), userId, tenantId);
                    
                    // Publish audit event for role reconciliation
                    List<UUID> usersWithRoleAfter = new ArrayList<>();
                    publishCdcRoleAuditEvent(role, userId, tenantId, usersWithRoleBefore, usersWithRoleAfter,
                        List.of(userId), new ArrayList<>(), new ArrayList<>(), 1);
                }
                
            } catch (Exception e) {
                logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.ERROR_CDC_RECONCILIATION_EXCEPTION)
                    .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                    .detail(AppConstants.DETAIL_KEY_CDC_RULE_ID, rule.getId().toString())
                    .log();
            }
        }
        
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_RULES_EVALUATED, String.valueOf(immediateRules.size()))
                .log();
        } finally {
            CDC_RECONCILIATION_CONTEXT.remove();
        }
    }

    /**
     * Reconciles all users in a tenant with all active dynamic rules.
     * Called by scheduled reconciliation for DAILY/WEEKLY/MONTHLY rules.
     * 
     * @param tenantId the tenant ID
     */
    @Override
    @Transactional
    public void reconcileAllUsersForTenant(UUID tenantId) {
        logger.info()
            .category(AppConstants.LOG_CATEGORY_RECONCILIATION)
            .message(AppConstants.LOG_RECONCILIATION_ALL_USERS_STARTED)
            .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
            .log();
        
        // Get all active scheduled rules (DAILY, WEEKLY, MONTHLY)
        List<DynamicRule> scheduledRules = dynamicRuleRepository.findAllActiveScheduledRules();
        
        if (scheduledRules.isEmpty()) {
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_RECONCILIATION)
                .message(AppConstants.LOG_RECONCILIATION_NO_SCHEDULED_RULES)
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
                .log();
            return;
        }
        
        // Filter by tenant
        List<DynamicRule> tenantRules = scheduledRules.stream()
            .filter(rule -> {
                Optional<Role> roleOpt = roleRepository.findById(rule.getRoleId());
                return roleOpt.isPresent() && roleOpt.get().getTenantId().equals(tenantId);
            })
            .toList();
        
        logger.info()
            .category(AppConstants.LOG_CATEGORY_RECONCILIATION)
            .message(AppConstants.LOG_RECONCILIATION_FOUND_SCHEDULED_RULES)
            .detail(AppConstants.DETAIL_KEY_CDC_RULES_COUNT, String.valueOf(tenantRules.size()))
            .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
            .log();
        
        // Execute each rule (existing logic from DynamicRuleService)
        for (DynamicRule rule : tenantRules) {
            try {
                Optional<Role> roleOpt = roleRepository.findById(rule.getRoleId());
                if (roleOpt.isPresent()) {
                    logger.debug()
                        .category(AppConstants.LOG_CATEGORY_RECONCILIATION)
                        .message(AppConstants.LOG_RECONCILIATION_EXECUTING_SCHEDULED_RULE)
                        .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, roleOpt.get().getRoleName())
                        .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
                        .log();
                }
                // This would call the existing executeDynamicRuleWithTenant method
                // Implementation depends on how scheduled execution is currently handled
            } catch (Exception e) {
                logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_RECONCILIATION)
                    .message(AppConstants.ERROR_RECONCILIATION_SCHEDULED_RULE_FAILED)
                    .detail(AppConstants.DETAIL_KEY_CDC_RULE_ID, rule.getId().toString())
                    .log();
            }
        }
    }

    /**
     * Fetches user data from auth service via gRPC.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return UserDto containing user data, or null if user not found
     */
    private UserDto fetchUserDataViaGrpc(UUID userId, String tenantId) {
        try {
            // Fetch all users in tenant via gRPC
            List<UserInfo> users = userGrpcClient.getUserListByTenant(tenantId);
            
            // Find the specific user
            Optional<UserInfo> userOpt = users.stream()
                .filter(u -> u.getId().equals(userId.toString()))
                .findFirst();
            
            if (userOpt.isEmpty()) {
                return null;
            }
            
            // Convert UserInfo to UserDto
            return convertToUserDto(userOpt.get());
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.ERROR_CDC_FETCH_USER_DATA_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
                .log();
            return null;
        }
    }
    
    /**
     * Converts gRPC UserInfo to UserDto.
     * 
     * @param userInfo the gRPC UserInfo object
     * @return UserDto
     */
    private UserDto convertToUserDto(UserInfo userInfo) {
        return new UserDto(
            userInfo.getId(),
            userInfo.getUsername(),
            userInfo.getEmail(),
            userInfo.getFirstName(),
            userInfo.getLastName(),
            userInfo.getDisplayName(),
            userInfo.getStatus(),
            userInfo.getTenantId(),
            userInfo.getLastLoginAt(),
            userInfo.getPasswordChangedAt(),
            userInfo.getFailedLoginAttempts(),
            userInfo.getLockedUntil(),
            userInfo.getEmailVerifiedAt(),
            userInfo.getMfaEnabled(),
            userInfo.getIsPasswordExpired(),
            userInfo.getProfileData(),
            userInfo.getCreatedAt(),
            userInfo.getUpdatedAt(),
            userInfo.getCreatedBy(),
            userInfo.getUpdatedBy()
        );
    }

    /**
     * Checks if a user currently has a specific role.
     * 
     * @param userId the user ID
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @return true if user has the role, false otherwise
     */
    private boolean userHasRole(UUID userId, UUID roleId, String tenantId) {
        try {
            // Use UserRoleService to check if user has role
            return userRoleService.hasRole(userId, roleId, tenantId);
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.ERROR_CDC_USER_ROLE_CHECK_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, roleId.toString())
                .log();
            return false;
        }
    }

    /**
     * Sends notification for CDC-triggered role assignment.
     * 
     * @param roleId the role ID
     * @param userId the user ID
     * @param tenantId the tenant ID
     */
    private void sendCdcRoleAssignmentNotification(UUID roleId, UUID userId, String tenantId) {
        try {
            // Fetch users from gRPC for notification data
            List<UserInfo> users = userGrpcClient.getUserListByTenant(tenantId);
            Optional<UserInfo> userOpt = users.stream()
                .filter(u -> u.getId().equals(userId.toString()))
                .findFirst();
            
            if (userOpt.isEmpty()) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_USER_NOT_FOUND_FOR_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                    .log();
                return;
            }
            
            UserInfo userInfo = userOpt.get();
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_ROLE_NOT_FOUND_FOR_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, roleId.toString())
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_PREPARING_NOTIFICATION)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, roleName)
                .log();
            
            // Create notification data
            com.aidentx.applicationpermissions.kafka.NotificationEventData.UserData userData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.UserData(
                    userInfo.getEmail(), 
                    userInfo.getUsername()
                );
            
            com.aidentx.applicationpermissions.kafka.NotificationEventData.TemplateVariables templateVariables = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.TemplateVariables(
                    userInfo.getUsername(),
                    userInfo.getEmail(),
                    roleName
                );
            
            com.aidentx.applicationpermissions.kafka.NotificationEventData.UserNotificationData userNotificationData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.UserNotificationData(
                    userId.toString(),
                    userData,
                    templateVariables
                );
            
            // Create and send notification event
            com.aidentx.applicationpermissions.kafka.NotificationEventData eventData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData(
                    com.aidentx.applicationpermissions.kafka.NotificationEventData.OperationType.ASSIGN_INTERNAL_ROLE,
                    tenantId,
                    List.of(userNotificationData)
                );
            
            notificationKafkaService.sendNotificationEvent(eventData, tenantId);
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_NOTIFICATION_SENT)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, roleName)
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.ERROR_CDC_NOTIFICATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .log();
        }
    }

    /**
     * Publishes audit event for CDC-triggered role reconciliation.
     * 
     * @param role the role entity
     * @param userId the user ID that triggered the CDC
     * @param tenantId the tenant ID
     * @param usersWithRoleBefore users who had the role before reconciliation
     * @param usersWithRoleAfter users who have the role after reconciliation
     * @param usersRemoved users whose role was removed
     * @param usersAdded users who got the role added
     * @param usersKept users who kept the role
     * @param successfulChanges number of successful changes (1 for CDC single user)
     */
    private void publishCdcRoleAuditEvent(
            Role role, 
            UUID userId, 
            String tenantId,
            List<UUID> usersWithRoleBefore,
            List<UUID> usersWithRoleAfter,
            List<UUID> usersRemoved,
            List<UUID> usersAdded,
            List<UUID> usersKept,
            int successfulChanges) {
        try {
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_AUDIT_EVENT_PUBLISHING)
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, role.getId().toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, role.getRoleName())
                .detail("usersBefore", usersWithRoleBefore.size())
                .detail("usersAfter", usersWithRoleAfter.size())
                .log();
            
            // Fetch user details for proper audit trail
            List<com.aidentx.applicationpermissions.shared.grpc.dto.UserDto> tenantUsers = new ArrayList<>();
            try {
                List<UserInfo> grpcUsers = userGrpcClient.getUserListByTenant(tenantId);
                UserInfo userInfo = grpcUsers.stream()
                    .filter(u -> u.getId().equals(userId.toString()))
                    .findFirst()
                    .orElse(null);
                
                if (userInfo != null) {
                    com.aidentx.applicationpermissions.shared.grpc.dto.UserDto userDto = 
                        new com.aidentx.applicationpermissions.shared.grpc.dto.UserDto();
                    userDto.setId(userInfo.getId());
                    userDto.setUsername(userInfo.getUsername());
                    tenantUsers.add(userDto);
                }
            } catch (Exception e) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_AUDIT_FETCH_USER_FALLBACK)
                    .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                    .log();
            }
            
            // Create user lookup map
            java.util.Map<String, String> userIdToNameMap = new java.util.HashMap<>();
            for (com.aidentx.applicationpermissions.shared.grpc.dto.UserDto user : tenantUsers) {
                userIdToNameMap.put(user.getId(), user.getUsername());
            }
            
            // Build stateBefore - users who had the role before reconciliation
            java.util.Map<String, Object> stateBefore = new java.util.HashMap<>();
            List<java.util.Map<String, String>> usersBeforeList = new ArrayList<>();
            for (UUID uid : usersWithRoleBefore) {
                java.util.Map<String, String> userInfo = new java.util.HashMap<>();
                userInfo.put("userId", uid.toString());
                userInfo.put("userName", userIdToNameMap.getOrDefault(uid.toString(), "unknown"));
                usersBeforeList.add(userInfo);
            }
            
            stateBefore.put("users", usersBeforeList);
            stateBefore.put("roleId", role.getId().toString());
            stateBefore.put("roleName", role.getRoleName());
            stateBefore.put("userCount", usersWithRoleBefore.size());
            
            // Build stateAfter - users who have the role after reconciliation
            java.util.Map<String, Object> stateAfter = new java.util.HashMap<>();
            List<java.util.Map<String, String>> usersAfterList = new ArrayList<>();
            for (UUID uid : usersWithRoleAfter) {
                java.util.Map<String, String> userInfo = new java.util.HashMap<>();
                userInfo.put("userId", uid.toString());
                userInfo.put("userName", userIdToNameMap.getOrDefault(uid.toString(), "unknown"));
                usersAfterList.add(userInfo);
            }
            
            stateAfter.put("users", usersAfterList);
            stateAfter.put("roleId", role.getId().toString());
            stateAfter.put("roleName", role.getRoleName());
            stateAfter.put("userCount", usersWithRoleAfter.size());
            
            // Add reconciliation summary to stateAfter
            java.util.Map<String, Object> reconciliationSummary = new java.util.HashMap<>();
            reconciliationSummary.put("usersAdded", usersAdded.size());
            reconciliationSummary.put("usersRemoved", usersRemoved.size());
            reconciliationSummary.put("usersKept", usersKept.size());
            reconciliationSummary.put("successfulAdditions", usersAdded.size());
            reconciliationSummary.put("successfulRemovals", usersRemoved.size());
            stateAfter.put("reconciliationSummary", reconciliationSummary);
            
            RoleAuditEvent auditEvent = RoleAuditEvent.create(
                AppConstants.SYSTEM_USER_ID,
                AppConstants.SYSTEM_USER_NAME,
                AppConstants.UNKNOWN_IP_ADDRESS,
                AppConstants.ACTION_ROLE_RECONCILIATION,
                AppConstants.ENTITY_TYPE_ROLE,
                role.getId().toString(),
                role.getRoleName(),
                tenantId,
                objectMapper.valueToTree(stateBefore),
                objectMapper.valueToTree(stateAfter),
                role.getRiskLevel().name().toLowerCase()
            );
            
            roleAuditEventPublisher.publishEvent(auditEvent);
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_AUDIT_EVENT_PUBLISHED)
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, role.getId().toString())
                .detail("action", AppConstants.ACTION_ROLE_RECONCILIATION)
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.ERROR_CDC_AUDIT_EVENT_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, role.getId().toString())
                .log();
        }
    }
    
    /**
     * Sends role revocation notification via Kafka for CDC-triggered revocations.
     * 
     * @param roleId the role ID that was revoked
     * @param userId the user ID who had the role revoked
     * @param tenantId the tenant ID
     */
    private void sendCdcRoleRevocationNotification(UUID roleId, UUID userId, String tenantId) {
        try {
            // Fetch users from gRPC for notification data
            List<UserInfo> users = userGrpcClient.getUserListByTenant(tenantId);
            Optional<UserInfo> userOpt = users.stream()
                .filter(u -> u.getId().equals(userId.toString()))
                .findFirst();
            
            if (userOpt.isEmpty()) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_USER_NOT_FOUND_FOR_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                    .log();
                return;
            }
            
            UserInfo userInfo = userOpt.get();
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                    .message(AppConstants.LOG_CDC_ROLE_NOT_FOUND_FOR_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, roleId.toString())
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message("Preparing CDC role revocation notification")
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, roleName)
                .log();
            
            // Create notification data
            com.aidentx.applicationpermissions.kafka.NotificationEventData.UserData userData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.UserData(
                    userInfo.getEmail(), 
                    userInfo.getUsername()
                );
            
            com.aidentx.applicationpermissions.kafka.NotificationEventData.TemplateVariables templateVariables = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.TemplateVariables(
                    userInfo.getUsername(),
                    userInfo.getEmail(),
                    roleName
                );
            
            com.aidentx.applicationpermissions.kafka.NotificationEventData.UserNotificationData userNotificationData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData.UserNotificationData(
                    userId.toString(),
                    userData,
                    templateVariables
                );
            
            // Create and send notification event
            com.aidentx.applicationpermissions.kafka.NotificationEventData eventData = 
                new com.aidentx.applicationpermissions.kafka.NotificationEventData(
                    com.aidentx.applicationpermissions.kafka.NotificationEventData.OperationType.REVOKE_INTERNAL_ROLE,
                    tenantId,
                    List.of(userNotificationData)
                );
            
            notificationKafkaService.sendNotificationEvent(eventData, tenantId);
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message("CDC role revocation notification sent successfully")
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_ROLE_NAME, roleName)
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message("Failed to send CDC role revocation notification")
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .log();
        }
    }
}
