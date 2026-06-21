package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkUserRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.DynamicRuleExecutionResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.DynamicRuleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.roles_management.service.DynamicRuleService;
import com.aidentx.applicationpermissions.roles_management.service.UserRoleService;
import com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl;
import com.aidentx.applicationpermissions.roles_management.config.audit.RoleAuditEvent;
import com.aidentx.applicationpermissions.roles_management.config.audit.RoleAuditEventPublisher;
import com.aidentx.applicationpermissions.roles_management.config.audit.RoleAuditAspect;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.applicationpermissions.shared.security.JwtUtil;
import com.aidentx.applicationpermissions.kafka.NotificationKafkaService;
import com.aidentx.applicationpermissions.kafka.NotificationEventData;
import com.aidentx.auth.grpc.GetUsersByTenantResponse;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of DynamicRuleService for managing dynamic rules.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class DynamicRuleServiceImpl implements DynamicRuleService {
    
    // CRITICAL: Per-rule lock map to prevent concurrent execution of the same rule
    private final ConcurrentHashMap<UUID, Object> ruleLocks = new ConcurrentHashMap<>();
    
    private final DynamicRuleRepository dynamicRuleRepository;
    private final RoleRepository roleRepository;
    private final TenantContextService tenantContextService;
    private final UserGrpcClient userGrpcClient;
    private final ConditionalRuleEvaluationService conditionalRuleEvaluationService;
    private final UserRoleService userRoleService;
    private final JwtUtil jwtUtil;
    private final RoleAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;
    private final NotificationKafkaService notificationKafkaService;
    private final IUnifiedLogger logger;
    
    @Autowired
    public DynamicRuleServiceImpl(DynamicRuleRepository dynamicRuleRepository,
                                 RoleRepository roleRepository,
                                 TenantContextService tenantContextService,
                                 UserGrpcClient userGrpcClient,
                                 ConditionalRuleEvaluationService conditionalRuleEvaluationService,
                                 UserRoleService userRoleService,
                                 JwtUtil jwtUtil,
                                 RoleAuditEventPublisher auditEventPublisher,
                                 ObjectMapper objectMapper,
                                 NotificationKafkaService notificationKafkaService,
                                 UnifiedLoggerFactory loggerFactory) {
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.roleRepository = roleRepository;
        this.tenantContextService = tenantContextService;
        this.userGrpcClient = userGrpcClient;
        this.conditionalRuleEvaluationService = conditionalRuleEvaluationService;
        this.userRoleService = userRoleService;
        this.jwtUtil = jwtUtil;
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
        this.notificationKafkaService = notificationKafkaService;
        this.logger = loggerFactory.getLogger("DynamicRuleService", "DYNAMIC_RULE_MANAGEMENT");
    }
    
    @Override
    public DynamicRuleResponse createDynamicRule(CreateDynamicRuleRequest request) {
        logger.debug()
            .message(AppConstants.LOG_DEBUG_STARTING_DYNAMIC_RULE_CREATION_SERVICE)
            .detail("roleId", request.roleId().toString())
            .detail("scheduleType", request.scheduleType().name())
            .log();
            
        String tenantId = tenantContextService.getCurrentTenantId();
        
        // Validate that the role exists and belongs to the current tenant
        Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
            .orElseThrow(() -> {
                logger.error()
                    .message(AppConstants.LOG_ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT)
                    .detail("roleId", request.roleId().toString())
                    .detail("tenantId", tenantId)
                    .log();
                return new IllegalArgumentException(AppConstants.ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT);
            });
        
        // Validate that no dynamic rule already exists for this role
        logger.debug()
            .message(AppConstants.LOG_CHECKING_EXISTING_DYNAMIC_RULE)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .log();
            
        if (dynamicRuleRepository.existsByRoleId(request.roleId())) {
            logger.error()
                .message(AppConstants.LOG_DYNAMIC_RULE_EXISTS_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .log();
            throw new IllegalArgumentException(AppConstants.EXCEPTION_DYNAMIC_RULE_EXISTS + request.roleId() + AppConstants.EXCEPTION_EACH_ROLE_ONE_RULE);
        }
        
        logger.debug()
            .message(AppConstants.LOG_NO_EXISTING_DYNAMIC_RULE_FOUND)
            .detail("roleId", request.roleId().toString())
            .log();
        
        // Create the dynamic rule entity
        DynamicRule dynamicRule = new DynamicRule(
            request.roleId(),
            request.ruleIsActive(),
            request.ruleConditionScript(),
            request.sendNotificationOnAssignment(),
            request.requireApprovalBeforeAssignment(),
            request.scheduleType(),
            request.time()
        );
        
        // Set audit fields using JWT extraction
        setAuditFieldsForCreate(dynamicRule);
        
        // Save the dynamic rule
        DynamicRule savedRule = dynamicRuleRepository.save(dynamicRule);
        
        logger.info()
            .message(AppConstants.LOG_INFO_DYNAMIC_RULE_CREATED_SUCCESSFULLY_SERVICE)
            .detail("dynamicRuleId", savedRule.getId().toString())
            .detail("roleId", savedRule.getRoleId().toString())
            .detail("tenantId", tenantId)
            .log();
        
        // Convert to response DTO
        DynamicRuleResponse response = new DynamicRuleResponse(
            savedRule.getId(),
            savedRule.getRoleId(),
            role.getRoleName(), 
            savedRule.getRuleIsActive(),
            savedRule.getRuleConditionScript(),
            savedRule.getSendNotificationOnAssignment(),
            savedRule.getRequireApprovalBeforeAssignment(),
            savedRule.getScheduleType(),
            savedRule.getTime(),
            savedRule.getCreatedAt(),
            savedRule.getUpdatedAt(),
            savedRule.getCreatedById(),
            savedRule.getCreatedByName(),
            savedRule.getUpdatedById(),
            savedRule.getUpdatedByName()
        );
        
        // Execute the rule immediately if schedule type is IMMEDIATE
        if (savedRule.getScheduleType() == ScheduleType.IMMEDIATE && savedRule.getRuleIsActive()) {
            logger.info()
                .message(AppConstants.LOG_EXECUTING_DYNAMIC_RULE_IMMEDIATELY_WITH_RECONCILIATION)
                .detail("dynamicRuleId", savedRule.getId().toString())
                .detail("roleId", savedRule.getRoleId().toString())
                .detail("conditionScript", savedRule.getRuleConditionScript())
                .log();
            
            try {
                // For new rules, there are no existing assignments, so this is just normal assignment
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    savedRule.getRoleId(),
                    savedRule.getRuleConditionScript(),
                    null, // No old condition script for new rules
                    tenantId,
                    savedRule.getSendNotificationOnAssignment()
                );
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_FOR_NEW_RULE)
                    .detail("dynamicRuleId", savedRule.getId().toString())
                    .detail("usersAdded", reconciliationResult.usersAdded().size())
                    .detail("successfulAdditions", reconciliationResult.successfulAdditions())
                    .detail("failedAdditions", reconciliationResult.failedAdditions())
                    .detail("executionTimeMs", reconciliationResult.executionTimeMs())
                    .log();
                    
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_IMMEDIATELY)
                    .detail("dynamicRuleId", savedRule.getId().toString())
                    .detail("errorMessage", e.getMessage())
                    .log();
            }
        }
        
        return response;
    }
    
    @Override
    public DynamicRuleExecutionResult executeDynamicRule(UUID dynamicRuleId) {
        long startTime = System.currentTimeMillis();
        
        logger.info()
            .message(AppConstants.LOG_STARTING_DYNAMIC_RULE_EXECUTION)
            .detail("dynamicRuleId", dynamicRuleId.toString())
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // 1. Retrieve the dynamic rule
            logger.debug()
                .message(AppConstants.LOG_RETRIEVING_DYNAMIC_RULE)
                .detail("dynamicRuleId", dynamicRuleId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            Optional<DynamicRule> dynamicRuleOpt = dynamicRuleRepository.findById(dynamicRuleId);
            if (dynamicRuleOpt.isEmpty()) {
                logger.error()
                    .message(AppConstants.ERROR_DYNAMIC_RULE_NOT_FOUND)
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .detail("tenantId", tenantId)
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_DYNAMIC_RULE_NOT_FOUND);
            }
            
            DynamicRule dynamicRule = dynamicRuleOpt.get();
            
            logger.debug()
                .message(AppConstants.LOG_DYNAMIC_RULE_FOUND)
                .detail("dynamicRuleId", dynamicRule.getId().toString())
                .detail("roleId", dynamicRule.getRoleId().toString())
                .detail("conditionScript", dynamicRule.getRuleConditionScript())
                .detail("isActive", String.valueOf(dynamicRule.getRuleIsActive()))
                .log();
            
            // Validate that the role exists and belongs to the current tenant
            Optional<Role> roleOpt = roleRepository.findById(dynamicRule.getRoleId());
            if (roleOpt.isEmpty() || !roleOpt.get().getTenantId().equals(tenantId)) {
                logger.error()
                    .message(AppConstants.LOG_ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT)
                    .detail("roleId", dynamicRule.getRoleId().toString())
                    .detail("tenantId", tenantId)
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT);
            }
            
            Role role = roleOpt.get();
            
            // Check if role is active - only active roles can be assigned by dynamic rules
            if (role.getIsActive() == null || !role.getIsActive()) {
                logger.error()
                    .message(AppConstants.LOG_CANNOT_EXECUTE_DYNAMIC_RULE_ROLE_INACTIVE)
                    .detail("roleId", role.getId().toString())
                    .detail("roleName", role.getRoleName())
                    .detail("isActive", String.valueOf(role.getIsActive()))
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_CANNOT_ASSIGN_INACTIVE_ROLE + role.getRoleName());
            }
            
            // 2. Fetch all users in the tenant via gRPC
            logger.debug()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_FOR_DYNAMIC_RULE)
                .detail("tenantId", tenantId)
                .log();
            
            GetUsersByTenantResponse usersResponse;
            try {
                usersResponse = userGrpcClient.getUsersByTenant(tenantId);
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.ERROR_FAILED_TO_FETCH_TENANT_USERS)
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .log();
                throw new RuntimeException(AppConstants.ERROR_FAILED_TO_FETCH_TENANT_USERS, e);
            }
            
            List<UserDto> tenantUsers = usersResponse.getUsersList().stream()
                .map(this::convertToUserDto)
                .toList();
            logger.info()
                .message(AppConstants.LOG_EVALUATING_CONDITION_SCRIPT_FOR_USERS)
                .detail("totalUsers", String.valueOf(tenantUsers.size()))
                .detail("conditionScript", dynamicRule.getRuleConditionScript())
                .log();
            
            // 3. Evaluate condition script for each user and assign roles
            int totalUsersEvaluated = tenantUsers.size();
            int usersMatchingCondition = 0;
            int successfulAssignments = 0;
            int failedAssignments = 0;
            int skippedAssignments = 0;
            List<UUID> assignedUserIds = new ArrayList<>();
            
            for (UserDto user : tenantUsers) {
                try {
                    boolean conditionMet = conditionalRuleEvaluationService.evaluateCondition(
                        dynamicRule.getRuleConditionScript(), user);
                    
                    if (conditionMet) {
                        usersMatchingCondition++;
                        
                        logger.debug()
                            .message(AppConstants.LOG_USER_MATCHES_CONDITION)
                            .detail("userId", user.getId())
                            .detail("username", user.getUsername())
                            .detail("conditionScript", dynamicRule.getRuleConditionScript())
                            .log();
                        
                        // Check if user already has the role
                        boolean alreadyHasRole = userRoleService.hasRole(UUID.fromString(user.getId()), dynamicRule.getRoleId());
                        
                        if (alreadyHasRole) {
                            skippedAssignments++;
                            logger.debug()
                                .message(AppConstants.LOG_ROLE_ASSIGNMENT_SKIPPED_ALREADY_HAS_ROLE)
                                .detail("userId", user.getId())
                                .detail("roleId", dynamicRule.getRoleId().toString())
                                .log();
                        } else {
                            logger.debug()
                                .message(AppConstants.LOG_ATTEMPTING_ROLE_ASSIGNMENT)
                                .detail("userId", user.getId())
                                .detail("roleId", dynamicRule.getRoleId().toString())
                                .log();
                            
                            try {
                                // Assign the role to the user with DYNAMIC assignment type
                                AssignRoleRequest assignRequest = new AssignRoleRequest(
                                    UUID.fromString(user.getId()),
                                    null,
                                    dynamicRule.getRoleId(),
                                    dynamicRule.getSendNotificationOnAssignment(),  // Use dynamic rule's notification setting
                                    null  // requireApprovalBeforeAssignment not used
                                );
                                userRoleService.assignRoleWithType(
                                    assignRequest, 
                                    tenantId, 
                                    AssignmentType.DYNAMIC
                                );
                                
                                successfulAssignments++;
                                assignedUserIds.add(UUID.fromString(user.getId()));
                                
                                logger.info()
                                    .message(AppConstants.LOG_ROLE_ASSIGNMENT_SUCCESSFUL)
                                    .detail("userId", user.getId())
                                    .detail("username", user.getUsername())
                                    .detail("roleId", dynamicRule.getRoleId().toString())
                                    .log();
                                
                            } catch (Exception e) {
                                failedAssignments++;
                                logger.error(e)
                                    .message(AppConstants.LOG_ROLE_ASSIGNMENT_FAILED)
                                    .detail("userId", user.getId())
                                    .detail("roleId", dynamicRule.getRoleId().toString())
                                    .detail("errorMessage", e.getMessage())
                                    .log();
                            }
                        }
                    } else {
                        logger.trace()
                            .message(AppConstants.LOG_USER_DOES_NOT_MATCH_CONDITION)
                            .detail("userId", user.getId())
                            .detail("username", user.getUsername())
                            .detail("conditionScript", dynamicRule.getRuleConditionScript())
                            .log();
                    }
                    
                } catch (Exception e) {
                    logger.error(e)
                        .message(AppConstants.LOG_ERROR_EVALUATING_CONDITION_FOR_USER)
                        .detail("userId", user.getId())
                        .detail("errorMessage", e.getMessage())
                        .log();
                }
            }
            
            // Send notifications for successful assignments if enabled
            if (!assignedUserIds.isEmpty() && dynamicRule.getSendNotificationOnAssignment()) {
                try {
                    logger.debug()
                        .message(AppConstants.LOG_SENDING_DYNAMIC_RULE_NOTIFICATIONS_IMMEDIATE)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, dynamicRule.getId().toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, dynamicRule.getRoleId().toString())
                        .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT, assignedUserIds.size())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .log();
                        
                    sendDynamicRuleAssignmentNotifications(dynamicRule.getRoleId(), assignedUserIds, tenantId);
                    
                } catch (Exception notificationException) {
                    // Log notification failure but don't fail the main operation
                    logger.warn()
                        .message(AppConstants.LOG_FAILED_SEND_DYNAMIC_RULE_NOTIFICATIONS_IMMEDIATE)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, dynamicRule.getId().toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, dynamicRule.getRoleId().toString())
                        .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT, assignedUserIds.size())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ERROR, notificationException.getMessage())
                        .log();
                }
            } else if (!assignedUserIds.isEmpty() && !dynamicRule.getSendNotificationOnAssignment()) {
                logger.debug()
                    .message(AppConstants.LOG_NOTIFICATIONS_DISABLED_DYNAMIC_RULE)
                    .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, dynamicRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, dynamicRule.getRoleId().toString())
                    .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT, assignedUserIds.size())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .log();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            DynamicRuleExecutionResult result = new DynamicRuleExecutionResult(
                dynamicRule.getId(),
                dynamicRule.getRoleId(),
                totalUsersEvaluated,
                usersMatchingCondition,
                successfulAssignments,
                failedAssignments,
                skippedAssignments,
                assignedUserIds,
                executionTime
            );
            
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_EXECUTION_COMPLETED)
                .detail("dynamicRuleId", dynamicRule.getId().toString())
                .detail("roleId", dynamicRule.getRoleId().toString())
                .detail("totalUsersEvaluated", String.valueOf(totalUsersEvaluated))
                .detail("usersMatchingCondition", String.valueOf(usersMatchingCondition))
                .detail("successfulAssignments", String.valueOf(successfulAssignments))
                .detail("failedAssignments", String.valueOf(failedAssignments))
                .detail("skippedAssignments", String.valueOf(skippedAssignments))
                .detail("executionTimeMs", String.valueOf(executionTime))
                .log();
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.error(e)
                .message(AppConstants.LOG_DYNAMIC_RULE_EXECUTION_FAILED)
                .detail("dynamicRuleId", dynamicRuleId.toString())
                .detail("errorMessage", e.getMessage())
                .detail("executionTimeMs", String.valueOf(executionTime))
                .log();
            
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_EXECUTE_DYNAMIC_RULE, e);
        }
    }

    /**
     * Executes a dynamic rule with explicit tenant context for scheduled executions.
     * This method bypasses the HTTP request context and uses the provided tenant ID.
     * 
     * @param dynamicRuleId the ID of the dynamic rule to execute
     * @param tenantId the tenant ID to execute the rule for
     * @return execution result containing assignment statistics
     */
    @Override
    public DynamicRuleExecutionResult executeDynamicRuleWithTenant(UUID dynamicRuleId, String tenantId) {
        // Get or create a lock for this specific rule to prevent concurrent execution
        Object lock = ruleLocks.computeIfAbsent(dynamicRuleId, k -> new Object());
        
        synchronized (lock) {
            long startTime = System.currentTimeMillis();
            
            logger.info()
                .message(AppConstants.LOG_STARTING_SCHEDULED_DYNAMIC_RULE_EXECUTION_WITH_TENANT)
                .detail("dynamicRuleId", dynamicRuleId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            try {
            // 1. Retrieve the dynamic rule
            Optional<DynamicRule> dynamicRuleOpt = dynamicRuleRepository.findById(dynamicRuleId);
            if (dynamicRuleOpt.isEmpty()) {
                throw new IllegalArgumentException(AppConstants.ERROR_DYNAMIC_RULE_NOT_FOUND_WITH_ID + dynamicRuleId);
            }
            
            DynamicRule dynamicRule = dynamicRuleOpt.get();
            
            // 2. Retrieve and validate the role with tenant context
            Optional<Role> roleOpt = roleRepository.findById(dynamicRule.getRoleId());
            if (roleOpt.isEmpty()) {
                throw new IllegalArgumentException(AppConstants.ERROR_ROLE_NOT_FOUND_WITH_ID + dynamicRule.getRoleId());
            }
            
            Role role = roleOpt.get();
            
            // 3. Validate tenant access
            if (!tenantId.equals(role.getTenantId())) {
                throw new IllegalArgumentException(AppConstants.ERROR_ROLE_DOES_NOT_BELONG_TO_TENANT + tenantId);
            }
            
            // 4. Check if role is active - only active roles can be assigned by dynamic rules
            if (role.getIsActive() == null || !role.getIsActive()) {
                logger.error()
                    .message(AppConstants.LOG_CANNOT_EXECUTE_SCHEDULED_DYNAMIC_RULE_ROLE_INACTIVE)
                    .detail("roleId", role.getId().toString())
                    .detail("roleName", role.getRoleName())
                    .detail("isActive", String.valueOf(role.getIsActive()))
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_CANNOT_ASSIGN_INACTIVE_ROLE_VIA_SCHEDULED_EXECUTION + role.getRoleName());
            }
            
            // 5. Fetch tenant users using gRPC with explicit tenant ID
            List<UserDto> tenantUsers = fetchTenantUsersWithTenantId(tenantId);
            int totalUsersEvaluated = tenantUsers.size();
            
            if (tenantUsers.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_NO_USERS_FOUND_FOR_TENANT_DURING_SCHEDULED_EXECUTION)
                    .detail("tenantId", tenantId)
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .log();
                
                return new DynamicRuleExecutionResult(
                    dynamicRuleId, role.getId(), tenantUsers.size(), 0, 0, 0, 0, 
                    new ArrayList<>(), System.currentTimeMillis() - startTime
                );
            }
            
            // 5. Use reconciliation logic for scheduled execution to handle any changes
            logger.info()
                .message(AppConstants.LOG_EXECUTING_SCHEDULED_DYNAMIC_RULE_WITH_RECONCILIATION)
                .detail("dynamicRuleId", dynamicRuleId.toString())
                .detail("tenantId", tenantId)
                .detail("conditionScript", dynamicRule.getRuleConditionScript())
                .log();
            
            try {
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    role.getId(),
                    dynamicRule.getRuleConditionScript(),
                    dynamicRule.getRuleConditionScript(), 
                    tenantId,
                    dynamicRule.getSendNotificationOnAssignment()
                );
                
                // Convert reconciliation result to DynamicRuleExecutionResult format
                DynamicRuleExecutionResult result = new DynamicRuleExecutionResult(
                    dynamicRuleId,
                    role.getId(),
                    totalUsersEvaluated,
                    reconciliationResult.usersWithRoleAfter().size(),
                    reconciliationResult.successfulAdditions(),
                    reconciliationResult.failedAdditions() + reconciliationResult.failedRemovals(),
                    0, // No skipped assignments in reconciliation
                    reconciliationResult.usersAdded(),
                    reconciliationResult.executionTimeMs()
                );
                
                logger.info()
                    .message(AppConstants.LOG_SCHEDULED_DYNAMIC_RULE_RECONCILIATION_COMPLETED)
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("totalUsersEvaluated", totalUsersEvaluated)
                    .detail("usersRemoved", reconciliationResult.usersRemoved().size())
                    .detail("usersAdded", reconciliationResult.usersAdded().size())
                    .detail("usersKept", reconciliationResult.usersKept().size())
                    .detail("successfulRemovals", reconciliationResult.successfulRemovals())
                    .detail("successfulAdditions", reconciliationResult.successfulAdditions())
                    .detail("executionTimeMs", reconciliationResult.executionTimeMs())
                    .log();
                
                return result;
                
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE_RECONCILIATION)
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();
                
                throw new RuntimeException(AppConstants.ERROR_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE_RECONCILIATION, e);
            }
            
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                
                logger.error(e)
                    .message(AppConstants.LOG_SCHEDULED_DYNAMIC_RULE_EXECUTION_FAILED)
                    .detail("dynamicRuleId", dynamicRuleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .detail("executionTimeMs", String.valueOf(executionTime))
                    .log();
                
                throw new RuntimeException(AppConstants.ERROR_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE, e);
            }
        } // End synchronized block
    }

    /**
     * Fetch tenant users with explicit tenant ID for scheduled executions.
     */
    private List<UserDto> fetchTenantUsersWithTenantId(String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_FOR_SCHEDULED_EXECUTION)
                .detail("tenantId", tenantId)
                .log();
            
            List<UserInfo> userInfoList = userGrpcClient.getUserListByTenant(tenantId);
            
            List<UserDto> users = userInfoList.stream()
                .map(this::convertToUserDto)
                .toList();
            
            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_USERS_FOR_SCHEDULED_EXECUTION)
                .detail("userCount", String.valueOf(users.size()))
                .detail("tenantId", tenantId)
                .log();
            
            return users;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_FETCH_TENANT_USERS_FOR_SCHEDULED_EXECUTION)
                .detail("tenantId", tenantId)
                .log();
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_FETCH_TENANT_USERS, e);
        }
    }
    
    /**
     * Converts gRPC UserInfo to UserDto.
     * 
     * @param userInfo gRPC UserInfo object
     * @return UserDto object
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
     * Updates an existing dynamic rule identified by role ID.
     * If the schedule changes from DAILY to IMMEDIATE, the rule will be executed instantly.
     * 
     * @param request the dynamic rule update request containing roleId and new properties
     * @return the updated dynamic rule response
     * @throws IllegalArgumentException if the role or dynamic rule doesn't exist or doesn't belong to the tenant
     */
    @Override
    public DynamicRuleResponse updateDynamicRule(UpdateDynamicRuleRequest request) {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message(AppConstants.LOG_STARTING_DYNAMIC_RULE_UPDATE)
            .detail("roleId", request.roleId().toString())
            .detail("tenantId", tenantId)
            .detail("newScheduleType", request.scheduleType().name())
            .detail("newRuleIsActive", request.ruleIsActive().toString())
            .log();
        
        // 1. Validate that the role exists and belongs to the tenant
        Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException(AppConstants.ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT_UPDATE));
        
        // 2. Find the existing dynamic rule for this role
        DynamicRule existingRule = dynamicRuleRepository.findByRoleId(request.roleId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(AppConstants.ERROR_DYNAMIC_RULE_NOT_FOUND_FOR_ROLE));
        
        // 3. Store the previous values for comparison
        ScheduleType previousScheduleType = existingRule.getScheduleType();
        String previousConditionScript = existingRule.getRuleConditionScript();
        boolean previousIsActive = existingRule.getRuleIsActive();
        
        // Check if condition script is changing
        boolean conditionScriptChanged = !previousConditionScript.equals(request.ruleConditionScript());
        
        logger.info()
            .message(AppConstants.LOG_FOUND_EXISTING_DYNAMIC_RULE_FOR_UPDATE)
            .detail("dynamicRuleId", existingRule.getId().toString())
            .detail("previousScheduleType", previousScheduleType.name())
            .detail("newScheduleType", request.scheduleType().name())
            .detail("conditionScriptChanged", conditionScriptChanged)
            .detail("previousConditionScript", previousConditionScript)
            .detail("newConditionScript", request.ruleConditionScript())
            .log();
        
        // 4. Update the dynamic rule properties
        existingRule.setRuleIsActive(request.ruleIsActive());
        existingRule.setRuleConditionScript(request.ruleConditionScript());
        existingRule.setSendNotificationOnAssignment(request.sendNotificationOnAssignment());
        existingRule.setRequireApprovalBeforeAssignment(request.requireApprovalBeforeAssignment());
        existingRule.setScheduleType(request.scheduleType());
        
        // Convert time string to LocalTime if provided
        if (request.time() != null && !request.time().trim().isEmpty()) {
            existingRule.setTime(java.time.LocalTime.parse(request.time()));
        } else {
            existingRule.setTime(null);
        }
        
        // Set audit fields using JWT extraction
        setAuditFieldsForUpdate(existingRule);
        
        // 5. Save the updated rule
        DynamicRule updatedRule = dynamicRuleRepository.save(existingRule);
        
        logger.info()
            .message(AppConstants.LOG_DYNAMIC_RULE_UPDATED_SUCCESSFULLY)
            .detail("dynamicRuleId", updatedRule.getId().toString())
            .detail("roleId", request.roleId().toString())
            .log();
        
        // 6. Check if we need to execute reconciliation
        boolean ruleActivated = (!previousIsActive && request.ruleIsActive());
        boolean ruleDeactivated = (previousIsActive && !request.ruleIsActive());
        boolean shouldExecuteImmediately = (previousScheduleType == ScheduleType.DAILY && 
                                          request.scheduleType() == ScheduleType.IMMEDIATE &&
                                          request.ruleIsActive());
        
        boolean shouldReconcileCondition = (conditionScriptChanged && 
                                          request.ruleIsActive() && 
                                          request.scheduleType() == ScheduleType.IMMEDIATE);
        
        // Handle rule activation - reconcile all users
        if (ruleActivated && request.scheduleType() == ScheduleType.IMMEDIATE) {
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_ACTIVATED_TRIGGERING_RECONCILIATION)
                .detail("dynamicRuleId", updatedRule.getId().toString())
                .detail("roleId", request.roleId().toString())
                .detail("scheduleType", request.scheduleType().name())
                .log();
            
            try {
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    updatedRule.getRoleId(),
                    request.ruleConditionScript(),
                    previousConditionScript,
                    tenantId,
                    updatedRule.getSendNotificationOnAssignment()
                );
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_ACTIVATION)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("usersAdded", reconciliationResult.usersAdded().size())
                    .detail("successfulAdditions", reconciliationResult.successfulAdditions())
                    .log();
                    
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_RECONCILIATION_AFTER_RULE_ACTIVATION)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        // Handle rule deactivation - remove role from all users who have it
        else if (ruleDeactivated) {
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_DEACTIVATED_REMOVING_ROLE)
                .detail("dynamicRuleId", updatedRule.getId().toString())
                .detail("roleId", request.roleId().toString())
                .log();
            
            try {
                // Execute reconciliation with a condition that always returns false
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    updatedRule.getRoleId(),
                    "false", // Always false condition to remove role from everyone
                    previousConditionScript,
                    tenantId,
                    false // No notifications when deactivating
                );
                
                logger.info()
                    .message(AppConstants.LOG_RECONCILIATION_COMPLETED_AFTER_DEACTIVATION)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("usersRemoved", reconciliationResult.usersRemoved().size())
                    .detail("successfulRemovals", reconciliationResult.successfulRemovals())
                    .log();
                    
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_RECONCILIATION_AFTER_RULE_DEACTIVATION)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        else if (shouldExecuteImmediately) {
            logger.info()
                .message(AppConstants.LOG_SCHEDULE_CHANGED_FROM_DAILY_TO_IMMEDIATE)
                .detail("dynamicRuleId", updatedRule.getId().toString())
                .detail("roleId", request.roleId().toString())
                .log();
            
            try {
                // Execute reconciliation for schedule change
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    updatedRule.getRoleId(),
                    request.ruleConditionScript(),
                    previousConditionScript,
                    tenantId,
                    updatedRule.getSendNotificationOnAssignment()
                );
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_SCHEDULE_CHANGE)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("usersRemoved", reconciliationResult.usersRemoved().size())
                    .detail("usersAdded", reconciliationResult.usersAdded().size())
                    .detail("usersKept", reconciliationResult.usersKept().size())
                    .detail("successfulRemovals", reconciliationResult.successfulRemovals())
                    .detail("successfulAdditions", reconciliationResult.successfulAdditions())
                    .log();
                    
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_AFTER_SCHEDULE_CHANGE)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
                // Don't fail the update operation if execution fails
            }
        } else if (shouldReconcileCondition) {
            logger.info()
                .message(AppConstants.LOG_CONDITION_SCRIPT_CHANGED_FOR_IMMEDIATE_RULE)
                .detail("dynamicRuleId", updatedRule.getId().toString())
                .detail("roleId", request.roleId().toString())
                .detail("previousCondition", previousConditionScript)
                .detail("newCondition", request.ruleConditionScript())
                .log();
            
            try {
                // Execute reconciliation for condition script change
                RoleReconciliationResult reconciliationResult = reconcileRoleAssignments(
                    updatedRule.getRoleId(),
                    request.ruleConditionScript(),
                    previousConditionScript,
                    tenantId,
                    updatedRule.getSendNotificationOnAssignment()
                );
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_CONDITION_CHANGE)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("usersRemoved", reconciliationResult.usersRemoved().size())
                    .detail("usersAdded", reconciliationResult.usersAdded().size())
                    .detail("usersKept", reconciliationResult.usersKept().size())
                    .detail("successfulRemovals", reconciliationResult.successfulRemovals())
                    .detail("successfulAdditions", reconciliationResult.successfulAdditions())
                    .log();
                    
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_AFTER_CONDITION_CHANGE)
                    .detail("dynamicRuleId", updatedRule.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        // 7. Convert to response DTO
        return new DynamicRuleResponse(
            updatedRule.getId(),
            updatedRule.getRoleId(),
            role.getRoleName(),
            updatedRule.getRuleIsActive(),
            updatedRule.getRuleConditionScript(),
            updatedRule.getSendNotificationOnAssignment(),
            updatedRule.getRequireApprovalBeforeAssignment(),
            updatedRule.getScheduleType(),
            updatedRule.getTime(),
            updatedRule.getCreatedAt(),
            updatedRule.getUpdatedAt(),
            updatedRule.getCreatedById(),
            updatedRule.getCreatedByName(),
            updatedRule.getUpdatedById(),
            updatedRule.getUpdatedByName()
        );
    }
    
    /**
     * Sets audit fields for new entities using JWT extraction.
     */
    private void setAuditFieldsForCreate(DynamicRule dynamicRule) {
        try {
            String token = getCurrentJwtToken();
            
            if (token != null && !token.trim().isEmpty() && jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);    
                String username = jwtUtil.extractUsername(token); 
                
                if (userId != null && username != null) {
                    UUID userUUID = UUID.fromString(userId);
                    dynamicRule.setCreatedById(userUUID);
                    dynamicRule.setCreatedByName(username);
                    dynamicRule.setUpdatedById(userUUID);
                    dynamicRule.setUpdatedByName(username);
                    return;
                }
            }
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_JWT_USING_SYSTEM_USER)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Fallback to system user
        UUID systemUserId = UUID.fromString(AppConstants.SYSTEM_USER_ID);
        dynamicRule.setCreatedById(systemUserId);
        dynamicRule.setCreatedByName(AppConstants.SYSTEM_USER_NAME);
        dynamicRule.setUpdatedById(systemUserId);
        dynamicRule.setUpdatedByName(AppConstants.SYSTEM_USER_NAME);
    }
    
    /**
     * Sets audit fields for updated entities using JWT extraction.
     */
    private void setAuditFieldsForUpdate(DynamicRule dynamicRule) {
        try {
            String token = getCurrentJwtToken();
            
            if (token != null && !token.trim().isEmpty() && jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);      // Uses your extractUserId method
                String username = jwtUtil.extractUsername(token);  // Uses your extractUsername method
                
                if (userId != null && username != null) {
                    dynamicRule.setUpdatedById(UUID.fromString(userId));
                    dynamicRule.setUpdatedByName(username);
                    return;
                }
            }
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_JWT_USING_SYSTEM_USER)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Fallback to system user
        dynamicRule.setUpdatedById(UUID.fromString(AppConstants.SYSTEM_USER_ID));
        dynamicRule.setUpdatedByName(AppConstants.SYSTEM_USER_NAME);
    }
    
    /**
     * Get the current JWT token from various sources.
     */
    private String getCurrentJwtToken() {
        try {
            // Try to get from Spring Security context credentials
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String) {
                return (String) authentication.getCredentials();
            }
            
            // Try to get from HTTP request header if available
            try {
                org.springframework.web.context.request.RequestAttributes requestAttributes = 
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                    jakarta.servlet.http.HttpServletRequest request = 
                        ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7);
                    }
                }
            } catch (Exception e) {
                // HTTP request not available (e.g., in scheduled context)
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Reconciles role assignments when rule condition script changes.
     * This method evaluates all tenant users against the new condition and:
     * - Removes role from users who no longer match
     * - Keeps role for users who still match
     * - Adds role to new users who now match
     * 
     * @param roleId the role ID to reconcile
     * @param newConditionScript the new condition script
     * @param oldConditionScript the old condition script (for logging)
     * @param tenantId the tenant ID
     * @param sendNotificationOnAssignment whether to send notifications for role assignments
     * @return reconciliation result with before/after user lists
     */
    public RoleReconciliationResult reconcileRoleAssignments(
            UUID roleId, 
            String newConditionScript, 
            String oldConditionScript, 
            String tenantId,
            boolean sendNotificationOnAssignment) {
        
        long startTime = System.currentTimeMillis();
        
        logger.info()
            .message(AppConstants.LOG_STARTING_ROLE_ASSIGNMENT_RECONCILIATION)
            .detail("roleId", roleId.toString())
            .detail("tenantId", tenantId)
            .detail("oldConditionScript", oldConditionScript)
            .detail("newConditionScript", newConditionScript)
            .log();
        
        try {
            // 1. Get all current users who have this role
            logger.info()
                .message(AppConstants.LOG_ABOUT_TO_QUERY_USERS_WITH_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TIMESTAMP_MS, System.currentTimeMillis())
                .log();
                
            List<UUID> usersWithRoleBefore = userRoleService.getUsersWithRole(roleId, tenantId);
            
            logger.info()
                .message(AppConstants.LOG_QUERIED_USERS_WITH_ROLE_SNAPSHOT)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, usersWithRoleBefore.size())
                .detail(AppConstants.DETAIL_KEY_USER_IDS, usersWithRoleBefore.toString())
                .detail(AppConstants.DETAIL_KEY_TIMESTAMP_MS, System.currentTimeMillis())
                .log();
            
            // 2. Get all tenant users
            List<UserDto> allTenantUsers = fetchTenantUsersWithTenantId(tenantId);
            
            // 3. Evaluate new condition for all users
            List<UUID> usersWhoShouldHaveRole = new ArrayList<>();
            
            for (UserDto user : allTenantUsers) {
                try {
                    boolean shouldHaveRole = conditionalRuleEvaluationService.evaluateCondition(newConditionScript, user);
                    if (shouldHaveRole) {
                        usersWhoShouldHaveRole.add(UUID.fromString(user.getId()));
                    }
                } catch (Exception e) {
                    logger.warn()
                        .message(AppConstants.LOG_FAILED_TO_EVALUATE_CONDITION_FOR_USER_DURING_RECONCILIATION)
                        .detail("userId", user.getId())
                        .detail("error", e.getMessage())
                        .log();
                }
            }
            
            // 4. Calculate changes needed
            List<UUID> usersToRemove = new ArrayList<>(usersWithRoleBefore);
            usersToRemove.removeAll(usersWhoShouldHaveRole); // Users who had role but shouldn't anymore
            
            List<UUID> usersToAdd = new ArrayList<>(usersWhoShouldHaveRole);
            usersToAdd.removeAll(usersWithRoleBefore); // Users who should have role but don't
            
            List<UUID> usersToKeep = new ArrayList<>(usersWithRoleBefore);
            usersToKeep.retainAll(usersWhoShouldHaveRole); // Users who had role and should keep it
            
            logger.info()
                .message(AppConstants.LOG_ROLE_RECONCILIATION_ANALYSIS_COMPLETED)
                .detail("roleId", roleId.toString())
                .detail("usersBefore", usersWithRoleBefore.size())
                .detail("usersWhoShouldHave", usersWhoShouldHaveRole.size())
                .detail("usersToRemove", usersToRemove.size())
                .detail("usersToAdd", usersToAdd.size())
                .detail("usersToKeep", usersToKeep.size())
                .log();
            
            // 5. Execute changes
            int successfulRemovals = 0;
            int failedRemovals = 0;
            int successfulAdditions = 0;
            int failedAdditions = 0;
            
            // Store the users to add BEFORE bulk assignment for notification purposes
            List<UUID> usersToNotify = new ArrayList<>(usersToAdd);
            
            logger.info()
                .message(AppConstants.LOG_DEBUG_NOTIFICATION_STORED_USERS)
                .detail(AppConstants.DETAIL_KEY_USERS_TO_NOTIFY_COUNT, usersToNotify.size())
                .detail(AppConstants.DETAIL_KEY_USERS_TO_NOTIFY_LIST, usersToNotify.toString())
                .detail(AppConstants.DETAIL_KEY_USERS_TO_ADD_COUNT, usersToAdd.size())
                .detail(AppConstants.DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT, sendNotificationOnAssignment)
                .log();
            
            // CRITICAL FIX: Send notifications BEFORE bulk assignment to ensure they are sent
            // even if bulk assignment fails (e.g., users already have role)
            if (!usersToNotify.isEmpty() && sendNotificationOnAssignment) {
                try {
                    logger.info()
                        .message(AppConstants.LOG_SENDING_NOTIFICATIONS_BEFORE_ASSIGNMENT)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USERS_TO_NOTIFY, usersToNotify.size())
                        .log();
                        
                    sendDynamicRuleAssignmentNotifications(roleId, usersToNotify, tenantId);
                    
                } catch (Exception notificationException) {
                    // Log notification failure but don't fail the main operation
                    logger.warn()
                        .message(AppConstants.LOG_FAILED_SEND_NOTIFICATIONS_BEFORE_ASSIGNMENT)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USERS_TO_NOTIFY_COUNT, usersToNotify.size())
                        .detail(AppConstants.DETAIL_KEY_ERROR, notificationException.getMessage())
                        .log();
                }
            } else if (!usersToAdd.isEmpty() && !sendNotificationOnAssignment) {
                logger.debug()
                    .message(AppConstants.LOG_NOTIFICATIONS_DISABLED_RECONCILIATION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_USERS_TO_ADD_COUNT, usersToAdd.size())
                    .log();
            }
            
            // Set dynamic rule execution context to prevent individual role assignment audits
            RoleAuditAspect.setDynamicRuleExecutionContext(true);
            
            // Track successfully removed users for notification
            List<UUID> successfullyRemovedUsers = new ArrayList<>();
            
            try {
                // CRITICAL: Remove roles from users who no longer match
                // BUT ONLY remove DYNAMIC assignments - never touch MANUAL assignments!
                // This handles the case where a user was manually assigned a role, and then
                // a dynamic rule also matched them, and now the rule no longer matches.
                for (UUID userId : usersToRemove) {
                    try {
                        // Use unassignDynamicRole instead of unassignRole to protect MANUAL assignments
                        boolean removed = ((UserRoleServiceImpl) userRoleService).unassignDynamicRole(userId, roleId, tenantId);
                        if (removed) {
                            successfulRemovals++;
                            successfullyRemovedUsers.add(userId);
                            logger.debug()
                                .message(AppConstants.LOG_SUCCESSFULLY_REMOVED_ROLE_FROM_USER)
                                .detail("userId", userId.toString())
                                .detail("roleId", roleId.toString())
                                .detail(AppConstants.DETAIL_KEY_ASSIGNMENT_TYPE, AssignmentType.DYNAMIC.name())
                                .log();
                        } else {
                            logger.info()
                                .message(AppConstants.LOG_SKIPPED_ROLE_REMOVAL_USER_HAS_MANUAL_ASSIGNMENT)
                                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                                .log();
                        }
                            
                    } catch (Exception e) {
                        failedRemovals++;
                        logger.error(e)
                            .message(AppConstants.LOG_FAILED_TO_REMOVE_ROLE_FROM_USER)
                            .detail("userId", userId.toString())
                            .detail("roleId", roleId.toString())
                            .detail("error", e.getMessage())
                            .log();
                    }
                }
                
                // Send revocation notifications for successfully removed users
                if (!successfullyRemovedUsers.isEmpty()) {
                    try {
                        logger.info()
                            .message("Sending revocation notifications for dynamic rule removals")
                            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                            .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfullyRemovedUsers.size())
                            .log();
                            
                        sendDynamicRuleRevocationNotifications(roleId, successfullyRemovedUsers, tenantId);
                        
                    } catch (Exception notificationException) {
                        // Log notification failure but don't fail the main operation
                        logger.error(notificationException)
                            .message("Failed to send revocation notifications for dynamic rule removals")
                            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                            .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfullyRemovedUsers.size())
                            .detail(AppConstants.DETAIL_KEY_ERROR, notificationException.getMessage())
                            .log();
                    }
                }
                
                // Add roles to users who now match
                if (!usersToAdd.isEmpty()) {
                    try {
                        // Use bulk assignment for efficiency with DYNAMIC assignment type
                        AssignRoleRequest bulkAssignRequest = new AssignRoleRequest(null, usersToAdd, roleId, sendNotificationOnAssignment, null);  // requireApprovalBeforeAssignment not used
                        BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) userRoleService.assignRoleWithType(
                            bulkAssignRequest, 
                            tenantId, 
                            AssignmentType.DYNAMIC
                        );
                        
                        successfulAdditions = bulkResponse.successfulAssignments().size();
                        failedAdditions = bulkResponse.failedAssignments().size();
                        
                        logger.info()
                            .message(AppConstants.LOG_BULK_ROLE_ASSIGNMENT_COMPLETED_DURING_RECONCILIATION)
                            .detail("roleId", roleId.toString())
                            .detail("successfulAdditions", successfulAdditions)
                            .detail("failedAdditions", failedAdditions)
                            .log();
                            
                    } catch (Exception e) {
                        failedAdditions = usersToAdd.size();
                        logger.error(e)
                            .message(AppConstants.LOG_FAILED_TO_PERFORM_BULK_ROLE_ASSIGNMENT_DURING_RECONCILIATION)
                            .detail("roleId", roleId.toString())
                            .detail("usersToAddCount", usersToAdd.size())
                            .detail("error", e.getMessage())
                            .log();
                    }
                }
                
            } finally {
                // Always clear the dynamic rule execution context
                RoleAuditAspect.clearDynamicRuleExecutionContext();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 6. Create reconciliation result
            RoleReconciliationResult result = new RoleReconciliationResult(
                roleId,
                usersWithRoleBefore,      
                usersWhoShouldHaveRole,   
                usersToRemove,
                usersToAdd,
                usersToKeep,
                successfulRemovals,
                failedRemovals,
                successfulAdditions,
                failedAdditions,
                executionTime
            );
            
            logger.info()
                .message(AppConstants.LOG_ROLE_ASSIGNMENT_RECONCILIATION_COMPLETED)
                .detail("roleId", roleId.toString())
                .detail("tenantId", tenantId)
                .detail("executionTimeMs", executionTime)
                .detail("successfulRemovals", successfulRemovals)
                .detail("failedRemovals", failedRemovals)
                .detail("successfulAdditions", successfulAdditions)
                .detail("failedAdditions", failedAdditions)
                .detail("usersKept", usersToKeep.size())
                .log();
            
            // Only create audit log if there were actual changes (users added or removed)
            if (successfulAdditions > 0 || successfulRemovals > 0) {
                // Create comprehensive audit log for role reconciliation
                // Use a separate method to ensure audit event creation is in its own transaction
                publishRoleReconciliationAuditEventSafely(
                    roleId, 
                    tenantId,
                    usersWithRoleBefore, 
                    usersWhoShouldHaveRole,
                    usersToRemove,
                    usersToAdd,
                    usersToKeep,
                    successfulRemovals,
                    successfulAdditions,
                    allTenantUsers
                );
            } else {
                logger.debug()
                    .message(AppConstants.LOG_SKIPPING_AUDIT_EVENT_NO_RECONCILIATION_CHANGES)
                    .detail("roleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("usersKept", usersToKeep.size())
                    .log();
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.error(e)
                .message(AppConstants.LOG_ROLE_ASSIGNMENT_RECONCILIATION_FAILED)
                .detail("roleId", roleId.toString())
                .detail("tenantId", tenantId)
                .detail("executionTimeMs", executionTime)
                .detail("error", e.getMessage())
                .log();
            
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_RECONCILE_ROLE_ASSIGNMENTS, e);
        }
    }
    
    /**
     * Safely publishes a comprehensive audit event for role reconciliation in a separate transaction.
     * This method handles transaction rollback scenarios gracefully.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishRoleReconciliationAuditEventSafely(
            UUID roleId,
            String tenantId,
            List<UUID> usersWithRoleBefore,
            List<UUID> usersWithRoleAfter,
            List<UUID> usersRemoved,
            List<UUID> usersAdded,
            List<UUID> usersKept,
            int successfulRemovals,
            int successfulAdditions,
            List<UserDto> allTenantUsers) {
        
        try {
            publishRoleReconciliationAuditEvent(
                roleId,
                tenantId,
                usersWithRoleBefore,
                usersWithRoleAfter,
                usersRemoved,
                usersAdded,
                usersKept,
                successfulRemovals,
                successfulAdditions,
                allTenantUsers
            );
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_PUBLISH_RECONCILIATION_AUDIT_SAFELY)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage() != null ? e.getMessage() : "No error message")
                .detail(AppConstants.DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .log();
        }
    }
    
    /**
     * Publishes a comprehensive audit event for role reconciliation showing 
     * users before and after the reconciliation process.
     */
    private void publishRoleReconciliationAuditEvent(
            UUID roleId,
            String tenantId,
            List<UUID> usersWithRoleBefore,
            List<UUID> usersWithRoleAfter,
            List<UUID> usersRemoved,
            List<UUID> usersAdded,
            List<UUID> usersKept,
            int successfulRemovals,
            int successfulAdditions,
            List<UserDto> allTenantUsers) {
        
        try {
            // Get role information
            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_CANNOT_CREATE_RECONCILIATION_AUDIT_ROLE_NOT_FOUND)
                    .detail("roleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            
            // Debug log to verify role risk level
            logger.info()
                .message(AppConstants.LOG_ROLE_RECONCILIATION_ROLE_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_RISK_LEVEL, role.getRiskLevel().name())
                .detail(AppConstants.DETAIL_KEY_ROLE_RISK_LEVEL_LOWERCASE, role.getRiskLevel().name().toLowerCase())
                .log();
            
            // Create user lookup map for efficiency
            Map<String, String> userIdToNameMap = allTenantUsers.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getUsername));
            
            // Build stateBefore - users who had the role before reconciliation
            Map<String, Object> stateBefore = new HashMap<>();
            List<Map<String, String>> usersBeforeList = usersWithRoleBefore.stream()
                .map(userId -> {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("userId", userId.toString());
                    userInfo.put("userName", userIdToNameMap.getOrDefault(userId.toString(), "unknown"));
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            stateBefore.put("users", usersBeforeList);
            stateBefore.put("roleId", roleId.toString());
            stateBefore.put("roleName", role.getRoleName());
            stateBefore.put("userCount", usersWithRoleBefore.size());
            
            // Build stateAfter - users who have the role after reconciliation
            Map<String, Object> stateAfter = new HashMap<>();
            List<Map<String, String>> usersAfterList = usersWithRoleAfter.stream()
                .map(userId -> {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("userId", userId.toString());
                    userInfo.put("userName", userIdToNameMap.getOrDefault(userId.toString(), "unknown"));
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            stateAfter.put("users", usersAfterList);
            stateAfter.put("roleId", roleId.toString());
            stateAfter.put("roleName", role.getRoleName());
            stateAfter.put("userCount", usersWithRoleAfter.size());
            
            // Add reconciliation summary to stateAfter
            Map<String, Object> reconciliationSummary = new HashMap<>();
            reconciliationSummary.put("usersAdded", usersAdded.size());
            reconciliationSummary.put("usersRemoved", usersRemoved.size());
            reconciliationSummary.put("usersKept", usersKept.size());
            reconciliationSummary.put("successfulAdditions", successfulAdditions);
            reconciliationSummary.put("successfulRemovals", successfulRemovals);
            stateAfter.put("reconciliationSummary", reconciliationSummary);
            
            // Debug log before creating audit event
            String riskLevelToUse = role.getRiskLevel().name().toLowerCase();
            String auditEventSource = "DynamicRuleServiceImpl-reconciliation-" + System.nanoTime();
            logger.info()
                .message(AppConstants.LOG_CREATING_ROLE_RECONCILIATION_AUDIT)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .detail(AppConstants.DETAIL_KEY_RISK_LEVEL_BEING_USED, riskLevelToUse)
                .detail(AppConstants.DETAIL_KEY_AUDIT_EVENT_SOURCE, auditEventSource)
                .detail(AppConstants.DETAIL_KEY_USERS_BEFORE_COUNT, usersWithRoleBefore.size())
                .detail(AppConstants.DETAIL_KEY_USERS_AFTER_COUNT, usersWithRoleAfter.size())
                .detail(AppConstants.DETAIL_KEY_USERS_ADDED_COUNT, usersAdded.size())
                .detail(AppConstants.DETAIL_KEY_USERS_REMOVED_COUNT, usersRemoved.size())
                .detail(AppConstants.DETAIL_KEY_THREAD_ID, Thread.currentThread().getId())
                .detail(AppConstants.DETAIL_KEY_THREAD_NAME, Thread.currentThread().getName())
                .detail(AppConstants.DETAIL_KEY_STACK_TRACE, getStackTraceInfo())
                .log();
            
            // Create audit event for reconciliation
            RoleAuditEvent event = RoleAuditEvent.create(
                AppConstants.SYSTEM_USER_ID, // System user ID
                AppConstants.SYSTEM_USER_NAME, // System username
                AppConstants.UNKNOWN_IP_ADDRESS, // IP address
                AppConstants.ACTION_ROLE_RECONCILIATION, // Action
                AppConstants.ENTITY_TYPE_ROLE,
                roleId.toString(),
                role.getRoleName(),
                tenantId,
                objectMapper.valueToTree(stateBefore),
                objectMapper.valueToTree(stateAfter),
                riskLevelToUse
            );
            
            logger.info()
                .message(AppConstants.LOG_PUBLISHING_ROLE_RECONCILIATION_AUDIT_EVENT)
                .detail("roleId", roleId.toString())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .detail("usersBefore", usersWithRoleBefore.size())
                .detail("usersAfter", usersWithRoleAfter.size())
                .log();
            
            auditEventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_PUBLISH_ROLE_RECONCILIATION_AUDIT_EVENT)
                .detail("roleId", roleId.toString())
                .detail("tenantId", tenantId)
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Result of role assignment reconciliation operation.
     */
    public record RoleReconciliationResult(
        UUID roleId,
        List<UUID> usersWithRoleBefore,
        List<UUID> usersWithRoleAfter,
        List<UUID> usersRemoved,
        List<UUID> usersAdded,
        List<UUID> usersKept,
        int successfulRemovals,
        int failedRemovals,
        int successfulAdditions,
        int failedAdditions,
        long executionTimeMs
    ) {}
    
    /**
     * Sends role assignment notifications via Kafka for successful Dynamic Rule assignments only.
     * This method is similar to the one in UserRoleController but specifically for Dynamic Rules.
     * 
     * @param roleId the role ID that was assigned
     * @param successfulUserIds the list of user IDs who successfully received the role
     * @param tenantId the tenant ID
     */
    private void sendDynamicRuleAssignmentNotifications(UUID roleId, List<UUID> successfulUserIds, String tenantId) {
        try {
            logger.debug()
                .message(AppConstants.LOG_PREPARING_DYNAMIC_RULE_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType("DYNAMIC_RULE_NOTIFICATION")
                .log();
            
            // Get role information
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_ROLE_NOT_FOUND_DYNAMIC_RULE_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType("DYNAMIC_RULE_NOTIFICATION")
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .message(AppConstants.LOG_ROLE_INFO_RETRIEVED_NOTIFICATION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType("DYNAMIC_RULE_NOTIFICATION")
                .log();
            
            // Get all users in the tenant
            List<UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo user : allUsers) {
                userMap.put(user.getId(), user);
            }
            
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_USER_DATA_NOTIFICATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_IN_TENANT, allUsers.size())
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType("DYNAMIC_RULE_NOTIFICATION")
                .log();
            
            // Create notification data for each successfully assigned user only
            List<NotificationEventData.UserNotificationData> notificationDataList = new ArrayList<>();
            
            for (UUID userId : successfulUserIds) {
                UserInfo userInfo = userMap.get(userId.toString());
                if (userInfo != null) {
                    NotificationEventData.UserData userData = new NotificationEventData.UserData(
                        userInfo.getEmail(), 
                        userInfo.getUsername()
                    );
                    
                    NotificationEventData.TemplateVariables templateVariables = 
                        new NotificationEventData.TemplateVariables(
                            userInfo.getUsername(),
                            userInfo.getEmail(),
                            roleName
                        );
                    
                    NotificationEventData.UserNotificationData userNotificationData = 
                        new NotificationEventData.UserNotificationData(
                            userId.toString(),
                            userData,
                            templateVariables
                        );
                    
                    notificationDataList.add(userNotificationData);
                    
                    logger.trace()
                        .message(AppConstants.LOG_CREATED_NOTIFICATION_DATA_USER)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_USERNAME, userInfo.getUsername())
                        .detail(AppConstants.DETAIL_KEY_EMAIL, userInfo.getEmail())
                        .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                        .objectType("DYNAMIC_RULE_NOTIFICATION")
                        .log();
                } else {
                    logger.warn()
                        .message(AppConstants.LOG_USER_NOT_FOUND_NOTIFICATION)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType("DYNAMIC_RULE_NOTIFICATION")
                        .log();
                }
            }
            
            if (!notificationDataList.isEmpty()) {
                // Create and send notification event
                NotificationEventData eventData = new NotificationEventData(
                    NotificationEventData.OperationType.ASSIGN_INTERNAL_ROLE,
                    tenantId,
                    notificationDataList
                );
                
                logger.debug()
                    .message(AppConstants.LOG_SENDING_DYNAMIC_RULE_NOTIFICATION_EVENT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.DETAIL_KEY_OPERATION_TYPE, NotificationEventData.OperationType.ASSIGN_INTERNAL_ROLE.name())
                    .detail(AppConstants.DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType("DYNAMIC_RULE_NOTIFICATION")
                    .log();
                
                notificationKafkaService.sendNotificationEvent(eventData, tenantId);
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_NOTIFICATION_SENT_SUCCESSFULLY)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType("DYNAMIC_RULE_NOTIFICATION")
                    .log();
            } else {
                logger.warn()
                    .message(AppConstants.LOG_NO_VALID_USERS_FOR_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .objectType("DYNAMIC_RULE_NOTIFICATION")
                    .log();
            }
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_SEND_DYNAMIC_RULE_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType("DYNAMIC_RULE_NOTIFICATION")
                .log();
            // Don't throw exception to avoid failing the main operation
        }
    }
    
    /**
     * Helper method to get simplified stack trace for debugging duplicate audit events
     */
    private String getStackTraceInfo() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= Math.min(5, stack.length - 1); i++) {
            StackTraceElement element = stack[i];
            sb.append(element.getMethodName()).append("()");
            if (i < Math.min(5, stack.length - 1)) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }
    
    @Override
    public DynamicRuleResponse getDynamicRuleByRoleId(UUID roleId) {
        logger.info()
            .message(AppConstants.LOG_RETRIEVING_DYNAMIC_RULE_BY_ROLE_ID)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .log();
        
        try {
            // Get tenant context
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Validate that the role exists and belongs to the tenant
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_ROLE_NOT_FOUND_FOR_DYNAMIC_RULE_RETRIEVAL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_ROLE_NOT_FOUND_WITH_ID + roleId);
            }
            
            Role role = roleOpt.get();
            
            // Validate tenant access
            if (!tenantId.equals(role.getTenantId())) {
                logger.error()
                    .message(AppConstants.LOG_ROLE_DOES_NOT_BELONG_TO_TENANT)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_TENANT_ID, role.getTenantId())
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_ROLE_DOES_NOT_BELONG_TO_TENANT + tenantId);
            }
            
            // Find dynamic rule by role ID
            List<DynamicRule> dynamicRules = dynamicRuleRepository.findByRoleId(roleId);
            
            if (dynamicRules.isEmpty()) {
                logger.info()
                    .message(AppConstants.LOG_NO_DYNAMIC_RULE_FOUND_FOR_ROLE)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .log();
                return null; // No dynamic rule for this role
            }
            
            // Get the first (and should be only) dynamic rule for this role
            DynamicRule dynamicRule = dynamicRules.get(0);
            
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, dynamicRule.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .log();
            
            // Convert to response DTO
            return new DynamicRuleResponse(
                dynamicRule.getId(),
                dynamicRule.getRoleId(),
                role.getRoleName(),
                dynamicRule.getRuleIsActive(),
                dynamicRule.getRuleConditionScript(),
                dynamicRule.getSendNotificationOnAssignment(),
                dynamicRule.getRequireApprovalBeforeAssignment(),
                dynamicRule.getScheduleType(),
                dynamicRule.getTime(),
                dynamicRule.getCreatedAt(),
                dynamicRule.getUpdatedAt(),
                dynamicRule.getCreatedById(),
                dynamicRule.getCreatedByName(),
                dynamicRule.getUpdatedById(),
                dynamicRule.getUpdatedByName()
            );
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_DYNAMIC_RULE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .log();
            throw e;
        }
    }
    
    /**
     * Sends role revocation notifications via Kafka for successful Dynamic Rule revocations only.
     * This method is called when users lose roles due to CDC, reconciliation, or rule updates.
     * 
     * @param roleId the role ID that was revoked
     * @param successfulUserIds the list of user IDs who successfully had the role revoked
     * @param tenantId the tenant ID
     */
    private void sendDynamicRuleRevocationNotifications(UUID roleId, List<UUID> successfulUserIds, String tenantId) {
        try {
            logger.debug()
                .message(AppConstants.LOG_PREPARING_DYNAMIC_RULE_REVOKE_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                .log();
            
            // Get role information
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_ROLE_NOT_FOUND_DYNAMIC_RULE_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .message(AppConstants.LOG_ROLE_INFO_RETRIEVED_NOTIFICATION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                .log();
            
            // Get all users in the tenant
            List<UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo user : allUsers) {
                userMap.put(user.getId(), user);
            }
            
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_USER_DATA_NOTIFICATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_IN_TENANT, allUsers.size())
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                .log();
            
            // Create notification data for each successfully revoked user
            List<NotificationEventData.UserNotificationData> notificationDataList = new ArrayList<>();
            
            for (UUID userId : successfulUserIds) {
                UserInfo userInfo = userMap.get(userId.toString());
                if (userInfo != null) {
                    NotificationEventData.UserData userData = new NotificationEventData.UserData(
                        userInfo.getEmail(), 
                        userInfo.getUsername()
                    );
                    
                    NotificationEventData.TemplateVariables templateVariables = 
                        new NotificationEventData.TemplateVariables(
                            userInfo.getUsername(),
                            userInfo.getEmail(),
                            roleName
                        );
                    
                    NotificationEventData.UserNotificationData userNotificationData = 
                        new NotificationEventData.UserNotificationData(
                            userId.toString(),
                            userData,
                            templateVariables
                        );
                    
                    notificationDataList.add(userNotificationData);
                    
                    logger.trace()
                        .message(AppConstants.LOG_CREATED_NOTIFICATION_DATA_USER)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_USERNAME, userInfo.getUsername())
                        .detail(AppConstants.DETAIL_KEY_EMAIL, userInfo.getEmail())
                        .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                        .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                        .log();
                } else {
                    logger.warn()
                        .message(AppConstants.LOG_USER_NOT_FOUND_NOTIFICATION)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                        .log();
                }
            }
            
            if (!notificationDataList.isEmpty()) {
                // Create and send notification event
                NotificationEventData eventData = new NotificationEventData(
                    NotificationEventData.OperationType.REVOKE_INTERNAL_ROLE,
                    tenantId,
                    notificationDataList
                );
                
                logger.debug()
                    .message(AppConstants.LOG_SENDING_DYNAMIC_RULE_REVOKE_NOTIFICATION_EVENT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.DETAIL_KEY_OPERATION_TYPE, NotificationEventData.OperationType.REVOKE_INTERNAL_ROLE.name())
                    .detail(AppConstants.DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                    .log();
                
                notificationKafkaService.sendNotificationEvent(eventData, tenantId);
                
                logger.info()
                    .message(AppConstants.LOG_DYNAMIC_RULE_REVOKE_NOTIFICATION_SENT_SUCCESSFULLY)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                    .log();
            } else {
                logger.warn()
                    .message(AppConstants.LOG_NO_VALID_USERS_FOR_REVOKE_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                    .log();
            }
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_SEND_DYNAMIC_RULE_REVOKE_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType("DYNAMIC_RULE_REVOKE_NOTIFICATION")
                .log();
            // Don't throw exception to avoid failing the main operation
        }
    }
}