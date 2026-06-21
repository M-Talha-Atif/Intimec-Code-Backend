package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import com.aidentx.applicationpermissions.roles_management.model.dto.PageAccessResponse;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.model.entity.ConditionalRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.*;
import com.aidentx.applicationpermissions.roles_management.service.UserPermissionService;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserPermissionService for checking user permissions.
 * 
 * <p><strong>Caching Strategy:</strong> Permission checks are cached using Caffeine for 5 minutes
 * to dramatically improve performance. Cache keys include userId, endpoint, and method to ensure
 * correctness. Cache is automatically evicted when permissions are assigned/revoked.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class UserPermissionServiceImpl implements UserPermissionService {
    
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final ActionRepository actionRepository;
    private final PageRepository pageRepository;
    private final ModuleRepository moduleRepository;
    private final ConditionalRuleRepository conditionalRuleRepository;
    private final ConditionalRuleEvaluationService conditionalRuleEvaluationService;
    private final UserGrpcClient userGrpcClient;
    private final IUnifiedLogger logger;
    
    @Autowired
    public UserPermissionServiceImpl(
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            ActionRepository actionRepository,
            PageRepository pageRepository,
            ModuleRepository moduleRepository,
            ConditionalRuleRepository conditionalRuleRepository,
            ConditionalRuleEvaluationService conditionalRuleEvaluationService,
            UserGrpcClient userGrpcClient,
            UnifiedLoggerFactory loggerFactory) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.actionRepository = actionRepository;
        this.pageRepository = pageRepository;
        this.moduleRepository = moduleRepository;
        this.conditionalRuleRepository = conditionalRuleRepository;
        this.conditionalRuleEvaluationService = conditionalRuleEvaluationService;
        this.userGrpcClient = userGrpcClient;
        this.logger = loggerFactory.getLogger("UserPermissionService", "PERMISSION_CHECK");
    }
    
    /**
     * Checks if a user has permission to access a specific API endpoint with a given HTTP method.
     * 
     * <p><strong>Caching:</strong> Results are cached for 5 minutes to avoid repeated database queries.
     * Cache key format: "userId:httpMethod:endpoint"
     * 
     * <p><strong>Performance Impact:</strong>
     * <ul>
     *   <li>Without cache: 5 DB queries × 50ms = 250ms</li>
     *   <li>With cache hit: ~0.1ms (2500x faster)</li>
     *   <li>Expected cache hit rate: 95-99%</li>
     * </ul>
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param apiEndpoint the API endpoint being accessed
     * @param httpMethod the HTTP method (GET, POST, PUT, DELETE)
     * @return true if user has permission, false otherwise
     */
    @Cacheable(
        value = "permissionChecks", 
        key = "#userId + ':' + #httpMethod + ':' + #apiEndpoint",
        unless = "#result == false"  // Don't cache denials to allow real-time permission grants
    )
    @Override
    public boolean hasPermissionForEndpoint(String userId, String tenantId, String apiEndpoint, String httpMethod) {
        try {
            logger.trace()
                .message(AppConstants.LOG_CHECKING_USER_PERMISSION_FOR_ENDPOINT)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                .log();
                
            logger.info()
                .message(AppConstants.LOG_CHECKING_USER_PERMISSION_FOR_ENDPOINT)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                .log();
            
            // Step 1: Find actions that match the endpoint and method
            logger.trace()
                .message(AppConstants.LOG_VALIDATING_HTTP_METHOD_ENUM)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_HTTP_METHOD_VALIDATION)
                .log();
                
            HttpMethod httpMethodEnum;
            try {
                httpMethodEnum = HttpMethod.valueOf(httpMethod.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message(AppConstants.WARN_INVALID_HTTP_METHOD)
                    .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                    .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                    .objectType(AppConstants.OBJECT_TYPE_HTTP_METHOD_VALIDATION)
                    .log();
                return false;
            }
            
            logger.trace()
                .message(AppConstants.LOG_SEARCHING_ACTIONS_FOR_ENDPOINT_AND_METHOD)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                .log();
            
            // Get all actions with this HTTP method and match endpoint patterns
            var allMethodActions = actionRepository.findAllByMethodWithEndpoint(httpMethodEnum);
            var matchingActions = allMethodActions.stream()
                .filter(action -> matchesEndpointPattern(action.getEndpoint(), apiEndpoint))
                .collect(java.util.stream.Collectors.toList());
            
            if (matchingActions.isEmpty()) {
                logger.warn()
                    .message(AppConstants.WARN_NO_ACTIONS_FOUND_FOR_ENDPOINT)
                    .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                    .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                    .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                    .log();
                return false; // No actions defined for this endpoint
            }
            
            logger.debug()
                .message(AppConstants.LOG_FOUND_MATCHING_ACTIONS_FOR_ENDPOINT)
                .detail(AppConstants.DETAIL_ACTION_COUNT, String.valueOf(matchingActions.size()))
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                .log();
            
                        
            // Step 2: Get user's roles in this tenant
            logger.trace()
                .message(AppConstants.LOG_VALIDATING_USER_ID_FORMAT)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .objectType(AppConstants.OBJECT_TYPE_USER_ID_VALIDATION)
                .log();
                
            UUID userIdUuid;
            try {
                userIdUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message(AppConstants.WARN_INVALID_USER_ID_FORMAT)
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_USER_ID_VALIDATION)
                    .log();
                return false;
            }
            
            logger.trace()
                .message(AppConstants.LOG_SEARCHING_USER_ROLES_IN_TENANT)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_USER_ROLE_SEARCH)
                .log();
            
            // Get only ACTIVE roles for the user (filter out inactive roles)
            List<UUID> activeRoleIds = userRoleRepository.findActiveRoleIdsByUserIdAndTenantId(userIdUuid, tenantId);
            
            if (activeRoleIds.isEmpty()) {
                logger.warn()
                    .message(AppConstants.WARN_NO_ROLES_FOUND_FOR_USER_IN_TENANT)
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_USER_ROLE_SEARCH)
                    .log();
                return false; // User has no active roles in this tenant
            }
            
            logger.debug()
                .message(AppConstants.LOG_FOUND_USER_ROLES_IN_TENANT)
                .detail(AppConstants.DETAIL_ROLE_COUNT, String.valueOf(activeRoleIds.size()))
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_USER_ROLE_SEARCH)
                .log();
            
                        
            // Step 3: Check if any of the user's ACTIVE roles have permission for any of the matching actions
            logger.trace()
                .message(AppConstants.LOG_STARTING_PERMISSION_CHECK_FOR_USER_ROLES_AND_ACTIONS)
                .detail(AppConstants.DETAIL_USER_ROLE_COUNT, String.valueOf(activeRoleIds.size()))
                .detail(AppConstants.DETAIL_ACTION_COUNT, String.valueOf(matchingActions.size()))
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                .log();
                
            logger.info()
                .message(AppConstants.LOG_STARTING_PERMISSION_CHECK_FOR_USER_ROLES_AND_ACTIONS)
                .detail(AppConstants.DETAIL_USER_ROLE_COUNT, String.valueOf(activeRoleIds.size()))
                .detail(AppConstants.DETAIL_ACTION_COUNT, String.valueOf(matchingActions.size()))
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_ITERATING_THROUGH_USER_ROLES)
                .detail(AppConstants.DETAIL_USER_ROLE_COUNT, String.valueOf(activeRoleIds.size()))
                .objectType(AppConstants.OBJECT_TYPE_USER_ROLE_SEARCH)
                .log();
                
            for (UUID roleId : activeRoleIds) {
                logger.trace()
                    .message(AppConstants.LOG_CHECKING_PERMISSIONS_FOR_USER_ROLE)
                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                    .log();
                    
                logger.info()
                    .message(AppConstants.LOG_CHECKING_PERMISSIONS_FOR_USER_ROLE)
                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                    .log();
                    
                logger.trace()
                    .message(AppConstants.LOG_ITERATING_THROUGH_MATCHING_ACTIONS)
                    .detail(AppConstants.DETAIL_ACTION_COUNT, String.valueOf(matchingActions.size()))
                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                    .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                    .log();
                    
                for (var action : matchingActions) {
                    logger.trace()
                        .message(AppConstants.LOG_CHECKING_PERMISSIONS_FOR_ACTION)
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .detail(AppConstants.DETAIL_ACTION_NAME, action.getActionName())
                        .detail(AppConstants.DETAIL_ENDPOINT, action.getEndpoint())
                        .detail(AppConstants.DETAIL_METHOD, action.getMethod() != null ? action.getMethod().toString() : "null")
                        .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                        .log();
                        
                    logger.info()
                        .message(AppConstants.LOG_CHECKING_PERMISSIONS_FOR_ACTION)
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .detail(AppConstants.DETAIL_ACTION_NAME, action.getActionName())
                        .detail(AppConstants.DETAIL_ENDPOINT, action.getEndpoint())
                        .detail(AppConstants.DETAIL_METHOD, action.getMethod() != null ? action.getMethod().toString() : "null")
                        .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                        .log();
                        
                    // Find permissions for this action
                    logger.trace()
                        .message(AppConstants.LOG_SEARCHING_PERMISSIONS_FOR_ACTION)
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                        .log();
                        
                    var permissions = permissionRepository.findByActionId(action.getId());
                    
                    logger.debug()
                        .message(AppConstants.LOG_FOUND_PERMISSIONS_FOR_ACTION)
                        .detail(AppConstants.DETAIL_PERMISSION_COUNT, String.valueOf(permissions.size()))
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                        .log();
                    
                    logger.info()
                        .message(AppConstants.LOG_FOUND_PERMISSIONS_FOR_ACTION)
                        .detail(AppConstants.DETAIL_PERMISSION_COUNT, String.valueOf(permissions.size()))
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                        .log();
                    
                    logger.trace()
                        .message(AppConstants.LOG_ITERATING_THROUGH_PERMISSIONS)
                        .detail(AppConstants.DETAIL_PERMISSION_COUNT, String.valueOf(permissions.size()))
                        .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                        .log();
                    
                    for (var permission : permissions) {
                        logger.trace()
                            .message(AppConstants.LOG_CHECKING_ROLE_PERMISSION_ASSOCIATION)
                            .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                            .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                            .log();
                            
                        logger.info()
                            .message(AppConstants.LOG_CHECKING_ROLE_PERMISSION_ASSOCIATION)
                            .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                            .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                            .log();
                            
                        // Check if this role has this permission
                        logger.trace()
                            .message(AppConstants.LOG_QUERYING_ROLE_PERMISSION_ASSOCIATION)
                            .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                            .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                            .log();
                            
                        boolean roleHasPermission = rolePermissionRepository.existsByRoleIdAndPermissionId(
                            roleId, permission.getId());
                        
                        logger.debug()
                            .message(AppConstants.LOG_ROLE_PERMISSION_CHECK_RESULT)
                            .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                            .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(roleHasPermission))
                            .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                            .log();
                        
                        logger.info()
                            .message(AppConstants.LOG_ROLE_PERMISSION_CHECK_RESULT)
                            .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                            .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(roleHasPermission))
                            .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                            .log();
                        
                        if (roleHasPermission) {
                            logger.debug()
                                .message(AppConstants.LOG_ROLE_HAS_BASIC_PERMISSION_CHECKING_CONDITIONAL_RULES)
                                .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                                .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                .log();
                            
                            // Check conditional rules for this permission
                            List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                            
                            if (conditionalRules.isEmpty()) {
                                // No conditional rules - basic permission is sufficient
                                logger.info()
                                    .message(AppConstants.LOG_PERMISSION_GRANTED_FOR_USER)
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                                    .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                                    .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                                    .detail(AppConstants.DETAIL_CONDITIONAL_RULES_COUNT, "0")
                                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                                    .log();
                                return true;
                            }
                            
                            // Get user data for conditional rule evaluation
                            UserDto userData = getUserData(userId, tenantId);
                            if (userData == null) {
                                logger.warn()
                                    .message(AppConstants.LOG_UNABLE_TO_RETRIEVE_USER_DATA_FOR_CONDITIONAL_RULE_EVALUATION)
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                    .log();
                                continue; // Skip this permission, try others
                            }
                            
                            // Evaluate each conditional rule
                            boolean allConditionsMet = true;
                            for (ConditionalRule rule : conditionalRules) {
                                logger.debug()
                                    .message(AppConstants.LOG_EVALUATING_CONDITIONAL_RULE)
                                    .detail(AppConstants.DETAIL_CONDITIONAL_RULE_ID, rule.getId().toString())
                                    .detail(AppConstants.DETAIL_CONDITION_SCRIPT, rule.getConditionScript())
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                    .log();
                                
                                boolean conditionMet = conditionalRuleEvaluationService.evaluateCondition(
                                    rule.getConditionScript(), userData);
                                
                                if (!conditionMet) {
                                    logger.info()
                                        .message(AppConstants.LOG_CONDITIONAL_RULE_NOT_MET)
                                        .detail(AppConstants.DETAIL_CONDITIONAL_RULE_ID, rule.getId().toString())
                                        .detail(AppConstants.DETAIL_CONDITION_SCRIPT, rule.getConditionScript())
                                        .detail(AppConstants.DETAIL_USER_ID, userId)
                                        .detail(AppConstants.DETAIL_CONDITION_MET, "false")
                                        .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                        .log();
                                    allConditionsMet = false;
                                    break;
                                }
                                
                                logger.debug()
                                    .message(AppConstants.LOG_CONDITIONAL_RULE_SATISFIED)
                                    .detail(AppConstants.DETAIL_CONDITIONAL_RULE_ID, rule.getId().toString())
                                    .detail(AppConstants.DETAIL_CONDITION_SCRIPT, rule.getConditionScript())
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                    .log();
                            }
                            
                            if (allConditionsMet) {
                                logger.info()
                                    .message(AppConstants.LOG_PERMISSION_GRANTED_FOR_USER)
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                                    .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                                    .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                                    .detail(AppConstants.DETAIL_CONDITIONAL_RULES_COUNT, String.valueOf(conditionalRules.size()))
                                    .detail(AppConstants.DETAIL_ALL_CONDITIONS_MET, "true")
                                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                                    .log();
                                return true;
                            } else {
                                logger.info()
                                    .message(AppConstants.LOG_PERMISSION_DENIED_DUE_TO_UNMET_CONDITIONAL_RULES)
                                    .detail(AppConstants.DETAIL_USER_ID, userId)
                                    .detail(AppConstants.DETAIL_ROLE_ID, roleId.toString())
                                    .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                                    .detail(AppConstants.DETAIL_CONDITIONAL_RULES_COUNT, String.valueOf(conditionalRules.size()))
                                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                                    .log();
                            }
                        }
                    }
                }
            }
            
            logger.warn()
                .message(AppConstants.WARN_NO_PERMISSION_FOUND_FOR_USER_ENDPOINT_ACCESS)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
                .log();
            
            return false; // No permission found
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_CHECKING_USER_PERMISSION)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_USER_PERMISSION_SERVICE)
                .log();
            
            return false; // Deny access on error
        }
    }
    
    @Override
    public boolean isEndpointManaged(String apiEndpoint, String httpMethod) {
        try {
            logger.trace()
                .message(AppConstants.LOG_CHECKING_IF_ENDPOINT_IS_MANAGED)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_MANAGEMENT_CHECK)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_CHECKING_IF_ENDPOINT_IS_MANAGED)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_MANAGEMENT_CHECK)
                .log();
            
            // Look for any action that matches this endpoint and HTTP method
            logger.trace()
                .message(AppConstants.LOG_VALIDATING_HTTP_METHOD_ENUM)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_HTTP_METHOD_VALIDATION)
                .log();
                
            HttpMethod httpMethodEnum;
            try {
                httpMethodEnum = HttpMethod.valueOf(httpMethod.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message(AppConstants.WARN_INVALID_HTTP_METHOD)
                    .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                    .objectType(AppConstants.OBJECT_TYPE_HTTP_METHOD_VALIDATION)
                    .log();
                return false;
            }
            
            logger.trace()
                .message(AppConstants.LOG_SEARCHING_ACTIONS_FOR_ENDPOINT_AND_METHOD)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                .log();
            
            // Get all actions with this HTTP method and match endpoint patterns
            var allMethodActions = actionRepository.findAllByMethodWithEndpoint(httpMethodEnum);
            var actions = allMethodActions.stream()
                .filter(action -> matchesEndpointPattern(action.getEndpoint(), apiEndpoint))
                .collect(java.util.stream.Collectors.toList());
            
            if (actions.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_ENDPOINT_NOT_FOUND_IN_ACTION_TABLE_NOT_MANAGED)
                    .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                    .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                    .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                    .log();
                return false; // Endpoint doesn't exist in Action table
            }
            
            logger.debug()
                .message(AppConstants.LOG_FOUND_MATCHING_ACTIONS_FOR_ENDPOINT)
                .detail(AppConstants.DETAIL_ACTIONS_FOUND, String.valueOf(actions.size()))
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_ACTION_SEARCH)
                .log();
            
            // Endpoint exists in Action table, now check if any roles have permissions for these actions
            logger.trace()
                .message(AppConstants.LOG_CHECKING_ROLES_WITH_PERMISSIONS_FOR_ACTIONS)
                .detail(AppConstants.DETAIL_ACTIONS_FOUND, String.valueOf(actions.size()))
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_MANAGEMENT_CHECK)
                .log();
                
            boolean hasRolePermissions = false;
            for (Action action : actions) {
                logger.trace()
                    .message(AppConstants.LOG_SEARCHING_PERMISSIONS_FOR_ACTION)
                    .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                    .detail(AppConstants.DETAIL_ACTION_NAME, action.getActionName())
                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                    .log();
                    
                List<Permission> permissions = permissionRepository.findByActionId(action.getId());
                
                logger.debug()
                    .message(AppConstants.LOG_FOUND_PERMISSIONS_FOR_ACTION)
                    .detail(AppConstants.DETAIL_PERMISSION_COUNT, String.valueOf(permissions.size()))
                    .detail(AppConstants.DETAIL_ACTION_ID, action.getId().toString())
                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_SEARCH)
                    .log();
                    
                for (Permission permission : permissions) {
                    logger.trace()
                        .message(AppConstants.LOG_COUNTING_ROLES_FOR_PERMISSION)
                        .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                        .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                        .log();
                        
                    // Count only ACTIVE roles - inactive roles should not enforce permissions
                    Long activeRoleCount = permissionRepository.countActiveRolesByPermissionId(permission.getId());
                    
                    logger.debug()
                        .message(AppConstants.LOG_ROLE_PERMISSION_CHECK_RESULT)
                        .detail(AppConstants.DETAIL_PERMISSION_ID, permission.getId().toString())
                        .detail(AppConstants.DETAIL_ROLE_COUNT_FOR_PERMISSION, String.valueOf(activeRoleCount))
                        .objectType(AppConstants.OBJECT_TYPE_ROLE_PERMISSION_CHECK)
                        .log();
                        
                    if (activeRoleCount > 0) {
                        hasRolePermissions = true;
                        break;
                    }
                }
                if (hasRolePermissions) {
                    break;
                }
            }
            
            logger.debug()
                .message(AppConstants.LOG_ENDPOINT_MANAGEMENT_CHECK_RESULT)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_ACTIONS_FOUND, String.valueOf(actions.size()))
                .detail(AppConstants.DETAIL_HAS_ROLE_PERMISSIONS, String.valueOf(hasRolePermissions))
                .detail(AppConstants.DETAIL_IS_MANAGED, String.valueOf(hasRolePermissions))
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_MANAGEMENT_CHECK)
                .log();
                        
            return hasRolePermissions; // Only managed if roles have permissions assigned
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_CHECKING_IF_ENDPOINT_IS_MANAGED)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_USER_PERMISSION_SERVICE)
                .log();
            
            return false; // Return false for safety
        }
    }
    
    @Override
    public List<PageAccessResponse> getUserPageAccess(String userId, String tenantId) {
        logger.trace()
            .message(AppConstants.LOG_STARTING_PAGE_ACCESS_CHECK_FOR_USER)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
            .log();
        
        List<PageAccessResponse> pageAccessList = new ArrayList<>();
        
        try {
            // Get all system pages (tenantId = null)
            List<Page> systemPages = pageRepository.findByTenantIdIsNull();
            
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_SYSTEM_PAGES_FOR_ACCESS_CHECK)
                .detail(AppConstants.DETAIL_KEY_PAGE_COUNT, String.valueOf(systemPages.size()))
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
            
            for (Page page : systemPages) {
                
                // Check if there's a "View" action for this page
                Optional<Action> viewActionOpt = actionRepository
                    .findByActionNameAndPageId("View", page.getId());
                
                boolean requiresPermission = false;
                boolean canAccess = true; // Default to true (open access)
                
                if (viewActionOpt.isPresent()) {
                    // Check if this action has any permissions assigned to ACTIVE roles
                    List<Permission> permissions = permissionRepository.findByActionId(viewActionOpt.get().getId());
                    
                    // Check if any of these permissions belong to ACTIVE roles
                    boolean hasActiveRolePermissions = false;
                    if (!permissions.isEmpty()) {
                        for (Permission permission : permissions) {
                            Long activeRoleCount = permissionRepository.countActiveRolesByPermissionId(permission.getId());
                            if (activeRoleCount > 0) {
                                hasActiveRolePermissions = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasActiveRolePermissions) {
                        // Permissions exist for ACTIVE roles - requires permission check
                        requiresPermission = true;
                        canAccess = hasPermissionForAction(userId, tenantId, viewActionOpt.get().getId());
                        
                        logger.trace()
                            .message(AppConstants.LOG_CHECKED_VIEW_PERMISSION_FOR_PAGE_WITH_ASSIGNED_PERMISSIONS)
                            .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                            .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                            .detail(AppConstants.DETAIL_KEY_CAN_ACCESS, String.valueOf(canAccess))
                            .detail(AppConstants.DETAIL_KEY_REQUIRES_PERMISSION, String.valueOf(requiresPermission))
                            .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                            .log();
                    } else {
                        // Action exists but no permissions assigned - open access
                        logger.trace()
                            .message(AppConstants.LOG_PAGE_HAS_VIEW_ACTION_BUT_NO_PERMISSIONS_ASSIGNED)
                            .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                            .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                            .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                            .log();
                    }
                } else {
                    // No View action exists - open access
                    logger.trace()
                        .message(AppConstants.LOG_PAGE_HAS_NO_VIEW_ACTION_ALLOWING_OPEN_ACCESS)
                        .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                        .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                        .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                        .log();
                }
                
                PageAccessResponse pageAccess = new PageAccessResponse(
                    page.getId(),
                    page.getPageName(),
                    page.getPagePath(),
                    canAccess,
                    requiresPermission
                );
                
                pageAccessList.add(pageAccess);
            }
            
            logger.info()
                .message(AppConstants.LOG_COMPLETED_PAGE_ACCESS_CHECK_FOR_USER)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_PAGES, String.valueOf(pageAccessList.size()))
                .detail(AppConstants.DETAIL_KEY_ACCESSIBLE_PAGES, String.valueOf((int) pageAccessList.stream().filter(PageAccessResponse::isCanAccess).count()))
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_CHECK_PAGE_ACCESS_FOR_USER)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
        }
        
        return pageAccessList;
    }
    
    /**
     * Checks if user has permission for a specific action.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param actionId the action ID
     * @return true if user has permission, false otherwise
     */
    private boolean hasPermissionForAction(String userId, String tenantId, UUID actionId) {
        logger.trace()
            .message(AppConstants.LOG_CHECKING_USER_PERMISSION_FOR_ACTION)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_ACTION_ID, actionId.toString())
            .objectType(AppConstants.OBJECT_TYPE_ACTION_PERMISSION_CHECK)
            .log();
        
        try {
            // Get ACTIVE user roles only (filter out inactive roles)
            UUID userUuid = UUID.fromString(userId);
            List<UUID> activeRoleIds = userRoleRepository.findActiveRoleIdsByUserIdAndTenantId(userUuid, tenantId);
            
            if (activeRoleIds.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_USER_HAS_NO_ROLES_ASSIGNED)
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_ACTION_PERMISSION_CHECK)
                    .log();
                return false;
            }
            
            // Check permissions for user's ACTIVE roles
            for (UUID roleId : activeRoleIds) {
                List<Permission> permissions = permissionRepository.findAllByRoleIdAndActionId(roleId, actionId);
                
                for (Permission permission : permissions) {
                    // Check if role has this permission
                    boolean roleHasPermission = rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permission.getId());
                    
                    if (roleHasPermission) {
                        // Check conditional rules for this permission
                        List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                        
                        if (conditionalRules.isEmpty()) {
                            // No conditional rules - basic permission is sufficient
                            return true;
                        }
                        
                        // Get user data for conditional rule evaluation
                        UserDto userData = getUserData(userId, tenantId);
                        if (userData == null) {
                            continue; // Skip this permission, try others
                        }
                        
                        // Evaluate each conditional rule - all must be satisfied
                        boolean allConditionsMet = true;
                        for (ConditionalRule rule : conditionalRules) {
                            boolean conditionMet = conditionalRuleEvaluationService
                                .evaluateCondition(rule.getConditionScript(), userData);
                            
                            if (!conditionMet) {
                                allConditionsMet = false;
                                break;
                            }
                        }
                        
                        if (allConditionsMet) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_CHECKING_PERMISSION_FOR_ACTION)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.OBJECT_TYPE_ACTION_PERMISSION_CHECK)
                .log();
            return false;
        }
    }
    
    /**
     * Retrieves user data via gRPC for conditional rule evaluation.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return UserDto containing user data, or null if user not found
     */
    private UserDto getUserData(String userId, String tenantId) {
        try {
            logger.trace()
                .message(AppConstants.LOG_RETRIEVING_USER_DATA_VIA_GRPC)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();
            
            // Get all users for the tenant and find the specific user
            List<UserInfo> users = userGrpcClient.getUserListByTenant(tenantId);
            
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_USERS_FROM_TENANT_SEARCHING_FOR_SPECIFIC_USER)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_TOTAL_USERS_RETRIEVED, String.valueOf(users.size()))
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();
            
            // Log all user IDs for debugging
            for (UserInfo u : users) {
                logger.debug()
                    .message(AppConstants.LOG_USER_IN_TENANT)
                    .detail(AppConstants.DETAIL_USER_ID_IN_LIST, u.getId())
                    .detail(AppConstants.DETAIL_USERNAME_IN_LIST, u.getUsername())
                    .detail(AppConstants.DETAIL_EMAIL_IN_LIST, u.getEmail())
                    .detail(AppConstants.DETAIL_SEARCHING_FOR_USER_ID, userId)
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
            }
            
            UserInfo userInfo = users.stream()
                .filter(user -> user.getId().equals(userId))
                .findFirst()
                .orElse(null);
            
            if (userInfo == null) {
                logger.warn()
                    .message(AppConstants.LOG_USER_NOT_FOUND_IN_TENANT)
                    .detail(AppConstants.DETAIL_USER_ID, userId)
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_TOTAL_USERS_IN_TENANT, String.valueOf(users.size()))
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
                return null;
            }
            
            logger.debug()
                .message(AppConstants.LOG_FOUND_USER_IN_TENANT)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_FOUND_USER_EMAIL, userInfo.getEmail())
                .detail(AppConstants.DETAIL_FOUND_USER_STATUS, userInfo.getStatus())
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();
            
            UserDto userDto = convertToUserDto(userInfo);
            
            logger.debug()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_USER_DATA_VIA_GRPC)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_USER_EMAIL, userInfo.getEmail())
                .detail(AppConstants.DETAIL_USER_STATUS, userInfo.getStatus())
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();
            
            return userDto;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_USER_DATA_VIA_GRPC)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();
            
            return null;
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
    
    @Override
    public boolean checkActionPermission(String userId, String tenantId, UUID actionId) {
        return hasPermissionForAction(userId, tenantId, actionId);
    }
    
    /**
     * Check if an actual endpoint matches a pattern with wildcards.
     * 
     * @param pattern the endpoint pattern (may contain asterisk)
     * @param actualEndpoint the actual request endpoint
     * @return true if actualEndpoint matches the pattern
     */
    private boolean matchesEndpointPattern(String pattern, String actualEndpoint) {
        if (pattern == null || actualEndpoint == null) {
            return false;
        }
        
        // Support comma-separated patterns (multiple patterns in one action)
        if (pattern.contains(",")) {
            String[] patterns = pattern.split(",");
            for (String singlePattern : patterns) {
                if (matchesSinglePattern(singlePattern.trim(), actualEndpoint)) {
                    return true;
                }
            }
            return false;
        }
        
        return matchesSinglePattern(pattern, actualEndpoint);
    }
    
    private boolean matchesSinglePattern(String pattern, String actualEndpoint) {
        if (pattern.equals(actualEndpoint)) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return false;
        }
        
        String regexPattern = pattern.replace(".", "\\.");
        regexPattern = regexPattern.replace("/", "\\/");
        regexPattern = regexPattern.replace("*", ".*");
        
        return actualEndpoint.matches(regexPattern);
    }
}