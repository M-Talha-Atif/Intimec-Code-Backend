package com.aidentx.applicationpermissions.roles_management.config.audit;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignPermissionToRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignPermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentItem;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevokePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevocationResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRoleRevocationResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkUserRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.RemoveRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.RevokeRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdatePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UserRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.ConditionalRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.ActionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.ConditionalRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.ModuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PageRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PermissionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleOwnersRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RolePermissionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.UserRoleRepository;
import com.aidentx.applicationpermissions.roles_management.model.entity.RoleOwners;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Aspect for auditing role management operations.
 * <p>
 * This aspect intercepts CRUD operations on role management entities
 * to capture audit information and publish it to Kafka for compliance and
 * monitoring purposes. Ensures sensitive data is handled appropriately
 * in audit logs.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-01-21
 */
@Aspect
@Component
public class RoleAuditAspect {

    private static final String LOG_CATEGORY = AppConstants.LOG_CATEGORY_ROLE_AUDIT;
    
    // ThreadLocal to track when we're in dynamic rule execution context
    private static final ThreadLocal<Boolean> DYNAMIC_RULE_EXECUTION_CONTEXT = new ThreadLocal<>();
    
    private final RoleSecurityContextService securityContextService;
    private final TenantContextService tenantContextService;
    private final IUnifiedLogger logger;
    private final RoleAuditEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RoleRepository repository;
    private final UserRoleRepository userRoleRepository;
    private final ActionRepository actionRepository;
    private final PageRepository pageRepository;
    private final ModuleRepository moduleRepository;
    private final ConditionalRuleRepository conditionalRuleRepository;
    private final PermissionRepository permissionRepository;
    private final UserGrpcClient userGrpcClient;
    private final DynamicRuleRepository dynamicRuleRepository;
    private final ConditionalRuleEvaluationService conditionalRuleEvaluationService;
    private final RoleOwnersRepository roleOwnersRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleAuditAspect(
            RoleAuditEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory unifiedLoggerFactory,
            RoleSecurityContextService securityContextService,
            TenantContextService tenantContextService,
            RoleRepository repository,
            UserRoleRepository userRoleRepository,
            ActionRepository actionRepository,
            PageRepository pageRepository,
            ModuleRepository moduleRepository,
            ConditionalRuleRepository conditionalRuleRepository,
            PermissionRepository permissionRepository,
            UserGrpcClient userGrpcClient,
            DynamicRuleRepository dynamicRuleRepository,
            ConditionalRuleEvaluationService conditionalRuleEvaluationService,
            RoleOwnersRepository roleOwnersRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger(
            AppConstants.ROLE_AUDIT_ASPECT_LOGGER_NAME, 
            LOG_CATEGORY
        );
        this.securityContextService = securityContextService;
        this.tenantContextService = tenantContextService;
        this.repository = repository;
        this.userRoleRepository = userRoleRepository;
        this.actionRepository = actionRepository;
        this.pageRepository = pageRepository;
        this.moduleRepository = moduleRepository;
        this.conditionalRuleRepository = conditionalRuleRepository;
        this.permissionRepository = permissionRepository;
        this.userGrpcClient = userGrpcClient;
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.conditionalRuleEvaluationService = conditionalRuleEvaluationService;
        this.roleOwnersRepository = roleOwnersRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    // ==================== DYNAMIC RULE EXECUTION CONTEXT ====================
    
    /**
     * Sets the dynamic rule execution context to prevent duplicate audit logs
     * when role assignments happen as part of IMMEDIATE dynamic rule execution.
     */
    public static void setDynamicRuleExecutionContext(boolean inExecution) {
        DYNAMIC_RULE_EXECUTION_CONTEXT.set(inExecution);
    }
    
    /**
     * Checks if we're currently in dynamic rule execution context.
     */
    public static boolean isInDynamicRuleExecutionContext() {
        Boolean inContext = DYNAMIC_RULE_EXECUTION_CONTEXT.get();
        return inContext != null && inContext;
    }
    
    /**
     * Clears the dynamic rule execution context.
     */
    public static void clearDynamicRuleExecutionContext() {
        DYNAMIC_RULE_EXECUTION_CONTEXT.remove();
    }

    // ==================== ROLE POINTCUTS ====================

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.createRole(..))")
    public void createRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.updateRole(..))")
    public void updateRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.deleteRole(..))")
    public void deleteRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.copyRole(..))")
    public void copyRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.bulkDeleteRoles(..))")
    public void bulkDeleteRolesPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl.assignRole(..))")
    public void assignRoleToUserPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl.removeRole(..))")
    public void removeRoleFromUserPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl.revokeRole(..))")
    public void revokeRoleFromUsersPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.assignPermissionToRole(..))")
    public void assignPermissionToRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.assignPermissionsBulk(..))")
    public void assignPermissionsBulkPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.revokePermissionsBulk(..))")
    public void revokePermissionsBulkPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.deletePermissionFromRole(..))")
    public void deletePermissionFromRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.updatePermissionForRole(java.util.UUID, java.util.UUID, com.aidentx.applicationpermissions.roles_management.model.dto.UpdatePermissionRequest))")
    public void updatePermissionForRolePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.DynamicRuleServiceImpl.createDynamicRule(..))")
    public void createDynamicRulePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.applicationpermissions.roles_management.service.impl.DynamicRuleServiceImpl.updateDynamicRule(..))")
    public void updateDynamicRulePointcut() {
    }

    // ==================== ROLE AUDIT METHODS ====================

    @Around("createRolePointcut()")
    public Object auditCreateRole(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_ROLE_CREATION)
                .log();
                
        CreateRoleRequest createDto = (CreateRoleRequest) pjp.getArgs()[0];
        boolean isSubmitForApproval = createDto.getSubmitForApproval() != null && createDto.getSubmitForApproval();

        logger.info()
                .category(LOG_CATEGORY)
                .message(isSubmitForApproval ? "Auditing role submission for approval" : AppConstants.AUDIT_LOG_AUDITING_ROLE_CREATION)
                .details(Map.of(
                        AppConstants.DTO_ROLE_NAME_DESCRIPTION, createDto.getRoleName(),
                        AppConstants.DTO_ROLE_DESCRIPTION_DESCRIPTION, createDto.getDescription() != null ? createDto.getDescription() : "",
                        "submitForApproval", String.valueOf(isSubmitForApproval)))
                .log();

        // Execute the method
        RoleResponse result = (RoleResponse) pjp.proceed();

        try {
            // Only publish audit event if role was actually created (not just submitted for approval)
            if (!isSubmitForApproval) {
                // Sanitize result and publish audit event
                JsonNode stateAfter = sanitizeRoleData(objectMapper.valueToTree(result));
                publishRoleAuditEvent(
                    AppConstants.AUDIT_EVENT_CREATE, 
                    result.getId().toString(), 
                    result.getRoleName(), 
                    result.getTenantId(), 
                    null, 
                    stateAfter,
                    result.getRiskLevel().name().toLowerCase()
                );
                
                logger.info()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_CREATION)
                        .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, result.getId().toString()))
                        .log();
            } else {
                logger.info()
                        .category(LOG_CATEGORY)
                        .message("Role submission audit skipped - role not yet created (pending approval)")
                        .details(Map.of("roleName", result.getRoleName()))
                        .log();
            }
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_CREATION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, result.getId().toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("updateRolePointcut()")
    public Object auditUpdateRole(ProceedingJoinPoint pjp) throws Throwable {
        UUID roleId = (UUID) pjp.getArgs()[0];
        UpdateRoleRequest updateDto = (UpdateRoleRequest) pjp.getArgs()[1];

        // Get state before update with complete role data including owners
        JsonNode stateBefore = null;
        String roleName = AppConstants.DEFAULT_UNKNOWN_ROLE;
        String tenantId = AppConstants.DEFAULT_UNKNOWN_ROLE;

        try {
            Role currentRoleEntity = repository.findById(roleId).orElse(null);
            if (currentRoleEntity != null) {
                // Build complete role response with owners data
                RoleResponse completeRoleData = buildCompleteRoleResponse(currentRoleEntity);
                stateBefore = sanitizeRoleData(objectMapper.valueToTree(completeRoleData));
                roleName = currentRoleEntity.getRoleName();
                tenantId = currentRoleEntity.getTenantId();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_UPDATE)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_ROLE_UPDATE)
                .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString()))
                .log();

        // Execute the method
        RoleResponse result = (RoleResponse) pjp.proceed();

        try {
            // Sanitize result and publish audit event
            JsonNode stateAfter = sanitizeRoleData(objectMapper.valueToTree(result));
            publishRoleAuditEvent(
                AppConstants.AUDIT_EVENT_UPDATE, 
                result.getId().toString(), 
                result.getRoleName(), 
                result.getTenantId() != null ? result.getTenantId() : tenantId, 
                stateBefore, 
                stateAfter,
                result.getRiskLevel().name().toLowerCase()
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_UPDATE)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, result.getId().toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_UPDATE)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, result.getId().toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("deleteRolePointcut()")
    public Object auditDeleteRole(ProceedingJoinPoint pjp) throws Throwable {
        UUID roleId = (UUID) pjp.getArgs()[0];

        // Get state before deletion
        JsonNode stateBefore = null;
        String roleName = null;
        String tenantId = null;
        Role currentRoleEntity = null; // Declare outside try block

        try {
            currentRoleEntity = repository.findById(roleId).orElse(null);
            if (currentRoleEntity != null) {
                stateBefore = sanitizeRoleData(objectMapper.valueToTree(currentRoleEntity));
                roleName = currentRoleEntity.getRoleName();
                tenantId = currentRoleEntity.getTenantId();
            } else {
                return pjp.proceed(); // Continue without audit if role doesn't exist
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_DELETION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            return pjp.proceed(); // Continue without audit if role doesn't exist
        }

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_ROLE_DELETION)
                .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString()))
                .log();

        // Execute the method
        Object result = pjp.proceed();

        try {
            // Get risk level from the role entity before deletion
            String riskLevel = currentRoleEntity != null ? 
                currentRoleEntity.getRiskLevel().name().toLowerCase() : AppConstants.DTO_RISK_LEVEL_EXAMPLE.toLowerCase();

            boolean wasDeleted = (result instanceof Boolean) && Boolean.TRUE.equals(result);

            // Publish audit event only when the entity was actually deleted.
            if (wasDeleted) {
                publishRoleAuditEvent(
                    AppConstants.AUDIT_EVENT_DELETE,
                    roleId.toString(),
                    roleName,
                    tenantId,
                    stateBefore,
                    null,
                    riskLevel
                );
            }
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_DELETION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_DELETION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("copyRolePointcut()")
    public Object auditCopyRole(ProceedingJoinPoint pjp) throws Throwable {
        UUID sourceRoleId = (UUID) pjp.getArgs()[0];
        
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_ROLE_COPY)
                .log();
        
        // Get source role state before copy
        JsonNode sourceRoleState = null;
        String sourceRoleName = null;
        String tenantId = null;
        String riskLevel = AppConstants.DTO_RISK_LEVEL_EXAMPLE.toLowerCase();
        
        try {
            Role sourceRole = repository.findById(sourceRoleId).orElse(null);
            if (sourceRole != null) {
                sourceRoleState = sanitizeRoleData(objectMapper.valueToTree(sourceRole));
                sourceRoleName = sourceRole.getRoleName();
                tenantId = sourceRole.getTenantId();
                riskLevel = sourceRole.getRiskLevel().name().toLowerCase();
                
                logger.info()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_AUDITING_ROLE_COPY)
                        .details(Map.of(
                                AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, sourceRoleId.toString(),
                                AppConstants.DETAIL_KEY_SOURCE_ROLE_NAME, sourceRoleName,
                                AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                        .log();
            } else {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_DELETION)
                        .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, sourceRoleId.toString()))
                        .log();
                return pjp.proceed(); // Continue without audit if source role doesn't exist
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_DELETION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, sourceRoleId.toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            return pjp.proceed(); // Continue without audit if error
        }
        
        // Execute the copy operation
        RoleResponse result = (RoleResponse) pjp.proceed();
        
        try {
            // Get the new copied role state
            JsonNode newRoleState = sanitizeRoleData(objectMapper.valueToTree(result));
            
            // Create audit event with copy action
            AuditUser user = securityContextService.getAuditUser();
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_OPERATION_TYPE, AppConstants.AUDIT_EVENT_COPY_ROLE,
                            AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, sourceRoleId.toString(),
                            AppConstants.DETAIL_KEY_NEW_ROLE_ID, result.getId().toString(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();
            
            // Publish audit event for the copy operation
            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.AUDIT_EVENT_COPY_ROLE,
                    AppConstants.ENTITY_TYPE_ROLE,
                    result.getId().toString(),
                    result.getRoleName(),
                    tenantId,
                    null,  // state before = null for copy (it's a creation)
                    newRoleState,     // state after = new copied role with all details
                    riskLevel);
            
            eventPublisher.publishEvent(event);
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_COPY)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, sourceRoleId.toString(),
                            AppConstants.DETAIL_KEY_NEW_ROLE_ID, result.getId().toString(),
                            AppConstants.DETAIL_KEY_SOURCE_ROLE_NAME, sourceRoleName,
                            AppConstants.DETAIL_KEY_NEW_ROLE_NAME, result.getRoleName()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_COPY)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, sourceRoleId.toString(),
                            AppConstants.DETAIL_KEY_NEW_ROLE_ID, result.getId().toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }
        
        return result;
    }

    @Around("bulkDeleteRolesPointcut()")
    public Object auditBulkDeleteRoles(ProceedingJoinPoint pjp) throws Throwable {
        List<UUID> roleIds = (List<UUID>) pjp.getArgs()[0];
        String tenantId = (String) pjp.getArgs()[1];
        
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_BULK_ROLE_DELETE)
                .details(Map.of(
                        AppConstants.DETAIL_KEY_TOTAL_REQUESTED, roleIds.size(),
                        AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                .log();
        
        // Capture role information before deletion
        List<String> roleNames = new ArrayList<>();
        List<String> deletedRoleIds = new ArrayList<>();
        
        for (UUID roleId : roleIds) {
            try {
                Role role = repository.findById(roleId).orElse(null);
                if (role != null && role.getTenantId().equals(tenantId)) {
                    roleNames.add(role.getRoleName());
                    deletedRoleIds.add(roleId.toString());
                }
            } catch (Exception e) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message("Could not retrieve role information before bulk deletion")
                        .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                        .log();
            }
        }
        
        // Execute the bulk delete operation
        Map<String, Object> result = (Map<String, Object>) pjp.proceed();
        
        try {
            // Extract success and failure counts from result
            Integer successCount = (Integer) result.get("successCount");
            Integer failureCount = (Integer) result.get("failureCount");
            
            // Create audit event for bulk deletion
            AuditUser user = securityContextService.getAuditUser();
            
            // Handle null counts
            int safeSuccessCount = successCount != null ? successCount : 0;
            int safeFailureCount = failureCount != null ? failureCount : 0;
            
            Map<String, Object> auditDetails = new LinkedHashMap<>();
            auditDetails.put(AppConstants.DETAIL_KEY_OPERATION_TYPE, AppConstants.AUDIT_EVENT_BULK_ROLE_DELETE);
            auditDetails.put(AppConstants.DETAIL_KEY_TOTAL_REQUESTED, roleIds.size());
            auditDetails.put(AppConstants.DETAIL_KEY_SUCCESSFUL_DELETIONS, safeSuccessCount);
            auditDetails.put(AppConstants.DETAIL_KEY_FAILED_DELETIONS, safeFailureCount);
            auditDetails.put(AppConstants.DETAIL_KEY_USER_ID, user.id() != null ? user.id() : "unknown");
            auditDetails.put(AppConstants.DETAIL_KEY_USERNAME, user.username() != null ? user.username() : "unknown");
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .details(auditDetails)
                    .log();
            
            // Create state representation for audit
            ObjectNode stateAfter = objectMapper.createObjectNode();
            stateAfter.put(AppConstants.DETAIL_KEY_TOTAL_REQUESTED, roleIds.size());
            stateAfter.put(AppConstants.DETAIL_KEY_SUCCESSFUL_DELETIONS, safeSuccessCount);
            stateAfter.put(AppConstants.DETAIL_KEY_FAILED_DELETIONS, safeFailureCount);
            stateAfter.set(AppConstants.DETAIL_KEY_DELETED_ROLE_IDS, objectMapper.valueToTree(deletedRoleIds));
            stateAfter.set(AppConstants.DETAIL_KEY_DELETED_ROLE_NAMES, objectMapper.valueToTree(roleNames));
            
            // Publish audit event for bulk deletion
            // Using the first role name or "Multiple Roles" as the entity name
            String entityName = roleNames.isEmpty() ? "Multiple Roles" : 
                               (roleNames.size() == 1 ? roleNames.get(0) : roleNames.size() + " Roles");
            
            // Use first deleted role ID as entity ID, or generate a unique ID for bulk operation
            String entityId = deletedRoleIds.isEmpty() ? UUID.randomUUID().toString() : deletedRoleIds.get(0);
            
            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.AUDIT_EVENT_BULK_ROLE_DELETE,
                    AppConstants.ENTITY_TYPE_ROLE,
                    entityId, // Use first role ID or generated UUID
                    entityName,
                    tenantId,
                    null, // no state before for bulk delete
                    stateAfter,
                    AppConstants.DTO_RISK_LEVEL_EXAMPLE.toLowerCase());
            
            eventPublisher.publishEvent(event);
            
            Map<String, Object> successDetails = new LinkedHashMap<>();
            successDetails.put(AppConstants.DETAIL_KEY_TOTAL_REQUESTED, roleIds.size());
            successDetails.put(AppConstants.DETAIL_KEY_SUCCESSFUL_DELETIONS, safeSuccessCount);
            successDetails.put(AppConstants.DETAIL_KEY_FAILED_DELETIONS, safeFailureCount);
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_BULK_ROLE_DELETE)
                    .details(successDetails)
                    .log();
        } catch (Exception e) {
            Map<String, Object> errorDetails = new LinkedHashMap<>();
            errorDetails.put(AppConstants.DETAIL_KEY_TOTAL_REQUESTED, roleIds.size());
            errorDetails.put(AppConstants.DETAIL_KEY_ERROR, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_BULK_ROLE_DELETE)
                    .details(errorDetails)
                    .log();
        }
        
        return result;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a complete RoleResponse with owners data for audit purposes.
     * 
     * @param role the role entity
     * @return RoleResponse with all data including owners
     */
    private RoleResponse buildCompleteRoleResponse(Role role) {
        // Get the count of users assigned to this role within the tenant
        Long userCount = userRoleRepository.countByRoleIdAndTenantId(role.getId(), role.getTenantId());
        
        // Get the count of permissions assigned to this role
        Long permissionsCount = rolePermissionRepository.countByRoleId(role.getId());
        
        // Get owners data for this role
        List<UserDto> ownersData = getRoleOwnersData(role.getId(), role.getTenantId());
        
        // Calculate hasManualAssignments and hasDynamicRules dynamically by checking assignment types
        Boolean hasManualAssignments = userRoleRepository.existsByRoleIdAndTenantIdAndAssignmentType(
            role.getId(), 
            role.getTenantId(), 
            com.aidentx.applicationpermissions.roles_management.enums.AssignmentType.MANUAL
        );
        Boolean hasDynamicRules = userRoleRepository.existsByRoleIdAndTenantIdAndAssignmentType(
            role.getId(), 
            role.getTenantId(), 
            com.aidentx.applicationpermissions.roles_management.enums.AssignmentType.DYNAMIC
        );
        
        return new RoleResponse(
            role.getId(),
            role.getTenantId(),
            role.getRoleType(),
            role.getRoleName(),
            role.getDescription(),
            role.getBusinessJustification(),
            role.getRequireOwnerApproval(),
            role.getIsActive(),
            role.getTags(),
            userCount,
            permissionsCount,
            hasManualAssignments,
            hasDynamicRules,
            role.getRiskLevel(),
            role.getCreatedAt(),
            role.getUpdatedAt(),
            role.getCreatedByName(),
            role.getCreatedById(),
            role.getUpdatedByName(),
            role.getUpdatedById(),
            ownersData
        );
    }

    /**
     * Gets owners data for a role by fetching user information from UserGrpcClient.
     * 
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @return list of UserDto with owner information
     */
    private List<UserDto> getRoleOwnersData(UUID roleId, String tenantId) {
        try {
            // Get owner user IDs from role_owners table
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(roleId);
            
            if (roleOwners.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .collect(Collectors.toList());
            
            // Get all tenant users via gRPC
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create a map for quick lookup of users by ID
            java.util.HashMap<UUID, UserInfo> userMap = new java.util.HashMap<>();
            for (UserInfo user : tenantUsers) {
                userMap.put(UUID.fromString(user.getId()), user);
            }
            
            // Convert owner user info to UserDto objects
            List<UserDto> ownersData = new ArrayList<>();
            for (UUID ownerId : ownerIds) {
                UserInfo userInfo = userMap.get(ownerId);
                if (userInfo != null) {
                    // User exists and belongs to the tenant
                    UserDto userDto = convertToUserDto(userInfo);
                    ownersData.add(userDto);
                }
            }
            
            return ownersData;
            
        } catch (Exception e) {
            logger.warn()
                .category(LOG_CATEGORY)
                .message("Failed to fetch role owners data")
                .details(Map.of(
                    AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                    AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                .log();
            return new ArrayList<>();
        }
    }

    /**
     * Converts gRPC UserInfo to UserDto.
     * 
     * @param userInfo the UserInfo from gRPC
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
     * Sanitizes role data by including only relevant fields for audit purposes.
     * 
     * @param jsonNode the original JSON node
     * @return sanitized JSON node with only audit-relevant data
     */
    private JsonNode sanitizeRoleData(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            ObjectNode originalNode = (ObjectNode) jsonNode;
            ObjectNode sanitizedNode = objectMapper.createObjectNode();
            
            // Include all relevant role fields for audit
            String[] allowedFields = {
                "id",
                "roleName",
                "name",
                "roleType",
                "description", 
                "permissions",
                "tenantId",
                "businessJustification",
                "requireOwnerApproval",
                "requireManagerApproval",
                "tags",
                "riskLevel",
                "isActive"
            };
            
            for (String field : allowedFields) {
                if (originalNode.has(field)) {
                    // Sort tags array alphabetically to ensure consistent ordering
                    if (field.equals("tags") && originalNode.get(field).isArray()) {
                        ArrayNode tagsArray = (ArrayNode) originalNode.get(field);
                        List<String> tagsList = new ArrayList<>();
                        tagsArray.forEach(tag -> tagsList.add(tag.asText()));
                        tagsList.sort(String::compareTo);
                        
                        ArrayNode sortedTagsArray = objectMapper.createArrayNode();
                        tagsList.forEach(sortedTagsArray::add);
                        sanitizedNode.set(field, sortedTagsArray);
                    } else {
                        sanitizedNode.set(field, originalNode.get(field));
                    }
                }
            }
            
            // Handle owners data transformation
            if (originalNode.has("ownersData") && originalNode.get("ownersData").isArray()) {
                ArrayNode ownersArray = objectMapper.createArrayNode();
                ArrayNode originalOwners = (ArrayNode) originalNode.get("ownersData");
                
                for (JsonNode owner : originalOwners) {
                    ObjectNode ownerNode = objectMapper.createObjectNode();
                    
                    // Extract required fields and transform to expected format
                    if (owner.has("id")) {
                        ownerNode.put("id", owner.get("id").asText());
                    }
                    if (owner.has("email")) {
                        ownerNode.put("email", owner.get("email").asText());
                    }
                    
                    // Create username from firstName and lastName or use email as fallback
                    String username = "";
                    if (owner.has("firstName") && owner.has("lastName")) {
                        String firstName = owner.get("firstName").asText("");
                        String lastName = owner.get("lastName").asText("");
                        username = (firstName + " " + lastName).trim();
                    }
                    if (username.isEmpty() && owner.has("email")) {
                        username = owner.get("email").asText();
                    }
                    ownerNode.put("username", username);
                    
                    ownersArray.add(ownerNode);
                }
                
                sanitizedNode.set("owners", ownersArray);
            }
            
            return sanitizedNode;
        }
        return jsonNode;
    }

    /**
     * Publishes role audit event.
     */
    private void publishRoleAuditEvent(String action, String roleId, String roleName, 
            String tenantId, JsonNode stateBefore, JsonNode stateAfter, String riskLevel) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .details(Map.of(
                            "action", action,
                            AppConstants.DETAIL_KEY_ROLE_ID, roleId,
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    action,
                    AppConstants.ENTITY_TYPE_ROLE,
                    roleId,
                    roleName,
                    tenantId,
                    stateBefore,
                    stateAfter,
                    riskLevel);
            
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_ROLE_AUDIT_EVENT)
                    .details(Map.of(
                            "action", action,
                            AppConstants.DETAIL_KEY_ROLE_ID, roleId,
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }
    }

    // ==================== USER ROLE ASSIGNMENT AUDIT METHODS ====================

    @Around("assignRoleToUserPointcut()")
    public Object auditAssignRoleToUser(ProceedingJoinPoint pjp) throws Throwable {
        try {
            // Add debug logging to help identify duplicate audit events
            boolean inDynamicContext = isInDynamicRuleExecutionContext();
            boolean inCdcContext = com.aidentx.applicationpermissions.roles_management.service.reconciliation.DynamicRuleReconciliationServiceImpl.isInCdcReconciliationContext();
            Object contextValue = DYNAMIC_RULE_EXECUTION_CONTEXT.get();
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_ASSIGN_ROLE_POINTCUT_TRIGGERED_DEBUG)
                    .details(Map.of(
                        AppConstants.DETAIL_KEY_IN_DYNAMIC_RULE_CONTEXT, inDynamicContext,
                        "inCdcContext", inCdcContext,
                        AppConstants.DETAIL_KEY_THREAD_ID, Thread.currentThread().getId(),
                        AppConstants.DETAIL_KEY_THREAD_NAME, Thread.currentThread().getName(),
                        AppConstants.DETAIL_KEY_CONTEXT_VALUE, contextValue != null ? contextValue.toString() : ""))
                    .log();
            
            // Skip auditing if we're in dynamic rule execution context or CDC reconciliation context
            // This prevents duplicate audit logs when IMMEDIATE dynamic rules trigger role assignments
            if (inDynamicContext || inCdcContext) {
                logger.info()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_SKIPPING_INDIVIDUAL_ROLE_ASSIGNMENT_DEBUG)
                        .details(Map.of(
                            AppConstants.DETAIL_KEY_THREAD_ID, Thread.currentThread().getId(),
                            "inCdcContext", inCdcContext,
                            AppConstants.DETAIL_KEY_CONTEXT_VALUE, contextValue != null ? contextValue.toString() : ""))
                        .log();
                return pjp.proceed();
            }

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_USER_ROLE_ASSIGNMENT)
                    .log();

            AssignRoleRequest assignRequest = (AssignRoleRequest) pjp.getArgs()[0];
            String tenantId;
            
            // Check if this is the 2-parameter method call (from scheduled execution) 
            // or 1-parameter method call (from HTTP context)
            if (pjp.getArgs().length == 2 && pjp.getArgs()[1] instanceof String) {
                // Called from scheduled execution with explicit tenantId parameter
                tenantId = (String) pjp.getArgs()[1];
                logger.debug()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_USING_TENANT_ID_FROM_METHOD_PARAMETER)
                        .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                        .log();
            } else {
                // Called from HTTP context, get tenant from request context
                try {
                    tenantId = tenantContextService.getCurrentTenantId();
                    logger.debug()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.LOG_USING_TENANT_ID_FROM_HTTP_CONTEXT)
                            .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                            .log();
                } catch (RuntimeException e) {
                    logger.warn()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.LOG_TENANT_ID_NOT_FOUND_IN_JWT)
                            .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                            .log();
                    // Continue with method execution but skip audit
                    return pjp.proceed();
                }
            }

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_AUDITING_USER_ROLE_ASSIGNMENT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, assignRequest.roleId().toString(),
                            AppConstants.DETAIL_KEY_ASSIGNMENT_TYPE, assignRequest.isBulkAssignment() ? "bulk" : "single",
                            AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(assignRequest.getAllUserIds().size())))
                    .log();

            // Execute the method first, then audit
            Object result = pjp.proceed();

            // Only audit after successful execution
            try {
                // Get role information after assignment
                Role role = repository.findByIdAndTenantId(assignRequest.roleId(), tenantId)
                        .orElse(null);
                
                if (role != null) {
                    // Create audit event for role assignment
                    createUserRoleAssignmentAuditEvent(assignRequest, role, tenantId, result);
                } else {
                    logger.warn()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.ROLE_NOT_FOUND)
                            .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, assignRequest.roleId().toString(), AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                            .log();
                }
            } catch (Exception e) {
                logger.error(e)
                        .category(LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_USER_ROLE_ASSIGNMENT)
                        .details(Map.of(
                                AppConstants.DETAIL_KEY_ROLE_ID, assignRequest.roleId().toString(),
                                AppConstants.DETAIL_KEY_ERROR, e.getMessage() != null ? e.getMessage() : ""))
                        .log();
            }

            return result;
            
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage() != null ? e.getMessage() : "",
                            AppConstants.DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName()))
                    .log();
            // Execute the original method without audit
            return pjp.proceed();
        }
    }

    /**
     * Creates audit event for user role assignment.
     */
    private void createUserRoleAssignmentAuditEvent(AssignRoleRequest request, Role role, String tenantId, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            
            // Create state after with role and assigned users information
            JsonNode stateAfter = createAssignmentStateAfter(request, role, result);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT_USER_ROLE_ASSIGNMENT)
                    .details(Map.of(
                            "action", "assign role",
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            "roleName", role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "assign role",
                    AppConstants.ENTITY_TYPE_ROLE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    null, // stateBefore is null for assignment
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase());

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_USER_ROLE_ASSIGNMENT_AUDIT_EVENT)
                    .details(Map.of(
                            "action", "assign role",
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }
    }

    /**
     * Creates the stateAfter JSON for user role assignment audit.
     */
    private JsonNode createAssignmentStateAfter(AssignRoleRequest request, Role role, Object result) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        // Add role information
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());
        
        // Create users array with assigned user information
        var usersArray = stateAfter.putArray("users");
        
        try {
            // Get users from tenant to map userId to username
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(role.getTenantId());
            Map<String, String> userIdToUsernameMap = tenantUsers.stream()
                .collect(Collectors.toMap(
                    UserInfo::getId,
                    UserInfo::getUsername
                ));

            if (request.isBulkAssignment()) {
                // Handle bulk assignment response
                BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) result;
                for (UserRoleResponse userRole : bulkResponse.successfulAssignments()) {
                    ObjectNode userObj = objectMapper.createObjectNode();
                    String userId = userRole.userId().toString();
                    userObj.put("userId", userId);
                    // Get actual username from the map, fallback to placeholder if not found
                    String username = userIdToUsernameMap.getOrDefault(userId, "user_" + userId.substring(0, 8));
                    userObj.put("userName", username);
                    usersArray.add(userObj);
                }
            } else {
                // Handle single assignment response
                UserRoleResponse singleResponse = (UserRoleResponse) result;
                ObjectNode userObj = objectMapper.createObjectNode();
                String userId = singleResponse.userId().toString();
                userObj.put("userId", userId);
                // Get actual username from the map, fallback to placeholder if not found
                String username = userIdToUsernameMap.getOrDefault(userId, "user_" + userId.substring(0, 8));
                userObj.put("userName", username);
                usersArray.add(userObj);
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_AUDIT_USING_FALLBACK)
                    .details(Map.of("error", e.getMessage()))
                    .log();
            
            // Fallback to original logic if user service call fails
            try {
                if (request.isBulkAssignment()) {
                    BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) result;
                    for (UserRoleResponse userRole : bulkResponse.successfulAssignments()) {
                        ObjectNode userObj = objectMapper.createObjectNode();
                        userObj.put("userId", userRole.userId().toString());
                        userObj.put("userName", "user_" + userRole.userId().toString().substring(0, 8));
                        usersArray.add(userObj);
                    }
                } else {
                    UserRoleResponse singleResponse = (UserRoleResponse) result;
                    ObjectNode userObj = objectMapper.createObjectNode();
                    userObj.put("userId", singleResponse.userId().toString());
                    userObj.put("userName", "user_" + singleResponse.userId().toString().substring(0, 8));
                    usersArray.add(userObj);
                }
            } catch (Exception fallbackException) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_ASSIGNMENT_RESULT)
                        .details(Map.of("error", fallbackException.getMessage()))
                        .log();
            }
        }
        
        return stateAfter;
    }

    // ==================== USER ROLE REMOVAL AUDIT METHODS ====================

    @Around("removeRoleFromUserPointcut()")
    public Object auditRemoveRoleFromUser(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_USER_ROLE_REMOVAL)
                .log();

        RemoveRoleRequest removeRequest = (RemoveRoleRequest) pjp.getArgs()[0];
        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_USER_ROLE_REMOVAL)
                .details(Map.of(
                        "roleId", removeRequest.roleId().toString(),
                        "userId", removeRequest.userId().toString()))
                .log();

        // Get role information and current users BEFORE removal
        Role role = repository.findByIdAndTenantId(removeRequest.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

        // Get users who currently have this role BEFORE removal (for stateBefore)
        JsonNode stateBefore = createRemovalStateBefore(removeRequest, role);

        // Execute the method (perform the removal)
        Object result = pjp.proceed();

        try {
            // Create audit event for role removal
            createUserRoleRemovalAuditEvent(removeRequest, role, tenantId, stateBefore, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_USER_ROLE_REMOVAL)
                    .details(Map.of(
                            "roleId", removeRequest.roleId().toString(),
                            "userId", removeRequest.userId().toString(),
                            "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Creates audit event for user role removal.
     */
    private void createUserRoleRemovalAuditEvent(RemoveRoleRequest request, Role role, String tenantId, JsonNode stateBefore, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            
            // Create state after with remaining users who have the role AFTER removal
            JsonNode stateAfter = createRemovalStateAfter(request, role, result);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT_USER_ROLE_REMOVAL)
                    .details(Map.of(
                            "action", "revoke role",
                            "roleId", role.getId().toString(),
                            "roleName", role.getRoleName(),
                            "removedUserId", request.userId().toString(),
                            "userId", user.id(),
                            "username", user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "revoke role",
                    "role",
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    stateBefore,
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase()
            );

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_USER_ROLE_REMOVAL_AUDIT_EVENT)
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "removedUserId", request.userId().toString()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_USER_ROLE_REMOVAL_AUDIT_EVENT)
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "removedUserId", request.userId().toString(),
                            "error", e.getMessage()))
                    .log();
            throw e;
        }
    }

    /**
     * Creates the stateBefore for role removal - shows all users who had the role before removal.
     */
    private JsonNode createRemovalStateBefore(RemoveRoleRequest request, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();
        ArrayNode usersArray = objectMapper.createArrayNode();

        try {
            // Get current tenant users from the auth service
            String tenantId = tenantContextService.getCurrentTenantId();
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create map for quick username lookup
            Map<String, String> userIdToUsernameMap = tenantUsers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            UserInfo::getId,
                            UserInfo::getUsername,
                            (existing, replacement) -> existing // Handle duplicates by keeping first
                    ));

            // Get all users currently assigned to this role
            java.util.Set<String> currentAssignedUserIds = userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId).stream()
                    .map(userRole -> userRole.getUserId().toString())
                    .collect(java.util.stream.Collectors.toSet());

            // Add all currently assigned users to stateBefore
            for (String userId : currentAssignedUserIds) {
                ObjectNode userObj = objectMapper.createObjectNode();
                userObj.put("userId", userId);
                // Get actual username from the map, fallback to placeholder if not found
                String username = userIdToUsernameMap.getOrDefault(userId, "user_" + userId.substring(0, 8));
                userObj.put("userName", username);
                usersArray.add(userObj);
            }

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_REMOVAL_STATE_BEFORE_USING_FALLBACK)
                    .details(Map.of("error", e.getMessage()))
                    .log();
            
            // Fallback: Use role's user assignments with placeholder usernames
            String tenantId = tenantContextService.getCurrentTenantId();
            for (var userRole : userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId)) {
                ObjectNode userObj = objectMapper.createObjectNode();
                String userId = userRole.getUserId().toString();
                userObj.put("userId", userId);
                userObj.put("userName", "user_" + userId.substring(0, 8));
                usersArray.add(userObj);
            }
        }

        stateBefore.set("users", usersArray);
        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());

        return stateBefore;
    }

    /**
     * Creates the stateAfter for role removal - shows remaining users who have the role after removal.
     */
    private JsonNode createRemovalStateAfter(RemoveRoleRequest request, Role role, Object result) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        ArrayNode usersArray = objectMapper.createArrayNode();

        try {
            // Get current tenant users from the auth service for username lookup
            String tenantId = tenantContextService.getCurrentTenantId();
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create map for quick username lookup
            Map<String, String> userIdToUsernameMap = tenantUsers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            UserInfo::getId,
                            UserInfo::getUsername,
                            (existing, replacement) -> existing // Handle duplicates by keeping first
                    ));

            // Get all users currently assigned to this role (after removal)
            java.util.Set<String> remainingAssignedUserIds = userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId).stream()
                    .map(userRole -> userRole.getUserId().toString())
                    .filter(userId -> !userId.equals(request.userId().toString())) // Exclude the removed user
                    .collect(java.util.stream.Collectors.toSet());

            // Add remaining assigned users to stateAfter
            for (String userId : remainingAssignedUserIds) {
                ObjectNode userObj = objectMapper.createObjectNode();
                userObj.put("userId", userId);
                // Get actual username from the map, fallback to placeholder if not found
                String username = userIdToUsernameMap.getOrDefault(userId, "user_" + userId.substring(0, 8));
                userObj.put("userName", username);
                usersArray.add(userObj);
            }

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_REMOVAL_STATE_AFTER_USING_FALLBACK)
                    .details(Map.of("error", e.getMessage()))
                    .log();
            
            // Fallback: Use role's user assignments with placeholder usernames (excluding removed user)
            String tenantId = tenantContextService.getCurrentTenantId();
            for (var userRole : userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId)) {
                String userId = userRole.getUserId().toString();
                if (!userId.equals(request.userId().toString())) { // Exclude the removed user
                    ObjectNode userObj = objectMapper.createObjectNode();
                    userObj.put("userId", userId);
                    userObj.put("userName", "user_" + userId.substring(0, 8));
                    usersArray.add(userObj);
                }
            }
        }

        stateAfter.set("users", usersArray);
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());

        return stateAfter;
    }

    // ==================== BULK ROLE REVOCATION AUDIT METHODS ====================

    @Around("revokeRoleFromUsersPointcut()")
    public Object auditRevokeRoleFromUsers(ProceedingJoinPoint pjp) throws Throwable {
        logger.debug()
                .category(LOG_CATEGORY)
                .message(AppConstants.LOG_AUDIT_ASPECT_TRIGGERED_BULK_REVOCATION)
                .log();

        Object[] args = pjp.getArgs();
        RevokeRoleRequest request = (RevokeRoleRequest) args[0];
        
        // Get tenant ID and role information before execution
        String tenantId = tenantContextService.getCurrentTenantId();
        Role role = repository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND));

        // Create stateBefore - users who currently have this role
        JsonNode stateBefore = createBulkRevocationStateBefore(request, role, tenantId);

        try {
            // Execute the revocation
            Object result = pjp.proceed();
            
            // Create stateAfter - users who still have this role after revocation
            JsonNode stateAfter = createBulkRevocationStateAfter(request, role, tenantId, result);

            // Create and publish audit event
            createBulkRoleRevocationAuditEvent(request, role, tenantId, stateBefore, stateAfter, result);
            
            return result;
            
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_COMPLETE_BULK_REVOCATION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString(),
                            AppConstants.DETAIL_KEY_USER_COUNT_VALUE, request.getUserCount(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            throw e;
        }
    }

    private JsonNode createBulkRevocationStateBefore(RevokeRoleRequest request, Role role, String tenantId) {
        ObjectNode stateBefore = objectMapper.createObjectNode();
        ArrayNode usersArray = objectMapper.createArrayNode();

        try {
            // Get all users who currently have this role
            List<UUID> usersWithRole = userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId)
                    .stream()
                    .map(userRole -> userRole.getUserId())
                    .collect(java.util.stream.Collectors.toList());

            // Get tenant users for username lookup
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, String> userIdToUsernameMap = tenantUsers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            UserInfo::getId,
                            UserInfo::getUsername,
                            (existing, replacement) -> existing
                    ));

            // Add users who currently have the role
            for (UUID userId : usersWithRole) {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("userId", userId.toString());
                userNode.put("userName", userIdToUsernameMap.getOrDefault(userId.toString(), "unknown"));
                usersArray.add(userNode);
            }

            stateBefore.set("users", usersArray);
            stateBefore.put("roleId", role.getId().toString());
            stateBefore.put("roleName", role.getRoleName());
            stateBefore.put("userCount", usersWithRole.size());

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_STATE_BEFORE_BULK_REVOCATION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return stateBefore;
    }

    private JsonNode createBulkRevocationStateAfter(RevokeRoleRequest request, Role role, String tenantId, Object result) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        ArrayNode usersArray = objectMapper.createArrayNode();

        try {
            // Get users who still have this role after revocation
            List<UUID> usersWithRole = userRoleRepository.findByRoleIdAndTenantId(role.getId(), tenantId)
                    .stream()
                    .map(userRole -> userRole.getUserId())
                    .collect(java.util.stream.Collectors.toList());

            // Get tenant users for username lookup
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, String> userIdToUsernameMap = tenantUsers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            UserInfo::getId,
                            UserInfo::getUsername,
                            (existing, replacement) -> existing
                    ));

            // Add users who still have the role
            for (UUID userId : usersWithRole) {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("userId", userId.toString());
                userNode.put("userName", userIdToUsernameMap.getOrDefault(userId.toString(), "unknown"));
                usersArray.add(userNode);
            }

            stateAfter.set("users", usersArray);
            stateAfter.put("roleId", role.getId().toString());
            stateAfter.put("roleName", role.getRoleName());
            stateAfter.put("userCount", usersWithRole.size());

            // Add revocation summary from result
            if (result instanceof BulkRoleRevocationResponse response) {
                ObjectNode revocationSummary = objectMapper.createObjectNode();
                revocationSummary.put("totalRequested", response.totalRequested());
                revocationSummary.put("totalSuccessful", response.totalSuccessful());
                revocationSummary.put("totalFailed", response.totalFailed());
                
                ArrayNode requestedUsersArray = objectMapper.createArrayNode();
                for (UUID userId : response.requestedUsers()) {
                    requestedUsersArray.add(userId.toString());
                }
                revocationSummary.set("requestedUsers", requestedUsersArray);
                
                stateAfter.set("revocationSummary", revocationSummary);
            }

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_STATE_AFTER_BULK_REVOCATION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return stateAfter;
    }

    private void createBulkRoleRevocationAuditEvent(RevokeRoleRequest request, Role role, String tenantId, 
                                                   JsonNode stateBefore, JsonNode stateAfter, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_BULK_REVOCATION)
                    .details(Map.of(
                            "action", "revoke role",
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_COUNT_VALUE, request.getUserCount(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "revoke role",
                    AppConstants.ENTITY_TYPE_ROLE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    stateBefore,
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase()
            );

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SUCCESSFULLY_PUBLISHED_BULK_REVOCATION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_USER_COUNT_VALUE, request.getUserCount()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_BULK_REVOCATION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_USER_COUNT_VALUE, request.getUserCount(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }
    }

    // ==================== PERMISSION ASSIGNMENT AUDIT METHODS ====================

    @Around("assignPermissionToRolePointcut()")
    public Object auditAssignPermissionToRole(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_PERMISSION_ASSIGNMENT)
                .log();

        UUID roleId = (UUID) pjp.getArgs()[0];
        AssignPermissionToRoleRequest permissionRequest = (AssignPermissionToRoleRequest) pjp.getArgs()[1];
        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_PERMISSION_ASSIGNMENT)
                .details(Map.of(
                        AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                        AppConstants.DETAIL_KEY_ACTION_ID, permissionRequest.getActionId().toString(),
                        AppConstants.DETAIL_KEY_HAS_CONDITION, String.valueOf(permissionRequest.hasConditionalRule())))
                .log();

        // Get role information
        Role role = repository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED));

        // Execute the method (perform the permission assignment)
        Object result = pjp.proceed();

        try {
            // Create audit event for permission assignment
            createPermissionAssignmentAuditEvent(roleId, permissionRequest, role, tenantId, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_ASSIGNMENT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, permissionRequest.getActionId().toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Audit aspect for bulk permission assignment to role.
     * Creates a single audit event representing the entire bulk operation.
     */
    @Around("assignPermissionsBulkPointcut()")
    public Object auditAssignPermissionsBulk(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message("Audit aspect triggered for bulk permission assignment")
                .log();

        UUID roleId = (UUID) pjp.getArgs()[0];
        BulkAssignPermissionRequest bulkRequest = (BulkAssignPermissionRequest) pjp.getArgs()[1];
        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message("Auditing bulk permission assignment")
                .details(Map.of(
                        "roleId", roleId.toString(),
                        "assignmentCount", String.valueOf(bulkRequest.getAssignmentCount()),
                        "tenantId", tenantId))
                .log();

        // Get role information
        Role role = repository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found or access denied"));

        // Execute the method (perform the bulk permission assignment)
        Object result = pjp.proceed();

        try {
            // Create audit event for bulk permission assignment
            createBulkPermissionAssignmentAuditEvent(roleId, bulkRequest, role, tenantId, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Failed to create audit event for bulk permission assignment")
                    .details(Map.of(
                            "roleId", roleId.toString(),
                            "assignmentCount", String.valueOf(bulkRequest.getAssignmentCount()),
                            "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Creates audit event for permission assignment to role.
     */
    private void createPermissionAssignmentAuditEvent(UUID roleId, AssignPermissionToRoleRequest request, Role role, String tenantId, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            
            // Create state after with permission hierarchy information
            JsonNode stateAfter = createPermissionAssignmentStateAfter(request, role, result);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT_PERMISSION_ASSIGNMENT)
                    .details(Map.of(
                            "action", "assign permission",
                            "roleId", role.getId().toString(),
                            "roleName", role.getRoleName(),
                            "actionId", request.getActionId().toString(),
                            "userId", user.id(),
                            "username", user.username()))
                    .log();

            // Create target object with role information
            ObjectNode targetObject = objectMapper.createObjectNode();
            targetObject.put("type", "role");
            targetObject.put("objectId", role.getId().toString());
            targetObject.put("objectName", role.getRoleName());

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "assign permission",
                    "role",
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    null, // stateBefore is null for permission assignment
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase()
            );

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_ASSIGNMENT_AUDIT_EVENT)
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "actionId", request.getActionId().toString()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_ASSIGNMENT_AUDIT_EVENT)
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "actionId", request.getActionId().toString(),
                            "error", e.getMessage()))
                    .log();
            throw e;
        }
    }

    @Around("deletePermissionFromRolePointcut()")
    public Object auditDeletePermissionFromRole(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        UUID roleId = (UUID) args[0];
        UUID actionId = (UUID) args[1];

        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.LOG_PROCESSING_PERMISSION_DELETION)
                .details(Map.of(
                        AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                        AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                .log();

        // Get role information
        Role role = repository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED));

        // IMPORTANT: Create stateBefore BEFORE deleting the permission
        JsonNode stateBefore = createPermissionDeletionStateBefore(actionId, role);

        // Execute the method (perform the permission deletion)
        Object result = pjp.proceed();

        try {
            // Create audit event for permission deletion
            createPermissionDeletionAuditEvent(roleId, actionId, role, tenantId, stateBefore, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_DELETION)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Creates audit event for permission deletion from role.
     */
    private void createPermissionDeletionAuditEvent(UUID roleId, UUID actionId, Role role, String tenantId, JsonNode stateBefore, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_PERMISSION_DELETION)
                    .details(Map.of(
                            "action", "delete permission",
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            publishRoleAuditEvent(
                    "delete permission",
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    stateBefore, // State before deletion
                    null, // State after deletion (null because permission is deleted)
                    role.getRiskLevel().name().toLowerCase()
            );

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_DELETION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_DELETION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            throw e;
        }
    }

    @Around("updatePermissionForRolePointcut()")
    public Object auditUpdatePermissionForRole(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        UUID roleId = (UUID) args[0];
        UUID actionId = (UUID) args[1];
        UpdatePermissionRequest updateRequest = (UpdatePermissionRequest) args[2];

        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.LOG_PROCESSING_PERMISSION_UPDATE)
                .details(Map.of(
                        AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                        AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                .log();

        // Get role information
        Role role = repository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED));

        // IMPORTANT: Create stateBefore BEFORE updating the permission
        JsonNode stateBefore = createPermissionUpdateStateBefore(actionId, role);

        // Execute the method (perform the permission update)
        Object result = pjp.proceed();

        try {
            // Create stateAfter AFTER updating the permission
            JsonNode stateAfter = createPermissionUpdateStateAfter(actionId, role, updateRequest);
            
            // Create audit event for permission update
            createPermissionUpdateAuditEvent(roleId, actionId, role, tenantId, stateBefore, stateAfter, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_UPDATE)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Creates audit event for permission update in role.
     */
    private void createPermissionUpdateAuditEvent(UUID roleId, UUID actionId, Role role, String tenantId, JsonNode stateBefore, JsonNode stateAfter, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_PERMISSION_UPDATE)
                    .details(Map.of(
                            "action", "update permission",
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            publishRoleAuditEvent(
                    "update permission",
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    stateBefore, // State before update
                    stateAfter,   // State after update
                    role.getRiskLevel().name().toLowerCase()
            );

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_UPDATE_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_UPDATE_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString(),
                            AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            throw e;
        }
    }

    /**
     * Creates audit event for bulk permission assignment to role.
     * Creates a single consolidated audit event showing all permissions assigned.
     */
    private void createBulkPermissionAssignmentAuditEvent(UUID roleId, BulkAssignPermissionRequest bulkRequest, Role role, String tenantId, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            
            // Create state after with bulk permission hierarchy information
            JsonNode stateAfter = createBulkPermissionAssignmentStateAfter(bulkRequest, role, result);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message("Creating audit event for bulk permission assignment")
                    .details(Map.of(
                            "action", "assign permission",
                            "operationType", "bulk",
                            "roleId", role.getId().toString(),
                            "roleName", role.getRoleName(),
                            "assignmentCount", String.valueOf(bulkRequest.getAssignmentCount()),
                            "userId", user.id(),
                            "username", user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "assign permission",
                    "role",
                    role.getId().toString(),
                    role.getRoleName() + " (Bulk: " + bulkRequest.getAssignmentCount() + " permissions)",
                    tenantId,
                    null, // stateBefore is null for permission assignment
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase()
            );

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message("Successfully published bulk permission assignment audit event")
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "assignmentCount", String.valueOf(bulkRequest.getAssignmentCount())))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Failed to create or publish bulk permission assignment audit event")
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "assignmentCount", String.valueOf(bulkRequest.getAssignmentCount()),
                            "error", e.getMessage()))
                    .log();
        }
    }

    /**
     * Audit aspect for bulk permission revocation from role.
     * Creates a single audit event representing the entire bulk revocation operation.
     */
    @Around("revokePermissionsBulkPointcut()")
    public Object auditRevokePermissionsBulk(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message("Audit aspect triggered for bulk permission revocation")
                .log();

        UUID roleId = (UUID) pjp.getArgs()[0];
        BulkRevokePermissionRequest bulkRequest = (BulkRevokePermissionRequest) pjp.getArgs()[1];
        String tenantId = tenantContextService.getCurrentTenantId();

        logger.info()
                .category(LOG_CATEGORY)
                .message("Auditing bulk permission revocation")
                .details(Map.of(
                        "roleId", roleId.toString(),
                        "revocationCount", String.valueOf(bulkRequest.getRevocationCount()),
                        "tenantId", tenantId))
                .log();

        // Get role information
        Role role = repository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found or access denied"));

        // IMPORTANT: Create stateBefore BEFORE revoking the permissions
        JsonNode stateBefore = createBulkPermissionRevocationStateBefore(bulkRequest, role);

        // Execute the method (perform the bulk permission revocation)
        Object result = pjp.proceed();

        try {
            // Create audit event for bulk permission revocation
            createBulkPermissionRevocationAuditEvent(roleId, bulkRequest, role, tenantId, stateBefore, result);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Failed to create audit event for bulk permission revocation")
                    .details(Map.of(
                            "roleId", roleId.toString(),
                            "revocationCount", String.valueOf(bulkRequest.getRevocationCount()),
                            "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    /**
     * Creates audit event for bulk permission revocation from role.
     */
    private void createBulkPermissionRevocationAuditEvent(UUID roleId, BulkRevokePermissionRequest bulkRequest, Role role, String tenantId, JsonNode stateBefore, Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message("Creating audit event for bulk permission revocation")
                    .details(Map.of(
                            "action", "delete permission",
                            "operationType", "bulk",
                            "roleId", role.getId().toString(),
                            "roleName", role.getRoleName(),
                            "revocationCount", String.valueOf(bulkRequest.getRevocationCount()),
                            "userId", user.id(),
                            "username", user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    "delete permission",
                    "role",
                    role.getId().toString(),
                    role.getRoleName() + " (Bulk Revoke: " + bulkRequest.getRevocationCount() + " permissions)",
                    tenantId,
                    stateBefore, // State before revocation
                    null, // stateAfter is null since permissions are deleted
                    role.getRiskLevel().name().toLowerCase()
            );

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message("Successfully published bulk permission revocation audit event")
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "revocationCount", String.valueOf(bulkRequest.getRevocationCount())))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Failed to create or publish bulk permission revocation audit event")
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "revocationCount", String.valueOf(bulkRequest.getRevocationCount()),
                            "error", e.getMessage()))
                    .log();
        }
    }

    /**
     * Creates the stateBefore for bulk permission revocation.
     * Shows all permissions being revoked in a hierarchical structure grouped by module and page.
     */
    private JsonNode createBulkPermissionRevocationStateBefore(BulkRevokePermissionRequest bulkRequest, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();

        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());
        stateBefore.put("operationType", "bulk_revoke");
        stateBefore.put("totalRevocations", bulkRequest.getRevocationCount());

        try {
            // Group permissions by module and page
            Map<String, Map<String, List<ObjectNode>>> modulePageActionMap = new LinkedHashMap<>();

            for (UUID actionId : bulkRequest.getActionIds()) {
                try {
                    // Get action details
                    Action action = actionRepository.findById(actionId).orElse(null);
                    if (action == null) {
                        logger.warn()
                                .category(LOG_CATEGORY)
                                .message("Action not found for bulk revocation audit")
                                .details(Map.of("actionId", actionId.toString()))
                                .log();
                        continue;
                    }

                    // Get permission details (justification, condition)
                    Permission permission = permissionRepository.findByRoleIdAndActionId(role.getId(), actionId).orElse(null);
                    List<ConditionalRule> conditionalRules = null;
                    if (permission != null) {
                        conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                    }

                    // Create action details node
                    ObjectNode actionNode = objectMapper.createObjectNode();
                    actionNode.put("actionId", action.getId().toString());
                    actionNode.put("actionName", action.getActionName());
                    actionNode.put("method", action.getMethod().name());
                    actionNode.put("endpoint", action.getEndpoint());

                    if (conditionalRules != null && !conditionalRules.isEmpty()) {
                        ConditionalRule conditionalRule = conditionalRules.get(0);
                        actionNode.put("hasConditionalRule", true);
                        actionNode.put("justification", conditionalRule.getJustification() != null ? conditionalRule.getJustification() : "");
                        actionNode.put("conditionScript", conditionalRule.getConditionScript() != null ? conditionalRule.getConditionScript() : "");
                    } else {
                        actionNode.put("hasConditionalRule", false);
                    }

                    // Get module and page information
                    Page page = pageRepository.findById(action.getPageId()).orElse(null);
                    if (page != null) {
                        Module module = moduleRepository.findById(page.getModuleId()).orElse(null);
                        if (module != null) {
                            String moduleName = module.getModuleName();
                            String pageName = page.getPageName();

                            modulePageActionMap
                                    .computeIfAbsent(moduleName, k -> new LinkedHashMap<>())
                                    .computeIfAbsent(pageName, k -> new ArrayList<>())
                                    .add(actionNode);
                        }
                    }

                } catch (Exception e) {
                    logger.error(e)
                            .category(LOG_CATEGORY)
                            .message("Error processing action for bulk revocation audit")
                            .details(Map.of("actionId", actionId.toString(), "error", e.getMessage()))
                            .log();
                }
            }

            // Build hierarchical structure
            ArrayNode modulesArray = objectMapper.createArrayNode();
            for (Map.Entry<String, Map<String, List<ObjectNode>>> moduleEntry : modulePageActionMap.entrySet()) {
                ObjectNode moduleNode = objectMapper.createObjectNode();
                moduleNode.put("moduleName", moduleEntry.getKey());

                ArrayNode pagesArray = objectMapper.createArrayNode();
                for (Map.Entry<String, List<ObjectNode>> pageEntry : moduleEntry.getValue().entrySet()) {
                    ObjectNode pageNode = objectMapper.createObjectNode();
                    pageNode.put("pageName", pageEntry.getKey());

                    ArrayNode actionsArray = objectMapper.createArrayNode();
                    pageEntry.getValue().forEach(actionsArray::add);
                    pageNode.set("actions", actionsArray);

                    pagesArray.add(pageNode);
                }
                moduleNode.set("pages", pagesArray);

                modulesArray.add(moduleNode);
            }

            stateBefore.set("permissions", modulesArray);

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Error creating bulk revocation stateBefore")
                    .details(Map.of(
                            "roleId", role.getId().toString(),
                            "error", e.getMessage()))
                    .log();
        }

        return stateBefore;
    }

    /**
     * Creates the stateAfter for bulk permission assignment.
     * Shows all permissions assigned in a hierarchical structure grouped by module and page.
     */
    private JsonNode createBulkPermissionAssignmentStateAfter(BulkAssignPermissionRequest bulkRequest, Role role, Object result) {
        ObjectNode stateAfter = objectMapper.createObjectNode();

        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());
        stateAfter.put("operationType", "bulk");
        stateAfter.put("totalAssignments", bulkRequest.getAssignmentCount());

        try {
            // Extract result information
            BulkAssignmentResult bulkResult = (BulkAssignmentResult) result;
            stateAfter.put("successfulAssignments", bulkResult.getAssignedCount());
            stateAfter.put("success", bulkResult.isSuccess());

            // Group assignments by module and page
            Map<String, Map<String, List<ObjectNode>>> modulePageActionMap = new LinkedHashMap<>();

            for (BulkAssignmentItem item : bulkRequest.getAssignments()) {
                try {
                    // Get action details
                    Action action = actionRepository.findById(item.getActionId()).orElse(null);
                    if (action == null) {
                        logger.warn()
                                .category(LOG_CATEGORY)
                                .message("Action not found for bulk assignment audit")
                                .details(Map.of("actionId", item.getActionId().toString()))
                                .log();
                        continue;
                    }

                    // Get page and module details
                    String moduleName = "Unknown Module";
                    String pageName = "Unknown Page";
                    
                    if (action.getPageId() != null) {
                        Page page = pageRepository.findById(action.getPageId()).orElse(null);
                        if (page != null) {
                            pageName = page.getPageName();
                            if (page.getModuleId() != null) {
                                Module module = moduleRepository.findById(page.getModuleId()).orElse(null);
                                if (module != null) {
                                    moduleName = module.getModuleName();
                                }
                            }
                        }
                    }

                    // Create action object
                    ObjectNode actionObj = objectMapper.createObjectNode();
                    actionObj.put("id", action.getId().toString());
                    actionObj.put("name", action.getActionName());
                    actionObj.put("httpMethod", action.getMethod().name());
                    actionObj.put("justification", item.getJustification());
                    if (item.hasConditionalRule()) {
                        actionObj.put("conditionScript", item.getConditionScript());
                    }

                    // Group by module and page
                    modulePageActionMap
                            .computeIfAbsent(moduleName, k -> new LinkedHashMap<>())
                            .computeIfAbsent(pageName, k -> new ArrayList<>())
                            .add(actionObj);

                } catch (Exception e) {
                    logger.warn()
                            .category(LOG_CATEGORY)
                            .message("Failed to process action for bulk assignment audit")
                            .details(Map.of(
                                    "actionId", item.getActionId().toString(),
                                    "error", e.getMessage()))
                            .log();
                }
            }

            // Build modules array with hierarchical structure
            ArrayNode modulesArray = objectMapper.createArrayNode();
            for (Map.Entry<String, Map<String, List<ObjectNode>>> moduleEntry : modulePageActionMap.entrySet()) {
                ObjectNode moduleObj = objectMapper.createObjectNode();
                moduleObj.put("moduleName", moduleEntry.getKey());

                ArrayNode pagesArray = objectMapper.createArrayNode();
                for (Map.Entry<String, List<ObjectNode>> pageEntry : moduleEntry.getValue().entrySet()) {
                    ObjectNode pageObj = objectMapper.createObjectNode();
                    pageObj.put("pageName", pageEntry.getKey());

                    ArrayNode actionsArray = objectMapper.createArrayNode();
                    pageEntry.getValue().forEach(actionsArray::add);
                    pageObj.set("actions", actionsArray);

                    pagesArray.add(pageObj);
                }
                moduleObj.set("pages", pagesArray);
                modulesArray.add(moduleObj);
            }

            stateAfter.set("modules", modulesArray);

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message("Failed to fetch permission hierarchy for bulk assignment audit")
                    .details(Map.of("error", e.getMessage()))
                    .log();
            
            // Add fallback information
            stateAfter.put("error", "Failed to fetch full hierarchy: " + e.getMessage());
            ArrayNode simpleActions = objectMapper.createArrayNode();
            for (BulkAssignmentItem item : bulkRequest.getAssignments()) {
                ObjectNode actionObj = objectMapper.createObjectNode();
                actionObj.put("actionId", item.getActionId().toString());
                actionObj.put("justification", item.getJustification());
                if (item.hasConditionalRule()) {
                    actionObj.put("conditionScript", item.getConditionScript());
                }
                simpleActions.add(actionObj);
            }
            stateAfter.set("assignments", simpleActions);
        }

        return stateAfter;
    }

    /**
     * Creates the stateAfter for permission assignment - shows the permission hierarchy with action details.
     */
    private JsonNode createPermissionAssignmentStateAfter(AssignPermissionToRoleRequest request, Role role, Object result) {
        // Create a new ObjectNode that will preserve insertion order
        ObjectNode stateAfter = objectMapper.createObjectNode();

        // CRITICAL: Add fields in the exact order we want them to appear
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());

        try {
            // Get action details
            Action action = actionRepository.findById(request.getActionId()).orElse(null);
            if (action == null) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_ACTION_NOT_FOUND_FOR_AUDIT)
                        .details(Map.of(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString()))
                        .log();
                return createFallbackPermissionAssignmentState(request, role);
            }

            // Get the SAVED justification and conditionScript from the database (AFTER the assignment)
            String savedJustification = "";
            String savedConditionScript = "";
            
            // Find the Permission for this specific roleId and actionId
            Optional<Permission> permissionOpt = permissionRepository.findByRoleIdAndActionId(role.getId(), request.getActionId());
            if (permissionOpt.isPresent()) {
                Permission permission = permissionOpt.get();
                
                // Find the ConditionalRule for this permission
                List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                if (!conditionalRules.isEmpty()) {
                    ConditionalRule rule = conditionalRules.get(0); // Should be only one
                    savedJustification = rule.getJustification() != null ? rule.getJustification() : "";
                    savedConditionScript = rule.getConditionScript() != null ? rule.getConditionScript() : "";
                }
            }

            // Get page details if action has a pageId
            Page page = null;
            Module module = null;
            if (action.getPageId() != null) {
                page = pageRepository.findById(action.getPageId()).orElse(null);
                if (page != null && page.getModuleId() != null) {
                    module = moduleRepository.findById(page.getModuleId()).orElse(null);
                }
            }

            // Create modules array - ADD THIS IMMEDIATELY AFTER roleName
            ArrayNode modulesArray = objectMapper.createArrayNode();
            
            if (module != null && page != null) {
                ObjectNode moduleObj = objectMapper.createObjectNode();
                moduleObj.put("id", module.getId().toString());
                moduleObj.put("name", module.getModuleName());

                // Create pages array
                ArrayNode pagesArray = objectMapper.createArrayNode();
                ObjectNode pageObj = objectMapper.createObjectNode();
                pageObj.put("id", page.getId().toString());
                pageObj.put("name", page.getPageName());

                // Create actions array
                ArrayNode actionsArray = objectMapper.createArrayNode();
                ObjectNode actionObj = objectMapper.createObjectNode();
                actionObj.put("id", action.getId().toString());
                actionObj.put("name", action.getActionName());
                actionObj.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                actionObj.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the SAVED values from the database (after the assignment)
                actionObj.put("justification", savedJustification);
                actionObj.put("conditionScript", savedConditionScript);

                actionsArray.add(actionObj);
                pageObj.set("actions", actionsArray);
                pagesArray.add(pageObj);
                moduleObj.set("pages", pagesArray);
                modulesArray.add(moduleObj);
            } else {
                // Create a fallback structure without module/page hierarchy
                ObjectNode standaloneAction = objectMapper.createObjectNode();
                standaloneAction.put("id", action.getId().toString());
                standaloneAction.put("name", action.getActionName());
                standaloneAction.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                standaloneAction.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the SAVED values from the database (after the assignment)
                standaloneAction.put("justification", savedJustification);
                standaloneAction.put("conditionScript", savedConditionScript);

                ArrayNode actionsArray = objectMapper.createArrayNode();
                actionsArray.add(standaloneAction);
                stateAfter.set("actions", actionsArray);
            }

            // Add modules array IMMEDIATELY after roleName - this is the key!
            stateAfter.set("modules", modulesArray);

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_AUDIT)
                    .details(Map.of("error", e.getMessage()))
                    .log();
            
            return createFallbackPermissionAssignmentState(request, role);
        }

        return stateAfter;
    }

    /**
     * Creates a fallback state for permission assignment when hierarchy lookup fails.
     */
    private JsonNode createFallbackPermissionAssignmentState(AssignPermissionToRoleRequest request, Role role) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        // Add role information in the exact order we want
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());
        
        // Add empty modules array right after role info to maintain order  
        stateAfter.set("modules", objectMapper.createArrayNode());
        
        // Create minimal action information
        ObjectNode actionObj = objectMapper.createObjectNode();
        actionObj.put("id", request.getActionId().toString());
        actionObj.put("name", "Unknown Action");
        actionObj.put("justification", request.getJustification());
        actionObj.put("conditionScript", request.getConditionScript() != null ? request.getConditionScript() : null);

        ArrayNode actionsArray = objectMapper.createArrayNode();
        actionsArray.add(actionObj);
        
        stateAfter.set("actions", actionsArray);

        return stateAfter;
    }

    /**
     * Creates the stateBefore for permission deletion - shows the permission hierarchy with action details
     * that will be deleted.
     */
    private JsonNode createPermissionDeletionStateBefore(UUID actionId, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();

        // Add role information in the exact order we want
        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());

        try {
            // Get action details
            Action action = actionRepository.findById(actionId).orElse(null);
            if (action == null) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_ACTION_NOT_FOUND_FOR_DELETION_AUDIT)
                        .details(Map.of(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                        .log();
                return createFallbackPermissionDeletionState(actionId, role);
            }

            // Get page details if action has a pageId
            Page page = null;
            Module module = null;
            if (action.getPageId() != null) {
                page = pageRepository.findById(action.getPageId()).orElse(null);
                if (page != null && page.getModuleId() != null) {
                    module = moduleRepository.findById(page.getModuleId()).orElse(null);
                }
            }

            // Create modules array - ADD THIS IMMEDIATELY AFTER roleName
            ArrayNode modulesArray = objectMapper.createArrayNode();
            
            if (module != null && page != null) {
                ObjectNode moduleObj = objectMapper.createObjectNode();
                moduleObj.put("id", module.getId().toString());
                moduleObj.put("name", module.getModuleName());

                // Create pages array
                ArrayNode pagesArray = objectMapper.createArrayNode();
                ObjectNode pageObj = objectMapper.createObjectNode();
                pageObj.put("id", page.getId().toString());
                pageObj.put("name", page.getPageName());

                // Create actions array
                ArrayNode actionsArray = objectMapper.createArrayNode();
                ObjectNode actionObj = objectMapper.createObjectNode();
                actionObj.put("id", action.getId().toString());
                actionObj.put("name", action.getActionName());
                actionObj.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                actionObj.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // For deletion, we don't have justification and conditionScript from request, 
                // but we can put empty values or get them from existing permission if stored
                actionObj.put("justification", "");
                actionObj.put("conditionScript", "");

                actionsArray.add(actionObj);
                pageObj.set("actions", actionsArray);
                pagesArray.add(pageObj);
                moduleObj.set("pages", pagesArray);
                modulesArray.add(moduleObj);
            } else {
                // Create a fallback structure without module/page hierarchy
                ObjectNode standaloneAction = objectMapper.createObjectNode();
                standaloneAction.put("id", action.getId().toString());
                standaloneAction.put("name", action.getActionName());
                standaloneAction.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                standaloneAction.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                standaloneAction.put("justification", "");
                standaloneAction.put("conditionScript", "");

                ArrayNode actionsArray = objectMapper.createArrayNode();
                actionsArray.add(standaloneAction);
                stateBefore.set("actions", actionsArray);
            }

            // Add modules array IMMEDIATELY after roleName
            stateBefore.set("modules", modulesArray);

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_DELETION)
                    .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            
            return createFallbackPermissionDeletionState(actionId, role);
        }

        return stateBefore;
    }

    /**
     * Creates a fallback state for permission deletion when hierarchy lookup fails.
     */
    private JsonNode createFallbackPermissionDeletionState(UUID actionId, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();
        
        // Add role information in the exact order we want
        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());
        
        // Add empty modules array right after role info to maintain order  
        stateBefore.set("modules", objectMapper.createArrayNode());
        
        // Create minimal action information
        ObjectNode actionObj = objectMapper.createObjectNode();
        actionObj.put("id", actionId.toString());
        actionObj.put("name", "Unknown Action");
        actionObj.put("justification", "");
        actionObj.put("conditionScript", "");

        ArrayNode actionsArray = objectMapper.createArrayNode();
        actionsArray.add(actionObj);
        
        stateBefore.set("actions", actionsArray);

        return stateBefore;
    }

    /**
     * Creates the stateBefore for permission update - shows the permission hierarchy with current action details
     * before the update.
     */
    private JsonNode createPermissionUpdateStateBefore(UUID actionId, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();

        // Add role information in the exact order we want
        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());

        try {
            // Get action details
            Action action = actionRepository.findById(actionId).orElse(null);
            if (action == null) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message("Action not found for permission update audit, using minimal information")
                        .details(Map.of("actionId", actionId.toString()))
                        .log();
                return createFallbackPermissionUpdateStateBefore(actionId, role);
            }

            // Get the current justification and conditionScript from ConditionalRule
            String currentJustification = "";
            String currentConditionScript = "";
            
            // Find the Permission for this specific roleId and actionId
            Optional<Permission> permissionOpt = permissionRepository.findByRoleIdAndActionId(role.getId(), actionId);
            if (permissionOpt.isPresent()) {
                Permission permission = permissionOpt.get();
                
                // Find the ConditionalRule for this permission
                List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                if (!conditionalRules.isEmpty()) {
                    ConditionalRule rule = conditionalRules.get(0); // Should be only one
                    currentJustification = rule.getJustification() != null ? rule.getJustification() : "";
                    currentConditionScript = rule.getConditionScript() != null ? rule.getConditionScript() : "";
                }
            }

            // Get page details if action has a pageId
            Page page = null;
            Module module = null;
            if (action.getPageId() != null) {
                page = pageRepository.findById(action.getPageId()).orElse(null);
                if (page != null && page.getModuleId() != null) {
                    module = moduleRepository.findById(page.getModuleId()).orElse(null);
                }
            }

            // Create modules array - ADD THIS IMMEDIATELY AFTER roleName
            ArrayNode modulesArray = objectMapper.createArrayNode();
            
            if (module != null && page != null) {
                ObjectNode moduleObj = objectMapper.createObjectNode();
                moduleObj.put("id", module.getId().toString());
                moduleObj.put("name", module.getModuleName());

                // Create pages array
                ArrayNode pagesArray = objectMapper.createArrayNode();
                ObjectNode pageObj = objectMapper.createObjectNode();
                pageObj.put("id", page.getId().toString());
                pageObj.put("name", page.getPageName());

                // Create actions array
                ArrayNode actionsArray = objectMapper.createArrayNode();
                ObjectNode actionObj = objectMapper.createObjectNode();
                actionObj.put("id", action.getId().toString());
                actionObj.put("name", action.getActionName());
                actionObj.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                actionObj.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the CURRENT values before update
                actionObj.put("justification", currentJustification);
                actionObj.put("conditionScript", currentConditionScript);

                actionsArray.add(actionObj);
                pageObj.set("actions", actionsArray);
                pagesArray.add(pageObj);
                moduleObj.set("pages", pagesArray);
                modulesArray.add(moduleObj);
            } else {
                // Create a fallback structure without module/page hierarchy
                ObjectNode standaloneAction = objectMapper.createObjectNode();
                standaloneAction.put("id", action.getId().toString());
                standaloneAction.put("name", action.getActionName());
                standaloneAction.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                standaloneAction.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the CURRENT values before update
                standaloneAction.put("justification", currentJustification);
                standaloneAction.put("conditionScript", currentConditionScript);

                ArrayNode actionsArray = objectMapper.createArrayNode();
                actionsArray.add(standaloneAction);
                stateBefore.set("actions", actionsArray);
            }

            // Add modules array IMMEDIATELY after roleName
            stateBefore.set("modules", modulesArray);

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_UPDATE_STATE_BEFORE)
                    .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            
            return createFallbackPermissionUpdateStateBefore(actionId, role);
        }

        return stateBefore;
    }

    /**
     * Creates a fallback state for permission update stateBefore when hierarchy lookup fails.
     */
    private JsonNode createFallbackPermissionUpdateStateBefore(UUID actionId, Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();
        
        // Add role information in the exact order we want
        stateBefore.put("roleId", role.getId().toString());
        stateBefore.put("roleName", role.getRoleName());
        
        // Add empty modules array right after role info to maintain order  
        stateBefore.set("modules", objectMapper.createArrayNode());
        
        // Create minimal action information
        ObjectNode actionObj = objectMapper.createObjectNode();
        actionObj.put("id", actionId.toString());
        actionObj.put("name", "Unknown Action");
        actionObj.put("justification", "");
        actionObj.put("conditionScript", "");

        ArrayNode actionsArray = objectMapper.createArrayNode();
        actionsArray.add(actionObj);
        
        stateBefore.set("actions", actionsArray);

        return stateBefore;
    }

    /**
     * Creates the stateAfter for permission update - shows the permission hierarchy with updated action details
     * after the update.
     */
    private JsonNode createPermissionUpdateStateAfter(UUID actionId, Role role, UpdatePermissionRequest updateRequest) {
        ObjectNode stateAfter = objectMapper.createObjectNode();

        // Add role information in the exact order we want
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());

        try {
            // Get action details
            Action action = actionRepository.findById(actionId).orElse(null);
            if (action == null) {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_ACTION_NOT_FOUND_FOR_PERMISSION_UPDATE_AUDIT)
                        .details(Map.of(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString()))
                        .log();
                return createFallbackPermissionUpdateState(actionId, role, updateRequest);
            }

            // Get page details if action has a pageId
            Page page = null;
            Module module = null;
            if (action.getPageId() != null) {
                page = pageRepository.findById(action.getPageId()).orElse(null);
                if (page != null && page.getModuleId() != null) {
                    module = moduleRepository.findById(page.getModuleId()).orElse(null);
                }
            }

            // Create modules array - ADD THIS IMMEDIATELY AFTER roleName
            ArrayNode modulesArray = objectMapper.createArrayNode();
            
            if (module != null && page != null) {
                ObjectNode moduleObj = objectMapper.createObjectNode();
                moduleObj.put("id", module.getId().toString());
                moduleObj.put("name", module.getModuleName());

                // Create pages array
                ArrayNode pagesArray = objectMapper.createArrayNode();
                ObjectNode pageObj = objectMapper.createObjectNode();
                pageObj.put("id", page.getId().toString());
                pageObj.put("name", page.getPageName());

                // Create actions array
                ArrayNode actionsArray = objectMapper.createArrayNode();
                ObjectNode actionObj = objectMapper.createObjectNode();
                actionObj.put("id", action.getId().toString());
                actionObj.put("name", action.getActionName());
                actionObj.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                actionObj.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the request data (what the user wants to update to)
                actionObj.put("justification", updateRequest.getJustification() != null ? updateRequest.getJustification() : "");
                actionObj.put("conditionScript", updateRequest.getConditionScript() != null ? updateRequest.getConditionScript() : "");

                actionsArray.add(actionObj);
                pageObj.set("actions", actionsArray);
                pagesArray.add(pageObj);
                moduleObj.set("pages", pagesArray);
                modulesArray.add(moduleObj);
            } else {
                // Create a fallback structure without module/page hierarchy
                ObjectNode standaloneAction = objectMapper.createObjectNode();
                standaloneAction.put("id", action.getId().toString());
                standaloneAction.put("name", action.getActionName());
                standaloneAction.put("apiEndpoint", action.getEndpoint() != null ? action.getEndpoint() : null);
                standaloneAction.put("method", action.getMethod() != null ? action.getMethod().toString() : null);
                // Use the request data (what the user wants to update to)
                standaloneAction.put("justification", updateRequest.getJustification() != null ? updateRequest.getJustification() : "");
                standaloneAction.put("conditionScript", updateRequest.getConditionScript() != null ? updateRequest.getConditionScript() : "");

                ArrayNode actionsArray = objectMapper.createArrayNode();
                actionsArray.add(standaloneAction);
                stateAfter.set("actions", actionsArray);
            }

            // Add modules array IMMEDIATELY after roleName
            stateAfter.set("modules", modulesArray);

        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_UPDATE)
                    .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                    .log();
            
            return createFallbackPermissionUpdateState(actionId, role, updateRequest);
        }

        return stateAfter;
    }

    /**
     * Creates a fallback state for permission update when hierarchy lookup fails.
     */
    private JsonNode createFallbackPermissionUpdateState(UUID actionId, Role role, UpdatePermissionRequest updateRequest) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        // Add role information in the exact order we want
        stateAfter.put("roleId", role.getId().toString());
        stateAfter.put("roleName", role.getRoleName());
        
        // Add empty modules array right after role info to maintain order  
        stateAfter.set("modules", objectMapper.createArrayNode());
        
        // Create minimal action information with NEW values
        ObjectNode actionObj = objectMapper.createObjectNode();
        actionObj.put(AppConstants.DETAIL_KEY_ID, actionId.toString());
        actionObj.put(AppConstants.DETAIL_KEY_NAME, AppConstants.DEFAULT_UNKNOWN_ACTION);
        actionObj.put(AppConstants.DETAIL_KEY_JUSTIFICATION, updateRequest.getJustification() != null ? updateRequest.getJustification() : "");
        actionObj.put(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, updateRequest.getConditionScript() != null ? updateRequest.getConditionScript() : "");

        ArrayNode actionsArray = objectMapper.createArrayNode();
        actionsArray.add(actionObj);
        
        stateAfter.set("actions", actionsArray);

        return stateAfter;
    }

    /**
     * Audits dynamic rule creation, treating IMMEDIATE rules as role assignments.
     */
    @Around("createDynamicRulePointcut()")
    public Object auditCreateDynamicRule(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.LOG_AUDIT_ASPECT_TRIGGERED_DYNAMIC_RULE_CREATION)
                .log();

        // Check if this is an IMMEDIATE dynamic rule and set context accordingly
        boolean isImmediateRule = false;
        Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest) {
            com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request = 
                (com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest) args[0];
            isImmediateRule = request.scheduleType() == com.aidentx.applicationpermissions.roles_management.enums.ScheduleType.IMMEDIATE;
        }

        // Set dynamic rule execution context to prevent individual role assignment audits
        if (isImmediateRule) {
            setDynamicRuleExecutionContext(true);
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SET_DYNAMIC_RULE_CONTEXT_FOR_IMMEDIATE_RULE)
                    .log();
        }

        try {
            // Execute the method (this will trigger role assignments for IMMEDIATE rules)
            Object result = pjp.proceed();

            try {
                com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request = 
                    (com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest) args[0];
                
                // ONLY ONE AUDIT LOG: Dynamic rule creation audit (for ALL schedule types)
                // The reconciliation audit will be generated separately by the service layer
                publishDynamicRuleCreationAudit(request, result);
                
            } catch (Exception e) {
                logger.error(e)
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_AUDIT_EVENT)
                        .log();
            }

            return result;
        } finally {
            // Always clear the context after execution
            if (isImmediateRule) {
                clearDynamicRuleExecutionContext();
                logger.debug()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_CLEARED_DYNAMIC_RULE_EXECUTION_CONTEXT)
                        .log();
            }
        }
    }

    /**
     * Publishes audit event for IMMEDIATE dynamic rule as if it was a manual role assignment.
     */
    private void publishImmediateDynamicRuleAsRoleAssignmentAudit(
            com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request,
            Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Get the role information
            Role role = repository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND));

            // Get users who match the rule condition and will be assigned the role
            List<com.aidentx.auth.grpc.UserInfo> eligibleUsers = 
                getEligibleUsersForDynamicRule(request, tenantId);

            // Create state after that looks like a manual role assignment
            JsonNode stateAfter = createDynamicRuleAssignmentStateAfter(role, eligibleUsers);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_IMMEDIATE_DYNAMIC_RULE)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ACTION, AppConstants.ACTION_ASSIGN_ROLE,
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username(),
                            AppConstants.DETAIL_KEY_ELIGIBLE_USER_COUNT, eligibleUsers.size()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.ACTION_ASSIGN_ROLE,
                    AppConstants.ENTITY_TYPE_ROLE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    null, // stateBefore is null for assignment
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase());

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SUCCESSFULLY_PUBLISHED_IMMEDIATE_DYNAMIC_RULE_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ELIGIBLE_USER_COUNT, eligibleUsers.size()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_IMMEDIATE_DYNAMIC_RULE_AUDIT)
                    .log();
        }
    }

    /**
     * Gets users eligible for dynamic rule assignment by evaluating the condition.
     */
    private List<com.aidentx.auth.grpc.UserInfo> getEligibleUsersForDynamicRule(
            com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request,
            String tenantId) {
        try {
            // Get all users in the tenant
            List<com.aidentx.auth.grpc.UserInfo> allUsers = 
                userGrpcClient.getUserListByTenant(tenantId);

            // For now, return all users (the actual condition evaluation would happen in the service)
            // This matches what happens in the real IMMEDIATE execution
            return allUsers;
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_GET_ELIGIBLE_USERS_FOR_DYNAMIC_RULE)
                    .log();
            return new ArrayList<>();
        }
    }

    /**
     * Creates the stateAfter JSON for dynamic rule assignment that looks like manual assignment.
     */
    private JsonNode createDynamicRuleAssignmentStateAfter(Role role, 
            List<com.aidentx.auth.grpc.UserInfo> eligibleUsers) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        // Add role information
        stateAfter.put(AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString());
        stateAfter.put(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName());

        // Add users array (similar to manual assignment)
        ArrayNode usersArray = objectMapper.createArrayNode();
        for (com.aidentx.auth.grpc.UserInfo userInfo : eligibleUsers) {
            ObjectNode userObj = objectMapper.createObjectNode();
            userObj.put(AppConstants.DETAIL_KEY_USER_ID_CAMEL, userInfo.getId());
            userObj.put(AppConstants.DETAIL_KEY_USER_NAME_CAMEL, userInfo.getUsername());
            usersArray.add(userObj);
        }
        
        stateAfter.set(AppConstants.DETAIL_KEY_USERS, usersArray);

        return stateAfter;
    }
    
    /**
     * Publishes audit event for dynamic rule creation (for ALL schedule types).
     * This creates the second audit log showing the dynamic rule creation details.
     */
    private void publishDynamicRuleCreationAudit(
            com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request,
            Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Get the role information
            Role role = repository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND));

            // Create state after that contains the dynamic rule details
            JsonNode stateAfter = createDynamicRuleCreationStateAfter(request);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_DYNAMIC_RULE_CREATION)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ACTION, AppConstants.ACTION_CREATE_DYNAMIC_RULE,
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username(),
                            AppConstants.DETAIL_KEY_SCHEDULE_TYPE, request.scheduleType().toString()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.ACTION_CREATE_DYNAMIC_RULE,
                    AppConstants.ENTITY_TYPE_ROLE_TITLE_CASE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    null, // stateBefore is null for creation
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase());

            eventPublisher.publishEvent(event);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SUCCESSFULLY_PUBLISHED_DYNAMIC_RULE_CREATION_AUDIT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_SCHEDULE_TYPE, request.scheduleType().toString()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_CREATION_AUDIT)
                    .log();
        }
    }

    /**
     * Creates the stateAfter JSON for dynamic rule creation audit.
     */
    private JsonNode createDynamicRuleCreationStateAfter(
            com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest request) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        stateAfter.put(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString());
        stateAfter.put(AppConstants.DETAIL_KEY_RULE_IS_ACTIVE, request.ruleIsActive());
        stateAfter.put(AppConstants.DETAIL_KEY_RULE_CONDITION_SCRIPT, request.ruleConditionScript());
        stateAfter.put(AppConstants.DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT, request.sendNotificationOnAssignment());
        stateAfter.put(AppConstants.DETAIL_KEY_REQUIRE_APPROVAL_BEFORE_ASSIGNMENT, request.requireApprovalBeforeAssignment());
        stateAfter.put(AppConstants.DETAIL_KEY_SCHEDULE_TYPE, request.scheduleType().toString());
        
        // Add time field - null for IMMEDIATE, actual time for scheduled types
        if (request.time() != null) {
            stateAfter.put(AppConstants.DETAIL_KEY_TIME, request.time().toString());
        } else {
            stateAfter.putNull(AppConstants.DETAIL_KEY_TIME);
        }

        return stateAfter;
    }

    /**
     * Audits dynamic rule updates, treating changes from DAILY to IMMEDIATE as role assignments.
     */
    @Around("updateDynamicRulePointcut()")
    public Object auditUpdateDynamicRule(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.LOG_AUDIT_ASPECT_TRIGGERED_DYNAMIC_RULE_UPDATE)
                .log();

        // Get the update request to analyze schedule type changes
        Object[] args = pjp.getArgs();
        com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest request = 
            (com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest) args[0];

        String tenantId = tenantContextService.getCurrentTenantId();
        
        // Get the existing rule to compare schedule types AND capture original state
        com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule existingRule = 
            dynamicRuleRepository.findByRoleId(request.roleId())
                .stream()
                .findFirst()
                .orElse(null);
        
        // Capture the original state BEFORE any modifications
        OriginalDynamicRuleState originalState = null;
        if (existingRule != null) {
            originalState = new OriginalDynamicRuleState(
                existingRule.getRoleId(),
                existingRule.getRuleIsActive(),
                existingRule.getRuleConditionScript(),
                existingRule.getSendNotificationOnAssignment(),
                existingRule.getRequireApprovalBeforeAssignment(),
                existingRule.getScheduleType(),
                existingRule.getTime()
            );
        }
        
        boolean wasDaily = originalState != null && 
            originalState.scheduleType() == com.aidentx.applicationpermissions.roles_management.enums.ScheduleType.DAILY;
        boolean willBeImmediate = request.scheduleType() == 
            com.aidentx.applicationpermissions.roles_management.enums.ScheduleType.IMMEDIATE;
        boolean scheduleChangesToImmediate = wasDaily && willBeImmediate && request.ruleIsActive();

        // Set dynamic rule execution context for schedule changes that trigger immediate execution
        if (scheduleChangesToImmediate) {
            setDynamicRuleExecutionContext(true);
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_SET_DYNAMIC_RULE_CONTEXT_FOR_DAILY_TO_IMMEDIATE)
                    .log();
        }

        try {
            // Execute the method (this will trigger role assignments if schedule changes to IMMEDIATE)
            Object result = pjp.proceed();

            try {
                // ONLY ONE AUDIT LOG: Dynamic rule update audit (for ALL updates)
                // The reconciliation audit will be generated separately by the service layer
                publishDynamicRuleUpdateAudit(request, originalState, result);
                
            } catch (Exception e) {
                logger.error(e)
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_UPDATE_AUDIT_EVENT)
                        .log();
            }

            return result;
        } finally {
            // Always clear the context after execution
            if (scheduleChangesToImmediate) {
                clearDynamicRuleExecutionContext();
                logger.debug()
                        .category(LOG_CATEGORY)
                        .message(AppConstants.LOG_CLEARED_DYNAMIC_RULE_EXECUTION_CONTEXT)
                        .log();
            }
        }
    }

    /**
     * Publishes audit event for schedule change from DAILY to IMMEDIATE as role assignment.
     */
    private void publishScheduleChangeAsRoleAssignmentAudit(
            com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest request,
            Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Get the role information
            Role role = repository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND));

            // Get users who match the rule condition and will be assigned the role
            List<com.aidentx.auth.grpc.UserInfo> eligibleUsers = 
                getEligibleUsersForUpdateRule(request, tenantId);

            // Create state after that looks like a manual role assignment
            JsonNode stateAfter = createDynamicRuleAssignmentStateAfter(role, eligibleUsers);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_SCHEDULE_CHANGE_AS_ROLE_ASSIGNMENT)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ACTION, AppConstants.ACTION_ASSIGN_ROLE,
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username(),
                            AppConstants.DETAIL_KEY_ELIGIBLE_USER_COUNT, eligibleUsers.size(),
                            AppConstants.DETAIL_KEY_SCHEDULE_CHANGE, AppConstants.DETAIL_KEY_SCHEDULE_CHANGE_VALUE))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.ACTION_ASSIGN_ROLE,
                    AppConstants.ENTITY_TYPE_ROLE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    null, // stateBefore is null for assignment
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase());

            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_SCHEDULE_CHANGE_AUDIT)
                    .log();
        }
    }

    /**
     * Publishes audit event for dynamic rule update.
     */
    private void publishDynamicRuleUpdateAudit(
            com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest request,
            OriginalDynamicRuleState originalState,
            Object result) {
        try {
            AuditUser user = securityContextService.getAuditUser();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Get the role information
            Role role = repository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND));

            // Create state before and after for the update
            JsonNode stateBefore = createDynamicRuleUpdateStateBefore(originalState, role);
            JsonNode stateAfter = createDynamicRuleUpdateStateAfter(request, role);

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_CREATING_AUDIT_EVENT_DYNAMIC_RULE_UPDATE)
                    .details(Map.of(
                            AppConstants.DETAIL_KEY_ACTION, AppConstants.ACTION_UPDATE_DYNAMIC_RULE,
                            AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString(),
                            AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName(),
                            AppConstants.DETAIL_KEY_USER_ID, user.id(),
                            AppConstants.DETAIL_KEY_USERNAME, user.username()))
                    .log();

            RoleAuditEvent event = RoleAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    AppConstants.ACTION_UPDATE_DYNAMIC_RULE,
                    AppConstants.ENTITY_TYPE_DYNAMIC_RULE,
                    role.getId().toString(),
                    role.getRoleName(),
                    tenantId,
                    stateBefore,
                    stateAfter,
                    role.getRiskLevel().name().toLowerCase());

            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_UPDATE_AUDIT_EVENT)
                    .log();
        }
    }

    /**
     * Gets eligible users for an update rule condition.
     */
    private List<com.aidentx.auth.grpc.UserInfo> getEligibleUsersForUpdateRule(
            com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest request,
            String tenantId) {
        try {
            // Get all tenant users
            List<com.aidentx.auth.grpc.UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Filter users based on the rule condition
            List<com.aidentx.auth.grpc.UserInfo> eligibleUsers = new ArrayList<>();
            for (com.aidentx.auth.grpc.UserInfo userInfo : allUsers) {
                com.aidentx.applicationpermissions.shared.grpc.dto.UserDto userDto = new com.aidentx.applicationpermissions.shared.grpc.dto.UserDto(
                    userInfo.getId(), userInfo.getUsername(), userInfo.getEmail(),
                    userInfo.getFirstName(), userInfo.getLastName(), userInfo.getDisplayName(),
                    userInfo.getStatus(), userInfo.getTenantId(), userInfo.getLastLoginAt(),
                    userInfo.getPasswordChangedAt(), userInfo.getFailedLoginAttempts(),
                    userInfo.getLockedUntil(), userInfo.getEmailVerifiedAt(), userInfo.getMfaEnabled(),
                    userInfo.getIsPasswordExpired(), userInfo.getProfileData(), userInfo.getCreatedAt(),
                    userInfo.getUpdatedAt(), userInfo.getCreatedBy(), userInfo.getUpdatedBy()
                );
                
                // Use the conditional rule evaluation service to check if user matches
                try {
                    boolean matches = conditionalRuleEvaluationService.evaluateCondition(request.ruleConditionScript(), userDto);
                    if (matches) {
                        eligibleUsers.add(userInfo);
                    }
                } catch (Exception e) {
                    logger.warn()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.LOG_FAILED_TO_EVALUATE_CONDITION_FOR_USER_IN_UPDATE_AUDIT)
                            .details(Map.of(AppConstants.DETAIL_KEY_USER_ID, userInfo.getId(), AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                            .log();
                }
            }
            
            return eligibleUsers;
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.LOG_FAILED_TO_GET_ELIGIBLE_USERS_FOR_UPDATE_RULE_AUDIT)
                    .log();
            return new ArrayList<>();
        }
    }

    /**
     * Creates the stateBefore JSON for dynamic rule update audit.
     */
    private JsonNode createDynamicRuleUpdateStateBefore(
            OriginalDynamicRuleState originalState,
            Role role) {
        ObjectNode stateBefore = objectMapper.createObjectNode();
        
        if (originalState != null) {
            stateBefore.put(AppConstants.DETAIL_KEY_ROLE_ID, originalState.roleId().toString());
            stateBefore.put(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName());
            stateBefore.put(AppConstants.DETAIL_KEY_RULE_IS_ACTIVE, originalState.ruleIsActive());
            stateBefore.put(AppConstants.DETAIL_KEY_RULE_CONDITION_SCRIPT, originalState.ruleConditionScript());
            stateBefore.put(AppConstants.DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT, originalState.sendNotificationOnAssignment());
            stateBefore.put(AppConstants.DETAIL_KEY_REQUIRE_APPROVAL_BEFORE_ASSIGNMENT, originalState.requireApprovalBeforeAssignment());
            stateBefore.put(AppConstants.DETAIL_KEY_SCHEDULE_TYPE, originalState.scheduleType().toString());
            
            if (originalState.time() != null) {
                stateBefore.put(AppConstants.DETAIL_KEY_TIME, originalState.time().toString());
            } else {
                stateBefore.putNull(AppConstants.DETAIL_KEY_TIME);
            }
        }

        return stateBefore;
    }

    /**
     * Creates the stateAfter JSON for dynamic rule update audit.
     */
    private JsonNode createDynamicRuleUpdateStateAfter(
            com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest request,
            Role role) {
        ObjectNode stateAfter = objectMapper.createObjectNode();
        
        stateAfter.put(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString());
        stateAfter.put(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName());
        stateAfter.put(AppConstants.DETAIL_KEY_RULE_IS_ACTIVE, request.ruleIsActive());
        stateAfter.put(AppConstants.DETAIL_KEY_RULE_CONDITION_SCRIPT, request.ruleConditionScript());
        stateAfter.put(AppConstants.DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT, request.sendNotificationOnAssignment());
        stateAfter.put(AppConstants.DETAIL_KEY_REQUIRE_APPROVAL_BEFORE_ASSIGNMENT, request.requireApprovalBeforeAssignment());
        stateAfter.put(AppConstants.DETAIL_KEY_SCHEDULE_TYPE, request.scheduleType().toString());
        
        if (request.time() != null) {
            stateAfter.put(AppConstants.DETAIL_KEY_TIME, request.time());
        } else {
            stateAfter.putNull(AppConstants.DETAIL_KEY_TIME);
        }

        return stateAfter;
    }
    
    /**
     * Record to capture the original state of a dynamic rule before any modifications.
     */
    private record OriginalDynamicRuleState(
        java.util.UUID roleId,
        Boolean ruleIsActive,
        String ruleConditionScript,
        Boolean sendNotificationOnAssignment,
        Boolean requireApprovalBeforeAssignment,
        com.aidentx.applicationpermissions.roles_management.enums.ScheduleType scheduleType,
        java.time.LocalTime time
    ) {}
    
}
