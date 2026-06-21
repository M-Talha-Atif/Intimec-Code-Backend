package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.event.payload.RoleAssignmentBatchEvent;
import com.aidentx.applicationpermissions.roles_management.event.producer.RoleAssignmentBatchProducer;
import com.aidentx.applicationpermissions.roles_management.model.dto.*;
import com.aidentx.applicationpermissions.roles_management.event.RoleEventPublisher;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.model.entity.RoleOwners;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleOwnersRepository;
import com.aidentx.applicationpermissions.roles_management.repository.UserRoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.UserRoleService;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.applicationpermissions.roles_management.model.entity.BatchJob;
import com.aidentx.applicationpermissions.roles_management.repository.BatchJobRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of UserRoleService for managing user-role assignments with tenant-aware access control.
 * 
 * <p>This service ensures that all user-role operations are scoped to the current user's tenant
 * as extracted from the JWT token. It provides complete tenant isolation and validates that
 * both users and roles exist and belong to the same tenant before operations.</p>
 * 
 * <p><strong>Caching Strategy:</strong> All methods that assign or revoke roles automatically evict
 * permission caches to ensure users immediately get or lose permissions based on role changes.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    @PersistenceContext
    private EntityManager entityManager;
    
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleOwnersRepository roleOwnersRepository;
    private final com.aidentx.applicationpermissions.roles_management.repository.RolePermissionRepository rolePermissionRepository;
    private final TenantContextService tenantContextService;
    private final UserGrpcClient userGrpcClient;
    private final RoleEventPublisher roleEventPublisher;
    private final IUnifiedLogger logger;

    private final BatchJobRepository batchJobRepository;
    private final RoleAssignmentBatchProducer batchProducer;

    // Topic name for batch job events
    private static final String BATCH_JOB_TOPIC = "role-assignment-jobs";

    /**
     * Constructor for UserRoleServiceImpl.
     * 
     * @param userRoleRepository repository for user-role entities
     * @param roleRepository repository for role entities
     * @param tenantContextService service for extracting tenant context from JWT
     * @param userGrpcClient gRPC client for user validation
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public UserRoleServiceImpl(UserRoleRepository userRoleRepository,
                              RoleRepository roleRepository,
                              RoleOwnersRepository roleOwnersRepository,
                              com.aidentx.applicationpermissions.roles_management.repository.RolePermissionRepository rolePermissionRepository,
                              TenantContextService tenantContextService,
                              UserGrpcClient userGrpcClient,
                              RoleEventPublisher roleEventPublisher,
                               BatchJobRepository batchJobRepository,
                               RoleAssignmentBatchProducer batchProducer,
                              UnifiedLoggerFactory loggerFactory) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleOwnersRepository = roleOwnersRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.tenantContextService = tenantContextService;
        this.userGrpcClient = userGrpcClient;
        this.roleEventPublisher = roleEventPublisher;
        this.batchJobRepository = batchJobRepository;
        this.batchProducer = batchProducer;
        this.logger = loggerFactory.getLogger(AppConstants.USER_ROLE_SERVICE_LOGGER_NAME, AppConstants.LOG_CATEGORY_USER_ROLE_MANAGEMENT);
    }

    /**
     * Assigns a role to one or more users within the current tenant.
     * Supports both single user assignment and bulk assignment.
     * 
     * <p><strong>Cache Eviction:</strong> Clears all permission caches when roles are assigned
     * to ensure users immediately get the new role's permissions.
     * 
     * @param request the role assignment request (single or bulk)
     * @return UserRoleResponse for single assignment, BulkUserRoleResponse for bulk assignment
     * @throws IllegalArgumentException if user(s) or role does not exist or belongs to different tenant
     * @throws IllegalStateException if the user already has the role assigned (single assignment only)
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Override
    public Object assignRole(AssignRoleRequest request) {
        if (request.isBulkAssignment()) {
            return processBulkAssignment(request);
        } else {
            return processSingleAssignment(request);
        }
    }

    @Override
    public Object assignRole(AssignRoleRequest request, String tenantId) {
        if (request.isBulkAssignment()) {
            return processBulkAssignment(request, tenantId);
        } else {
            return processSingleAssignment(request, tenantId);
        }
    }
    
    /**
     * Processes a single user role assignment.
     */
    private UserRoleResponse processSingleAssignment(AssignRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            return processSingleAssignment(request, tenantId);
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_PROCESSING_USER_ROLE_ASSIGNMENT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .log();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Processes a single user role assignment with explicit tenant context.
     */
    private UserRoleResponse processSingleAssignment(AssignRoleRequest request, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_PROCESSING_USER_ROLE_ASSIGNMENT_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_REQUEST)
                .log();

            // Validate that the role exists and belongs to the tenant
            Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Validate that the user exists and belongs to the tenant
            validateUserExistsInTenant(request.userId(), tenantId);

            // Check if the user already has this role assigned
            if (userRoleRepository.existsByUserIdAndRoleIdAndTenantId(request.userId(), request.roleId(), tenantId)) {
                throw new IllegalStateException(AppConstants.USER_ALREADY_HAS_THIS_ROLE_ASSIGNED);
            }

            // Create the user-role assignment with MANUAL assignment type
            UserRole userRole = new UserRole(
                request.userId(), 
                request.roleId(), 
                tenantId, 
                AssignmentType.MANUAL
            );
            UserRole savedUserRole = userRoleRepository.save(userRole);

            logger.info()
                .message(AppConstants.LOG_USER_ROLE_ASSIGNED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_USER_ROLE_ID, savedUserRole.getId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT)
                .log();

            return mapToUserRoleResponse(savedUserRole, role);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_ASSIGN_ROLE_TO_USER)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ERROR)
                .log();
            throw e;
        }
    }
    
    /**
     * Processes bulk user role assignment.
     */
    private BulkUserRoleResponse processBulkAssignment(AssignRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            return processBulkAssignment(request, tenantId);
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_PROCESSING_BULK_USER_ROLE_ASSIGNMENT)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(request.userIds().size()))
                .log();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Processes bulk user role assignment with explicit tenant context.
     */
    private BulkUserRoleResponse processBulkAssignment(AssignRoleRequest request, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_PROCESSING_BULK_USER_ROLE_ASSIGNMENT_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(request.userIds().size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_REQUEST)
                .log();

            // Validate that the role exists and belongs to the tenant
            Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            List<UserRoleResponse> successfulAssignments = new ArrayList<>();
            List<BulkUserRoleResponse.FailedAssignment> failedAssignments = new ArrayList<>();

            // CRITICAL FIX: Pre-fetch all existing assignments BEFORE the transaction to avoid transaction poisoning
            // Query which users already have this role assigned
            List<UserRole> existingAssignments = userRoleRepository.findByRoleIdAndTenantId(request.roleId(), tenantId);
            Set<UUID> usersWithRole = existingAssignments.stream()
                .map(UserRole::getUserId)
                .collect(Collectors.toSet());

            // Process each user ID
            for (UUID userId : request.userIds()) {
                try {
                    // Validate that the user exists and belongs to the tenant
                    validateUserExistsInTenant(userId, tenantId);

                    // Check if the user already has this role assigned (using pre-fetched data)
                    if (usersWithRole.contains(userId)) {
                        failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                            userId,
                            AppConstants.USER_ALREADY_HAS_THIS_ROLE_ASSIGNED,
                            AppConstants.ERROR_CODE_ROLE_ALREADY_ASSIGNED
                        ));
                        continue;
                    }

                    // Create the user-role assignment with MANUAL assignment type
                    UserRole userRole = new UserRole(
                        userId, 
                        request.roleId(), 
                        tenantId, 
                        AssignmentType.MANUAL
                    );
                    UserRole savedUserRole = userRoleRepository.save(userRole);
                    successfulAssignments.add(mapToUserRoleResponse(savedUserRole, role));
                    
                    // Add to the set to prevent duplicate attempts in the same batch
                    usersWithRole.add(userId);

                    logger.debug()
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNED_SUCCESSFULLY)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_USER_ROLE_ID, savedUserRole.getId().toString())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT)
                        .log();

                } catch (IllegalArgumentException e) {
                    String errorCode = e.getMessage().contains(AppConstants.ERROR_DETECTION_USER_NOT_FOUND) ? AppConstants.ERROR_CODE_USER_NOT_FOUND : 
                                     e.getMessage().contains(AppConstants.ERROR_DETECTION_DOES_NOT_BELONG_TO_TENANT) ? AppConstants.ERROR_CODE_USER_NOT_IN_TENANT : AppConstants.ERROR_CODE_VALIDATION_ERROR;
                    failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                        userId,
                        e.getMessage(),
                        errorCode
                    ));

                    logger.warn()
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                        .log();

                } catch (Exception e) {
                    failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                        userId,
                        AppConstants.FAILED_TO_ASSIGN_ROLE_TO_USER,
                        AppConstants.ERROR_CODE_INTERNAL_ERROR
                    ));

                    logger.error()
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                        .log();
                }
            }

            // Create summary
            BulkUserRoleResponse.BulkOperationSummary summary = new BulkUserRoleResponse.BulkOperationSummary(
                request.userIds().size(),
                successfulAssignments.size(),
                failedAssignments.size(),
                request.roleId(),
                role.getRoleName()
            );

            logger.info()
                .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS, String.valueOf(summary.totalUsers()))
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS, String.valueOf(summary.successCount()))
                .detail(AppConstants.DETAIL_KEY_FAILED_ASSIGNMENTS, String.valueOf(summary.failureCount()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_RESPONSE)
                .log();

            return new BulkUserRoleResponse(successfulAssignments, failedAssignments, summary);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_PROCESS_BULK_USER_ROLE_ASSIGNMENT)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Removes a role from a user within the current tenant.
     * 
     * <p><strong>Cache Eviction:</strong> Clears all permission caches when a role is removed
     * to ensure users immediately lose the role's permissions.
     * 
     * @param request the role removal request
     * @return true if the role was successfully removed, false if assignment didn't exist
     * @throws IllegalArgumentException if user or role does not exist or belongs to different tenant
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Override
    public boolean removeRole(RemoveRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_PROCESSING_USER_ROLE_REMOVAL_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_REQUEST)
                .log();

            // Validate that the role exists and belongs to the tenant
            roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Validate that the user exists and belongs to the tenant
            validateUserExistsInTenant(request.userId(), tenantId);

            // Remove the user-role assignment
            int deletedCount = userRoleRepository.deleteByUserIdAndRoleIdAndTenantId(request.userId(), request.roleId(), tenantId);
            
            boolean removed = deletedCount > 0;
            
            logger.info()
                .message(AppConstants.LOG_USER_ROLE_REMOVAL_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_REMOVED, String.valueOf(removed))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL)
                .log();

            return removed;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_REMOVE_ROLE_FROM_USER)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Retrieves all roles assigned to a specific user within the current tenant.
     * 
     * @param userId the user ID to get roles for
     * @return list of user-role assignment responses
     * @throws IllegalArgumentException if user does not exist or belongs to different tenant
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRoles(UUID userId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_RETRIEVING_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLES_QUERY)
                .log();

            // Note: User validation removed to support cross-service queries
            // It's acceptable for a user to have zero roles or not exist in user-service
            
            // Get user-role assignments
            List<UserRole> userRoles = userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
            
            return userRoles.stream()
                .map(this::mapToUserRoleResponseWithRoleDetails)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLES_QUERY_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Retrieves user roles with enriched details including role type, permissions count, risk level, etc.
     * This method returns complete role information needed for UI display.
     *
     * @param userId the user ID to get roles for
     * @return list of maps containing complete role details
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserRolesEnriched(UUID userId) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Retrieving enriched user roles")
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .log();
            
            List<UserRole> userRoles = userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
            
            return userRoles.stream()
                .map(this::mapToEnrichedUserRoleResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve enriched user roles")
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .log();
            throw e;
        }
    }

    /**
     * Retrieves all users assigned to a specific role within the current tenant.
     * 
     * @param roleId the role ID to get users for
     * @return list of user-role assignment responses
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getRoleUsers(UUID roleId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_RETRIEVING_ROLE_USERS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_USERS_QUERY)
                .log();

            // Validate that the role exists and belongs to the tenant
            roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Get user-role assignments
            List<UserRole> userRoles = userRoleRepository.findByRoleIdAndTenantId(roleId, tenantId);
            
            return userRoles.stream()
                .map(this::mapToUserRoleResponseWithRoleDetails)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_ROLE_USERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_USERS_QUERY_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Retrieves all user-role assignments within the current tenant.
     * 
     * @return list of all user-role assignment responses
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getAllUserRoles() {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_RETRIEVING_ALL_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ALL_USER_ROLES_QUERY)
                .log();

            // Get all user-role assignments for the tenant
            List<UserRole> userRoles = userRoleRepository.findByTenantId(tenantId);
            
            return userRoles.stream()
                .map(this::mapToUserRoleResponseWithRoleDetails)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_ALL_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ALL_USER_ROLES_QUERY_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Checks if a user has a specific role within the current tenant.
     * 
     * @param userId the user ID to check
     * @param roleId the role ID to check
     * @return true if the user has the role, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, UUID roleId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            return hasRole(userId, roleId, tenantId);
        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_CHECK_USER_ROLE)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_CHECK_ERROR)
                .log();
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, UUID roleId, String tenantId) {
        try {
            return userRoleRepository.existsByUserIdAndRoleIdAndTenantId(userId, roleId, tenantId);
        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_CHECK_USER_ROLE)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_CHECK_ERROR)
                .log();
            return false;
        }
    }

    /**
     * Gets all users that have a specific role within the given tenant.
     * Used for role reconciliation during dynamic rule updates.
     * CRITICAL: Uses new transaction and clears cache to ensure absolutely fresh data.
     * 
     * @param roleId the role ID to get users for
     * @param tenantId the tenant ID to scope the search to
     * @return list of user IDs that have the role
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<UUID> getUsersWithRole(UUID roleId, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_GETTING_USERS_WITH_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_QUERY)
                .log();

            // CRITICAL: Clear entity manager cache to ensure absolutely fresh data
            // This prevents Hibernate from returning cached results during reconciliation
            entityManager.clear();
            
            // Get user-role assignments for the role and tenant
            // Repository query uses native SQL with @QueryHints to bypass ALL caching
            List<UserRole> userRoles = userRoleRepository.findByRoleIdAndTenantId(roleId, tenantId);
            
            List<UUID> userIds = userRoles.stream()
                .map(UserRole::getUserId)
                .collect(Collectors.toList());

            logger.info()
                .message(AppConstants.LOG_RETRIEVED_USERS_WITH_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_USER_COUNT_DETAIL, String.valueOf(userIds.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_RESULT)
                .log();

            return userIds;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_GET_USERS_WITH_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Removes a role from a specific user within the given tenant.
     * Used for role reconciliation during dynamic rule updates.
     * 
     * @param userId the user ID to remove the role from
     * @param roleId the role ID to remove
     * @param tenantId the tenant ID to scope the operation to
     * @return true if the role was removed, false if the assignment didn't exist
     */
    public boolean unassignRole(UUID userId, UUID roleId, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_UNASSIGNING_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN)
                .log();

            // Remove the user-role assignment
            int deletedCount = userRoleRepository.deleteByUserIdAndRoleIdAndTenantId(userId, roleId, tenantId);
            
            boolean removed = deletedCount > 0;
            
            logger.info()
                .message(AppConstants.LOG_ROLE_UNASSIGNMENT_COMPLETED_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_REMOVED, String.valueOf(removed))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_RESULT)
                .log();

            return removed;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_UNASSIGN_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * SAFELY removes a role from a specific user ONLY if it has DYNAMIC assignment type.
     * This is CRITICAL for dynamic rule reconciliation to avoid removing MANUAL assignments.
     * 
     * <p><b>Use Case:</b> When a dynamic rule is disabled/deleted or a user no longer matches
     * the rule conditions, we should only remove the DYNAMIC assignment. If the user also
     * has a MANUAL assignment (admin explicitly assigned), it should NOT be removed.</p>
     * 
     * @param userId the user ID to remove the role from
     * @param roleId the role ID to remove
     * @param tenantId the tenant ID to scope the operation to
     * @return true if the DYNAMIC assignment was removed, false if no DYNAMIC assignment existed
     */
    public boolean unassignDynamicRole(UUID userId, UUID roleId, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_UNASSIGNING_DYNAMIC_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ASSIGNMENT_TYPE, AssignmentType.DYNAMIC.name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN)
                .log();

            // ONLY remove if assignment type is DYNAMIC - this protects MANUAL assignments
            int deletedCount = userRoleRepository.deleteByUserIdAndRoleIdAndTenantIdAndAssignmentType(
                userId, 
                roleId, 
                tenantId,
                AssignmentType.DYNAMIC
            );
            
            boolean removed = deletedCount > 0;
            
            if (removed) {
                logger.info()
                    .message(AppConstants.LOG_SUCCESSFULLY_REMOVED_DYNAMIC_ROLE_ASSIGNMENT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_REMOVED, "true")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_RESULT)
                    .log();
            } else {
                logger.info()
                    .message(AppConstants.LOG_NO_DYNAMIC_ASSIGNMENT_FOUND_USER_MAY_HAVE_MANUAL)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_REMOVED, "false")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_RESULT)
                    .log();
            }

            return removed;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_UNASSIGN_DYNAMIC_ROLE_FOR_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Removes a role from a specific user within the current tenant.
     * Convenience method that extracts tenant from JWT context.
     * 
     * @param userId the user ID to remove the role from
     * @param roleId the role ID to remove
     */
    @Override
    public void unassignRole(UUID userId, UUID roleId) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();
        unassignRole(userId, roleId, tenantId);
    }

    /**
     * Removes all role assignments for a specific user within the current tenant.
     * 
     * @param userId the user ID to remove all roles from
     * @return number of role assignments removed
     * @throws IllegalArgumentException if user does not exist or belongs to different tenant
     */
    @Override
    public int removeAllUserRoles(UUID userId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_REMOVING_ALL_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_REMOVE_ALL_USER_ROLES)
                .log();

            // Validate that the user exists and belongs to the tenant
            validateUserExistsInTenant(userId, tenantId);

            // Remove all role assignments for the user
            int deletedCount = userRoleRepository.deleteByUserIdAndTenantId(userId, tenantId);
            
            logger.info()
                .message(AppConstants.LOG_ALL_USER_ROLES_REMOVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_DELETED_COUNT, String.valueOf(deletedCount))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ALL_USER_ROLES_REMOVED)
                .log();

            return deletedCount;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_REMOVE_ALL_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_REMOVE_ALL_USER_ROLES_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Removes all user assignments for a specific role within the current tenant.
     * 
     * @param roleId the role ID to remove all users from
     * @return number of user assignments removed
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    @Override
    public int removeAllRoleUsers(UUID roleId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_REMOVING_ALL_ROLE_USERS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_REMOVE_ALL_ROLE_USERS)
                .log();

            // Validate that the role exists and belongs to the tenant
            roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Remove all user assignments for the role
            int deletedCount = userRoleRepository.deleteByRoleIdAndTenantId(roleId, tenantId);
            
            logger.info()
                .message(AppConstants.LOG_ALL_ROLE_USERS_REMOVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_DELETED_COUNT, String.valueOf(deletedCount))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ALL_ROLE_USERS_REMOVED)
                .log();

            return deletedCount;

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_REMOVE_ALL_ROLE_USERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_REMOVE_ALL_ROLE_USERS_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Validates that a user exists and belongs to the current tenant.
     * 
     * @param userId the user ID to validate
     * @param tenantId the tenant ID to validate against
     * @throws IllegalArgumentException if user does not exist or belongs to different tenant
     */
    private void validateUserExistsInTenant(UUID userId, String tenantId) {
        try {
            var users = userGrpcClient.getUserListByTenant(tenantId);
            boolean userExists = users.stream()
                .anyMatch(user -> user.getId().equals(userId.toString()));
            
            if (!userExists) {
                throw new IllegalArgumentException(AppConstants.USER_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT);
            }
        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_VALIDATE_USER_EXISTENCE)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_VALIDATION_ERROR)
                .log();
            throw new IllegalArgumentException(AppConstants.UNABLE_TO_VALIDATE_USER_EXISTENCE + ": " + e.getMessage());
        }
    }

    /**
     * Maps a UserRole entity to a UserRoleResponse DTO with role details.
     * 
     * @param userRole the user role entity
     * @return the user role response DTO
     */
    private UserRoleResponse mapToUserRoleResponseWithRoleDetails(UserRole userRole) {
        // Get role details
        Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
        
        //Count permissions for this role
        Long permissionCount = role != null ? rolePermissionRepository.countByRoleId(role.getId()) : 0L;
        
        // Convert Instant to LocalDateTime
        LocalDateTime assignedAtLocal = userRole.getAssignedAt() != null 
            ? LocalDateTime.ofInstant(userRole.getAssignedAt(), java.time.ZoneId.systemDefault())
            : LocalDateTime.now();
        
        return new UserRoleResponse(
            userRole.getId(),
            userRole.getUserId(),
            userRole.getRoleId(),
            userRole.getTenantId(),
            role != null ? role.getRoleName() : AppConstants.DEFAULT_UNKNOWN_ROLE,
            role != null ? role.getDescription() : AppConstants.DEFAULT_ROLE_DETAILS_NOT_AVAILABLE,
            role != null ? role.getIsActive() : true,
            assignedAtLocal
        );
    }

    /**
     * Maps UserRole to enriched Map with complete role details.
     */
    private Map<String, Object> mapToEnrichedUserRoleResponse(UserRole userRole) {
        Map<String, Object> roleData = new HashMap<>();
        
        // Get full role details
        Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
        
        if (role == null) {
            // Return minimal data if role not found
            roleData.put("roleId", userRole.getRoleId());
            roleData.put("roleName", AppConstants.DEFAULT_UNKNOWN_ROLE);
            roleData.put("roleDescription", AppConstants.DEFAULT_ROLE_DETAILS_NOT_AVAILABLE);
            roleData.put("roleActive", false);
            roleData.put("permissionCount", 0);
            roleData.put("assignedAt", userRole.getAssignedAt());
            return roleData;
        }
        
        // Count permissions
        Long permissionCount = rolePermissionRepository.countByRoleId(role.getId());
        
        // Build enriched response
        roleData.put("id", userRole.getId()); // user_role assignment ID
        roleData.put("roleId", role.getId());
        roleData.put("roleName", role.getRoleName());
        roleData.put("roleDescription", role.getDescription());
        roleData.put("roleType", role.getRoleType() != null ? role.getRoleType().name() : null);
        roleData.put("riskLevel", role.getRiskLevel() != null ? role.getRiskLevel().name() : null);
        roleData.put("roleActive", role.getIsActive());
        roleData.put("isCritical", role.getRiskLevel() != null && "High".equalsIgnoreCase(role.getRiskLevel().name()));
        roleData.put("permissionCount", permissionCount != null ? permissionCount.intValue() : 0);
        roleData.put("assignedAt", userRole.getAssignedAt());
        roleData.put("assignmentType", userRole.getAssignmentType() != null ? userRole.getAssignmentType().name() : "MANUAL");
        roleData.put("businessJustification", role.getBusinessJustification());
        roleData.put("tags", role.getTags());
        
        return roleData;
    }

    /**
     * Maps a UserRole entity to a UserRoleResponse DTO with provided role details.
     * 
     * @param userRole the user role entity
     * @param role the role entity
     * @return the user role response DTO
     */
    private UserRoleResponse mapToUserRoleResponse(UserRole userRole, Role role) {
        // Convert Instant to LocalDateTime
        LocalDateTime assignedAtLocal = userRole.getAssignedAt() != null 
            ? LocalDateTime.ofInstant(userRole.getAssignedAt(), java.time.ZoneId.systemDefault())
            : LocalDateTime.now();
            
        return new UserRoleResponse(
            userRole.getId(),
            userRole.getUserId(),
            userRole.getRoleId(),
            userRole.getTenantId(),
            role.getRoleName(),
            role.getDescription(),
            role.getIsActive() != null ? role.getIsActive() : true,
            assignedAtLocal
        );
    }
    
    /**
     * Revokes a role from multiple users within the current tenant.
     * 
     * @param request the bulk role revocation request
     * @return BulkRoleRevocationResponse with details of successful and failed revocations
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    @Override
    public BulkRoleRevocationResponse revokeRole(RevokeRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_PROCESSING_BULK_ROLE_REVOCATION_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_IS_BULK_REVOCATION, request.isBulkRevocation())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_REQUEST)
                .log();

            // Validate that the role exists and belongs to the tenant
            Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Validate that all users exist and belong to the tenant
            for (UUID userId : request.userIds()) {
                validateUserExistsInTenant(userId, tenantId);
            }

            // Process revocations
            List<UUID> successfulRevocations = new ArrayList<>();
            List<UUID> failedRevocations = new ArrayList<>();
            
            for (UUID userId : request.userIds()) {
                try {
                    // Check if user actually has the role
                    boolean hadRole = userRoleRepository.existsByUserIdAndRoleIdAndTenantId(userId, request.roleId(), tenantId);
                    
                    if (hadRole) {
                        // Remove the role assignment
                        int deletedCount = userRoleRepository.deleteByUserIdAndRoleIdAndTenantId(userId, request.roleId(), tenantId);
                        if (deletedCount > 0) {
                            successfulRevocations.add(userId);
                            logger.debug()
                                .message(AppConstants.LOG_SUCCESSFULLY_REVOKED_ROLE_FROM_USER)
                                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                                .log();
                        } else {
                            failedRevocations.add(userId);
                            logger.warn()
                                .message(AppConstants.LOG_FAILED_TO_REVOKE_ROLE_NO_ASSIGNMENT_FOUND)
                                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                                .log();
                        }
                    } else {
                        // User didn't have the role - treat as successful (idempotent)
                        successfulRevocations.add(userId);
                        logger.debug()
                            .message(AppConstants.LOG_USER_DIDNT_HAVE_ROLE_TREATING_AS_SUCCESSFUL_REVOCATION)
                            .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                            .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                            .log();
                    }
                    
                } catch (Exception e) {
                    failedRevocations.add(userId);
                    logger.error(e)
                        .message(AppConstants.LOG_FAILED_TO_REVOKE_ROLE_FROM_USER)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .log();
                }
            }
            
            BulkRoleRevocationResponse response = new BulkRoleRevocationResponse(
                request.roleId(),
                role.getRoleName(),
                request.userIds(),
                successfulRevocations,
                failedRevocations,
                request.getUserCount(),
                successfulRevocations.size(),
                failedRevocations.size()
            );
            
            logger.info()
                .message(AppConstants.LOG_BULK_ROLE_REVOCATION_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_REQUESTED, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_TOTAL_SUCCESSFUL, successfulRevocations.size())
                .detail(AppConstants.DETAIL_KEY_TOTAL_FAILED, failedRevocations.size())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_COMPLETED)
                .log();

            return response;

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_PROCESS_BULK_ROLE_REVOCATION_REQUEST)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_ERROR)
                .log();
            throw e;
        }
    }

    /**
     * Assigns a role to a user or multiple users with a specific assignment type (MANUAL or DYNAMIC).
     * This method is primarily used by the dynamic rule reconciliation service
     * to create DYNAMIC assignments. Supports both single and bulk assignments.
     * 
     * @param request the role assignment request (single or bulk)
     * @param tenantId the tenant ID for the assignment
     * @param assignmentType the type of assignment (MANUAL or DYNAMIC)
     * @return the assignment response - single UserRoleResponse for single assignment, 
     *         or BulkUserRoleResponse for bulk assignment
     * @throws IllegalArgumentException if user(s) or role does not exist or belongs to different tenant
     * @throws IllegalStateException if the user already has the role assigned (single assignment only)
     */
    @Override
    public Object assignRoleWithType(AssignRoleRequest request, String tenantId, AssignmentType assignmentType) {
        
        // Check if this is a bulk assignment (userIds provided) or single assignment (userId provided)
        if (request.userIds() != null && !request.userIds().isEmpty()) {
            // Bulk assignment
            return processBulkAssignmentWithType(request, tenantId, assignmentType);
        } else if (request.userId() != null) {
            // Single assignment
            return processSingleAssignmentWithType(request, tenantId, assignmentType);
        } else {
            throw new IllegalArgumentException("Either userId or userIds must be provided in the assignment request");
        }
    }
    
    /**
     * Processes a single user role assignment with specified assignment type.
     */
    private UserRoleResponse processSingleAssignmentWithType(AssignRoleRequest request, String tenantId, AssignmentType assignmentType) {
        try {
            logger.debug()
                .message("Assigning role with specific assignment type")
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail("assignmentType", assignmentType.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .log();

            // Validate that the role exists and belongs to the tenant
            Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            // Validate that the user exists and belongs to the tenant
            validateUserExistsInTenant(request.userId(), tenantId);

            // Check if the user already has this role assigned
            if (userRoleRepository.existsByUserIdAndRoleIdAndTenantId(request.userId(), request.roleId(), tenantId)) {
                throw new IllegalStateException(AppConstants.USER_ALREADY_HAS_THIS_ROLE_ASSIGNED);
            }

            // Create the user-role assignment with specified assignment type
            UserRole userRole = new UserRole(
                request.userId(), 
                request.roleId(), 
                tenantId, 
                assignmentType
            );
            UserRole savedUserRole = userRoleRepository.save(userRole);

            logger.info()
                .message("Successfully assigned role with assignment type")
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail("assignmentType", assignmentType.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT)
                .log();

            return mapToUserRoleResponse(savedUserRole, role);

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to assign role with assignment type")
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail("assignmentType", assignmentType.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .log();
            throw new RuntimeException("Failed to assign role with assignment type: " + e.getMessage(), e);
        }
    }
    
    /**
     * Processes bulk user role assignments with specified assignment type.
     */
    private BulkUserRoleResponse processBulkAssignmentWithType(AssignRoleRequest request, String tenantId, AssignmentType assignmentType) {
        try {
            logger.info()
                .message("Processing bulk user role assignment request with assignment type")
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(request.userIds().size()))
                .detail("assignmentType", assignmentType.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_REQUEST)
                .log();

            // Validate that the role exists and belongs to the tenant
            Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT));

            List<UserRoleResponse> successfulAssignments = new ArrayList<>();
            List<BulkUserRoleResponse.FailedAssignment> failedAssignments = new ArrayList<>();
            
            // Get users who already have this role to avoid duplicate attempts
            Set<UUID> usersWithRole = new HashSet<>(userRoleRepository.findUserIdsByRoleIdAndTenantId(request.roleId(), tenantId));
            
            for (UUID userId : request.userIds()) {
                try {
                    // Check if user already has the role
                    if (usersWithRole.contains(userId)) {
                        failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                            userId,
                            AppConstants.USER_ALREADY_HAS_THIS_ROLE_ASSIGNED,
                            AppConstants.ERROR_CODE_ROLE_ALREADY_ASSIGNED
                        ));
                        continue;
                    }

                    // Create the user-role assignment with specified assignment type
                    UserRole userRole = new UserRole(
                        userId, 
                        request.roleId(), 
                        tenantId, 
                        assignmentType
                    );
                    UserRole savedUserRole = userRoleRepository.save(userRole);
                    successfulAssignments.add(mapToUserRoleResponse(savedUserRole, role));
                    
                    // Add to the set to prevent duplicate attempts in the same batch
                    usersWithRole.add(userId);

                    logger.debug()
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNED_SUCCESSFULLY)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_USER_ROLE_ID, savedUserRole.getId().toString())
                        .detail("assignmentType", assignmentType.toString())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT)
                        .log();

                } catch (IllegalArgumentException e) {
                    String errorCode = e.getMessage().contains(AppConstants.ERROR_DETECTION_USER_NOT_FOUND) ? AppConstants.ERROR_CODE_USER_NOT_FOUND : 
                                     e.getMessage().contains(AppConstants.ERROR_DETECTION_DOES_NOT_BELONG_TO_TENANT) ? AppConstants.ERROR_CODE_USER_NOT_IN_TENANT : AppConstants.ERROR_CODE_VALIDATION_ERROR;
                    failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                        userId,
                        e.getMessage(),
                        errorCode
                    ));
                    logger.warn()
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                        .log();
                } catch (Exception e) {
                    failedAssignments.add(new BulkUserRoleResponse.FailedAssignment(
                        userId,
                        AppConstants.FAILED_TO_ASSIGN_ROLE_TO_USER,
                        AppConstants.ERROR_CODE_INTERNAL_ERROR
                    ));
                    logger.error(e)
                        .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                        .log();
                }
            }
            
            // Create summary
            BulkUserRoleResponse.BulkOperationSummary summary = new BulkUserRoleResponse.BulkOperationSummary(
                request.userIds().size(),
                successfulAssignments.size(),
                failedAssignments.size(),
                request.roleId(),
                role.getRoleName()
            );
            
            logger.info()
                .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS, String.valueOf(summary.totalUsers()))
                .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS, String.valueOf(summary.successCount()))
                .detail(AppConstants.DETAIL_KEY_FAILED_ASSIGNMENTS, String.valueOf(summary.failureCount()))
                .detail("assignmentType", assignmentType.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_RESPONSE)
                .log();

            return new BulkUserRoleResponse(successfulAssignments, failedAssignments, summary);

        } catch (Exception e) {
            logger.error(e)
                .message("Failed to process bulk role assignment with assignment type")
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(request.userIds().size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail("assignmentType", assignmentType.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR)
                .log();
            throw e;
        }
    }
    
    @Override
    public Map<String, Object> submitUserRoleAssignmentForApproval(SubmitUserRoleAssignmentForApprovalRequest request) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            String initiatorUsername = tenantContextService.getCurrentUsername();
            UUID initiatorId = UUID.fromString(tenantContextService.getCurrentUserId());
            
            logger.info()
                .message("Submitting user role assignment for approval")
                .detail("tenantId", tenantId)
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("workflowId", request.getWorkflowId())
                .detail("initiatorId", initiatorId.toString())
                .objectType("UserRoleAssignmentApprovalSubmission")
                .log();
            
            // Validate role exists and belongs to tenant
            Role role = roleRepository.findByIdAndTenantId(request.getRoleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found or does not belong to tenant"));
            
            // Get owner IDs for approval workflow
            List<UUID> ownerIds = roleOwnersRepository.findByRoleId(request.getRoleId())
                .stream()
                .map(RoleOwners::getUserId)
                .collect(Collectors.toList());
            if (ownerIds.isEmpty()) {
                throw new IllegalArgumentException("Role must have owners for approval workflow");
            }
            
            // Publish event to trigger workflow
            roleEventPublisher.publishUserRoleAssignmentPendingEvent(
                role.getId(),
                role.getRoleName(),
                request.getUserIds(),
                request.getJustification(),
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                initiatorUsername,
                initiatorId,
                request.shouldSendNotification()
            );
            
            logger.info()
                .message("User role assignment approval event published successfully")
                .detail("tenantId", tenantId)
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("workflowId", request.getWorkflowId())
                .objectType("UserRoleAssignmentApprovalSubmission")
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User role assignment submitted for approval successfully");
            response.put("roleId", role.getId().toString());
            response.put("roleName", role.getRoleName());
            response.put("userCount", request.getUserIds().size());
            response.put("workflowId", request.getWorkflowId());
            
            return response;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit user role assignment for approval")
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("error", e.getMessage())
                .objectType("UserRoleAssignmentApprovalSubmission")
                .log();
            throw e;
        }
    }
    
    @Override
    public Map<String, Object> submitUserRoleRevocationForApproval(SubmitUserRoleRevocationForApprovalRequest request) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            String initiatorUsername = tenantContextService.getCurrentUsername();
            UUID initiatorId = UUID.fromString(tenantContextService.getCurrentUserId());
            
            logger.info()
                .message("Submitting user role revocation for approval")
                .detail("tenantId", tenantId)
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("workflowId", request.getWorkflowId())
                .detail("initiatorId", initiatorId.toString())
                .objectType("UserRoleRevocationApprovalSubmission")
                .log();
            
            // Validate role exists and belongs to tenant
            Role role = roleRepository.findByIdAndTenantId(request.getRoleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found or does not belong to tenant"));
            
            // Get owner IDs for approval workflow
            List<UUID> ownerIds = roleOwnersRepository.findByRoleId(request.getRoleId())
                .stream()
                .map(RoleOwners::getUserId)
                .collect(Collectors.toList());
            if (ownerIds.isEmpty()) {
                throw new IllegalArgumentException("Role must have owners for approval workflow");
            }
            
            // Publish event to trigger workflow
            roleEventPublisher.publishUserRoleRevocationPendingEvent(
                role.getId(),
                role.getRoleName(),
                request.getUserIds(),
                request.getJustification(),
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                initiatorUsername,
                initiatorId,
                request.shouldSendNotification()
            );
            
            logger.info()
                .message("User role revocation approval event published successfully")
                .detail("tenantId", tenantId)
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("workflowId", request.getWorkflowId())
                .objectType("UserRoleRevocationApprovalSubmission")
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User role revocation submitted for approval successfully");
            response.put("roleId", role.getId().toString());
            response.put("roleName", role.getRoleName());
            response.put("userCount", request.getUserIds().size());
            response.put("workflowId", request.getWorkflowId());
            
            return response;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit user role revocation for approval")
                .detail("roleId", request.getRoleId().toString())
                .detail("userCount", String.valueOf(request.getUserIds().size()))
                .detail("error", e.getMessage())
                .objectType("UserRoleRevocationApprovalSubmission")
                .log();
            throw e;
        }
    }

    /**
     * Assigns multiple roles to multiple users (M×N assignment).
     *
     * <p><b>Processing Strategy:</b></p>
     * <ul>
     *   <li>Small batches (<1000 assignments): Synchronous processing</li>
     *   <li>Large batches (≥1000 assignments): Asynchronous via Kafka</li>
     * </ul>
     *
     * <p><b>Example:</b> 10 roles × 500 users = 5,000 assignments → Async processing</p>
     *
     * @param request the bulk multi-role assignment request containing roleIds and userIds
     * @return BulkMultiRoleAssignmentResponse with batch job details
     * @throws IllegalArgumentException if roles or users do not exist or belong to different tenant
     */
    @Override
    public BulkMultiRoleAssignmentResponse assignMultipleRolesToMultipleUsers(
            BulkMultiRoleAssignmentRequest request) {

        try {
            // Step 1: Extract tenant context from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            UUID currentUserId = UUID.fromString(tenantContextService.getCurrentUserId());
            String currentUsername = tenantContextService.getCurrentUsername();

            // Calculate total assignments: M roles × N users
            int totalAssignments = request.getTotalAssignments();

            logger.info()
                    .message("Processing bulk multi-role assignment request")
                    .detail("tenantId", tenantId)
                    .detail("roleCount", String.valueOf(request.roleIds().size()))
                    .detail("userCount", String.valueOf(request.userIds().size()))
                    .detail("totalAssignments", String.valueOf(totalAssignments))
                    .detail("processAsync", String.valueOf(request.shouldProcessAsync()))
                    .objectType("BulkMultiRoleAssignmentRequest")
                    .log();

            // Step 2: Validate all roles exist and belong to tenant
            // This prevents processing if any role is invalid
            List<Role> roles = validateAndFetchRoles(request.roleIds(), tenantId);

            // Step 3: Validate all users exist and belong to tenant
            // This prevents processing if any user is invalid
            validateUsersExistInTenant(request.userIds(), tenantId);

            // Step 4: Create batch job record to track progress
            BatchJob batchJob = createBatchJob(
                    tenantId,
                    totalAssignments,
                    currentUserId,
                    currentUsername,
                    request
            );

            // Step 5: Decide processing strategy based on batch size
            if (request.shouldProcessAsync()) {
                // ASYNC: Publish to Kafka for background processing
                return processAsyncBatchAssignment(batchJob, request, tenantId);
            } else {
                // SYNC: Process immediately and return full results
                return processSyncBatchAssignment(batchJob, request, tenantId, roles);
            }

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to process bulk multi-role assignment")
                    .detail("roleCount", String.valueOf(request.roleIds().size()))
                    .detail("userCount", String.valueOf(request.userIds().size()))
                    .detail("error", e.getMessage())
                    .objectType("BulkMultiRoleAssignmentError")
                    .log();
            throw e;
        }
    }

    /**
     * Validates that all role IDs exist and belong to the tenant.
     *
     * <p><b>Why validate upfront?</b> Prevents wasting resources on invalid batch jobs.</p>
     *
     * @param roleIds list of role UUIDs to validate
     * @param tenantId tenant identifier for security check
     * @return list of validated Role entities
     * @throws IllegalArgumentException if any role is invalid or doesn't belong to tenant
     */
    private List<Role> validateAndFetchRoles(List<UUID> roleIds, String tenantId) {
        logger.debug()
                .message("Validating roles for bulk assignment")
                .detail("roleCount", String.valueOf(roleIds.size()))
                .detail("tenantId", tenantId)
                .log();

        // Fetch all roles in one query (efficient batch lookup)
        List<Role> roles = roleRepository.findAllById(roleIds);

        // Check if all requested roles were found
        if (roles.size() != roleIds.size()) {
            Set<UUID> foundRoleIds = roles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            // Find which roles are missing
            List<UUID> missingRoleIds = roleIds.stream()
                    .filter(id -> !foundRoleIds.contains(id))
                    .collect(Collectors.toList());

            logger.error()
                    .message("Some roles not found")
                    .detail("missingRoleIds", missingRoleIds.toString())
                    .log();

            throw new IllegalArgumentException(
                    "Roles not found: " + missingRoleIds
            );
        }

        // Verify all roles belong to the tenant (security check)
        List<Role> invalidTenantRoles = roles.stream()
                .filter(role -> !role.getTenantId().equals(tenantId))
                .collect(Collectors.toList());

        if (!invalidTenantRoles.isEmpty()) {
            logger.error()
                    .message("Some roles do not belong to tenant")
                    .detail("invalidRoleIds", invalidTenantRoles.stream()
                            .map(r -> r.getId().toString())
                            .collect(Collectors.joining(", ")))
                    .log();

            throw new IllegalArgumentException(
                    "Some roles do not belong to tenant: " +
                            invalidTenantRoles.stream()
                                    .map(Role::getRoleName)
                                    .collect(Collectors.joining(", "))
            );
        }

        logger.debug()
                .message("All roles validated successfully")
                .detail("roleCount", String.valueOf(roles.size()))
                .log();

        return roles;
    }

    /**
     * Validates that all user IDs exist and belong to the tenant.
     *
     * <p><b>Implementation Note:</b> Uses gRPC call to auth-service to verify users.</p>
     *
     * @param userIds list of user UUIDs to validate
     * @param tenantId tenant identifier for security check
     * @throws IllegalArgumentException if any user is invalid or doesn't belong to tenant
     */
    private void validateUsersExistInTenant(List<UUID> userIds, String tenantId) {
        logger.debug()
                .message("Validating users for bulk assignment")
                .detail("userCount", String.valueOf(userIds.size()))
                .detail("tenantId", tenantId)
                .log();

        try {
            // Fetch all users for tenant via gRPC
            var tenantUsers = userGrpcClient.getUserListByTenant(tenantId);

            // Create a Set of tenant user IDs for O(1) lookup
            Set<UUID> tenantUserIds = tenantUsers.stream()
                    .map(user -> UUID.fromString(user.getId()))
                    .collect(Collectors.toSet());

            // Find users that don't exist in tenant
            List<UUID> invalidUserIds = userIds.stream()
                    .filter(userId -> !tenantUserIds.contains(userId))  // checking UUID in UUID Set
                    .collect(Collectors.toList());

            if (!invalidUserIds.isEmpty()) {
                logger.error()
                        .message("Tenant Ids")
                        .detail("Valid user ids:", tenantUserIds.toString())
                        .detail("tenantId", tenantId)
                        .log();

                logger.error()
                        .message("Some users not found or do not belong to tenant")
                        .detail("invalidUserIds", invalidUserIds.toString())
                        .detail("tenantId", tenantId)
                        .log();

                throw new IllegalArgumentException(
                        "Users not found or do not belong to tenant: " + invalidUserIds
                );
            }

            logger.debug()
                    .message("All users validated successfully")
                    .detail("userCount", String.valueOf(userIds.size()))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to validate users")
                    .detail("userCount", String.valueOf(userIds.size()))
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();

            throw new IllegalArgumentException(
                    "Unable to validate users: " + e.getMessage(), e
            );
        }
    }

    /**
     * Creates a batch job record in the database.
     *
     * <p>This record tracks the progress and status of the async operation.</p>
     *
     * @param tenantId tenant identifier
     * @param totalAssignments total number of assignments to be created
     * @param createdById ID of the user creating the batch
     * @param createdByName name of the user creating the batch
     * @param request the original request for metadata storage
     * @return saved BatchJob entity
     */
    private BatchJob createBatchJob(
            String tenantId,
            int totalAssignments,
            UUID createdById,
            String createdByName,
            BulkMultiRoleAssignmentRequest request) {

        // Build metadata JSON containing request details
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("roleIds", request.roleIds());
        metadata.put("userIds", request.userIds());
        metadata.put("justification", request.justification());
        metadata.put("validityDays", request.validityDays());
        metadata.put("sendNotification", request.shouldSendNotification());
        metadata.put("assignmentType", "MANUAL");

        // Create batch job entity with PENDING status
        BatchJob batchJob = new BatchJob(
                tenantId,
                "MULTI_ROLE_ASSIGNMENT",
                "PENDING",
                totalAssignments,
                createdById,
                createdByName,
                metadata
        );

        // Save to database
        BatchJob savedBatchJob = batchJobRepository.save(batchJob);

        logger.info()
                .message("Batch job created")
                .detail("batchId", savedBatchJob.getId().toString())
                .detail("totalAssignments", String.valueOf(totalAssignments))
                .detail("status", "PENDING")
                .objectType("BatchJobCreated")
                .log();

        return savedBatchJob;
    }

    /**
     * Processes batch assignment asynchronously via Kafka.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Publish batch job to Kafka topic</li>
     *   <li>Worker consumes message</li>
     *   <li>Worker processes assignments in chunks</li>
     *   <li>Worker updates batch job status</li>
     * </ol>
     *
     * @param batchJob the batch job entity
     * @param request the original request
     * @param tenantId tenant identifier
     * @return response with batch ID for tracking
     */
    private BulkMultiRoleAssignmentResponse processAsyncBatchAssignment(
            BatchJob batchJob,
            BulkMultiRoleAssignmentRequest request,
            String tenantId) {

        logger.info()
                .message("Publishing batch job to Kafka for async processing")
                .detail("batchId", batchJob.getId().toString())
                .detail("totalAssignments", String.valueOf(batchJob.getTotalAssignments()))
                .detail("topic", BATCH_JOB_TOPIC)
                .log();

        try {
            // Build Kafka message payload
            RoleAssignmentBatchEvent event = new RoleAssignmentBatchEvent();
            event.setBatchId(batchJob.getId());
            event.setTenantId(tenantId);
            event.setRoleIds(request.roleIds());
            event.setUserIds(request.userIds());
            event.setJustification(request.justification());
            event.setValidityDays(request.validityDays());
            event.setAssignmentType("MANUAL");

            // Publish to Kafka topic (worker will consume this)
            batchProducer.publish(event);

            logger.info()
                    .message("Batch job published to Kafka successfully")
                    .detail("batchId", batchJob.getId().toString())
                    .log();

            // Calculate estimated completion time (rough estimate: 100 assignments/second)
            int estimatedSeconds = Math.max(5, batchJob.getTotalAssignments() / 100);
            Instant estimatedCompletion = Instant.now().plusSeconds(estimatedSeconds);

            // Return response with batch ID for tracking
            return new BulkMultiRoleAssignmentResponse(
                    batchJob.getId(),
                    "PENDING",
                    batchJob.getTotalAssignments(),
                    0, // completedCount
                    0, // failedCount
                    batchJob.getCreatedAt(),
                    null, // startedAt (not started yet)
                    null, // completedAt
                    estimatedCompletion.toString(),
                    "/api/user-roles/batch-status/" + batchJob.getId()
            );

        } catch (Exception e) {
            // Update batch job status to FAILED
            batchJob.setStatus("FAILED");
            batchJob.setErrorMessage("Failed to publish to Kafka: " + e.getMessage());
            batchJobRepository.save(batchJob);

            logger.error(e)
                    .message("Failed to publish batch job to Kafka")
                    .detail("batchId", batchJob.getId().toString())
                    .detail("error", e.getMessage())
                    .log();

            throw new RuntimeException("Failed to queue batch job for processing", e);
        }
    }

    /**
     * Assigns multiple roles to multiple users based on access portal cart selections.
     *
     * <p><b>Implementation Details:</b></p>
     * <ol>
     *   <li>Validate all roles exist and belong to tenant</li>
     *   <li>Validate all users exist and belong to tenant</li>
     *   <li>Pre-fetch existing assignments to detect duplicates</li>
     *   <li>Process synchronously for <1000 assignments</li>
     *   <li>Process asynchronously for ≥1000 assignments (TODO: NOT IMPLEMENTED YET)</li>
     * </ol>
     *
     * @param request the cart-based assignment request
     * @return response with detailed assignment results
     * @throws IllegalArgumentException if validation fails
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Override
    public CartBasedAssignmentResponse assignRolesFromCart(AssignRolesFromCartRequest request) {
        try {
            // Step 1: Extract tenant context from JWT
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                    .message("Processing cart-based role assignment request")
                    .detail("tenantId", tenantId)
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("uniqueUserCount", String.valueOf(request.getUniqueUserCount()))
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .objectType("CartBasedAssignmentRequest")
                    .log();

            // Step 2: Validate request
            validateCartBasedRequest(request);

            // Step 3: Extract all unique role IDs and user IDs
            List<UUID> roleIds = List.copyOf(request.roleUserMap().keySet());
            Set<UUID> allUserIds = request.getAllUniqueUserIds();

            // Step 4: Validate all roles exist and belong to tenant
            List<Role> roles = validateAndFetchRoles(roleIds, tenantId);
            Map<UUID, Role> roleMap = roles.stream()
                    .collect(Collectors.toMap(Role::getId, role -> role));

            logger.debug()
                    .message("Roles validated successfully")
                    .detail("roleCount", String.valueOf(roles.size()))
                    .log();

            // Step 5: Validate all users exist and belong to tenant
            validateUsersExistInTenant(List.copyOf(allUserIds), tenantId);

            logger.debug()
                    .message("Users validated successfully")
                    .detail("userCount", String.valueOf(allUserIds.size()))
                    .log();

            // Step 6: Determine processing strategy
            if (request.shouldProcessAsync()) {
                // TODO: Implement async processing via Kafka for large batches
                // For now, throw an exception to inform that async is not yet implemented
                logger.warn()
                        .message("Async processing requested but not yet implemented")
                        .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                        .log();

                throw new UnsupportedOperationException(
                        "Async processing for batches ≥1000 assignments is not yet implemented. " +
                                "Current batch size: " + request.getTotalAssignments() + ". " +
                                "TODO: Implement Kafka-based async processing."
                );
            }

            // Step 7: Process synchronously for small batches
            return processSyncCartBasedAssignment(request, tenantId, roleMap);

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to process cart-based assignment")
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("userCount", String.valueOf(request.getUniqueUserCount()))
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }

    /**
     * Validates the cart-based assignment request.
     *
     * @param request the request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCartBasedRequest(AssignRolesFromCartRequest request) {
        if (request.roleUserMap() == null || request.roleUserMap().isEmpty()) {
            throw new IllegalArgumentException("Role-user map cannot be empty");
        }

        // Validate each role has at least one user
        for (Map.Entry<UUID, RoleAssignmentDetail> entry : request.roleUserMap().entrySet()) {
            UUID roleId = entry.getKey();
            RoleAssignmentDetail detail = entry.getValue();

            if (detail.userIds() == null || detail.userIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "Role " + roleId + " must have at least one user"
                );
            }
        }

        logger.debug()
                .message("Cart-based request validation passed")
                .detail("roleCount", String.valueOf(request.getRoleCount()))
                .log();
    }

    /**
     * Processes cart-based assignment synchronously for small batches.
     *
     * <p><b>Processing Steps:</b></p>
     * <ol>
     *   <li>Pre-fetch existing assignments for duplicate detection</li>
     *   <li>Iterate through each role and its users</li>
     *   <li>Create assignments with role-specific justification and validity</li>
     *   <li>Batch insert every 5000 records for performance</li>
     *   <li>Track success/failure/skipped counts per role</li>
     * </ol>
     *
     * @param request the cart-based assignment request
     * @param tenantId the tenant identifier
     * @param roleMap map of role IDs to Role entities
     * @return response with detailed results
     */
    private CartBasedAssignmentResponse processSyncCartBasedAssignment(
            AssignRolesFromCartRequest request,
            String tenantId,
            Map<UUID, Role> roleMap) {

        try {
            logger.info()
                    .message("Processing cart-based assignment synchronously")
                    .detail("tenantId", tenantId)
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .log();

            // Step 1: Pre-fetch existing assignments to detect duplicates
            List<UUID> allRoleIds = List.copyOf(request.roleUserMap().keySet());
            List<UUID> allUserIds = List.copyOf(request.getAllUniqueUserIds());

            List<UserRole> existingAssignments = userRoleRepository.findByTenantIdAndUserIdInAndRoleIdIn(
                    tenantId,
                    allUserIds,
                    allRoleIds
            );

            // Build Set for O(1) duplicate checking: "userId:roleId"
            Set<String> existingPairs = existingAssignments.stream()
                    .map(ur -> ur.getUserId() + ":" + ur.getRoleId())
                    .collect(Collectors.toSet());

            logger.debug()
                    .message("Found existing assignments (will skip)")
                    .detail("existingCount", String.valueOf(existingAssignments.size()))
                    .log();

            // Step 2: Initialize tracking structures
            List<UserRole> assignmentsToCreate = new ArrayList<>();
            int totalSuccessful = 0;
            int totalSkipped = 0;
            int totalFailed = 0;

            Map<UUID, Integer> successByRole = new HashMap<>();
            Map<UUID, List<CartBasedAssignmentResponse.RoleAssignmentFailure>> failuresByRole = new HashMap<>();

            // Step 3: Process each role and its users
            for (Map.Entry<UUID, RoleAssignmentDetail> entry : request.roleUserMap().entrySet()) {
                UUID roleId = entry.getKey();
                RoleAssignmentDetail detail = entry.getValue();
                Role role = roleMap.get(roleId);

                int roleSuccessCount = 0;
                List<CartBasedAssignmentResponse.RoleAssignmentFailure> roleFailures = new ArrayList<>();

                // Get role-specific or fallback justification and validity
                String justification = request.getJustificationForRole(roleId);
                Integer validityDays = request.getValidityDaysForRole(roleId);

                logger.debug()
                        .message("Processing role assignments")
                        .detail("roleId", roleId.toString())
                        .detail("roleName", role.getRoleName())
                        .detail("userCount", String.valueOf(detail.userIds().size()))
                        .detail("justification", justification)
                        .log();

                // Step 4: Process each user for this role
                for (UUID userId : detail.userIds()) {
                    try {
                        // Check if assignment already exists
                        String pair = userId + ":" + roleId;
                        if (existingPairs.contains(pair)) {
                            totalSkipped++;

                            logger.trace()
                                    .message("Skipping duplicate assignment")
                                    .detail("userId", userId.toString())
                                    .detail("roleId", roleId.toString())
                                    .log();

                            continue;
                        }

                        // Create new assignment with role-specific details
                        UserRole assignment = new UserRole(
                                userId,
                                roleId,
                                tenantId,
                                AssignmentType.MANUAL,
                                null, // batchId - only used for async processing
                                validityDays  // null means permanent assignment
                        );

                        assignmentsToCreate.add(assignment);
                        roleSuccessCount++;
                        totalSuccessful++;

                        // Add to existing pairs to prevent duplicate attempts in same batch
                        existingPairs.add(pair);

                        // Batch insert every 5000 records for performance
                        if (assignmentsToCreate.size() >= 5000) {
                            userRoleRepository.saveAll(assignmentsToCreate);
                            assignmentsToCreate.clear();

                            logger.debug()
                                    .message("Saved batch of assignments")
                                    .detail("savedCount", "5000")
                                    .log();
                        }

                    } catch (Exception e) {
                        totalFailed++;

                        // Record failure details
                        roleFailures.add(new CartBasedAssignmentResponse.RoleAssignmentFailure(
                                roleId,
                                userId,
                                role.getRoleName(),
                                e.getMessage(),
                                "ASSIGNMENT_ERROR"
                        ));

                        logger.warn()
                                .message("Failed to create assignment")
                                .detail("userId", userId.toString())
                                .detail("roleId", roleId.toString())
                                .detail("error", e.getMessage())
                                .log();
                    }
                }

                // Track success count for this role
                if (roleSuccessCount > 0) {
                    successByRole.put(roleId, roleSuccessCount);
                }

                // Track failures for this role
                if (!roleFailures.isEmpty()) {
                    failuresByRole.put(roleId, roleFailures);
                }
            }

            // Step 5: Save remaining assignments
            if (!assignmentsToCreate.isEmpty()) {
                userRoleRepository.saveAll(assignmentsToCreate);

                logger.debug()
                        .message("Saved final batch of assignments")
                        .detail("savedCount", String.valueOf(assignmentsToCreate.size()))
                        .log();
            }

            // Step 6: Build response
            CartBasedAssignmentResponse response = new CartBasedAssignmentResponse(
                    null, // batchId - null for sync processing
                    "COMPLETED",
                    request.getTotalAssignments(),
                    totalSuccessful,
                    totalSkipped,
                    totalFailed,
                    successByRole,
                    failuresByRole,
                    LocalDateTime.now(),
                    null, // estimatedCompletionTime - not needed for sync
                    null  // trackingUrl - not needed for sync
            );

            logger.info()
                    .message("Cart-based assignment completed successfully")
                    .detail("tenantId", tenantId)
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .detail("successful", String.valueOf(totalSuccessful))
                    .detail("skipped", String.valueOf(totalSkipped))
                    .detail("failed", String.valueOf(totalFailed))
                    .log();

            return response;

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to process sync cart-based assignment")
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }


    /**
     * Processes batch assignment synchronously (for small batches).
     *
     * <p><b>Why synchronous for small batches?</b> Avoids Kafka overhead and provides
     * immediate results to the user.</p>
     *
     * @param batchJob the batch job entity
     * @param request the original request
     * @param tenantId tenant identifier
     * @param roles pre-validated roles
     * @return response with full assignment results
     */
    private BulkMultiRoleAssignmentResponse processSyncBatchAssignment(
            BatchJob batchJob,
            BulkMultiRoleAssignmentRequest request,
            String tenantId,
            List<Role> roles) {

        logger.info()
                .message("Processing batch synchronously")
                .detail("batchId", batchJob.getId().toString())
                .detail("totalAssignments", String.valueOf(batchJob.getTotalAssignments()))
                .log();

        try {
            // Update batch job status to PROCESSING
            batchJob.setStatus("PROCESSING");
            batchJob.setStartedAt(Instant.now());
            batchJobRepository.save(batchJob);

            // Pre-fetch existing assignments to avoid duplicates
            // Query: SELECT user_id, role_id FROM user_role WHERE tenant_id = ? AND user_id IN (?) AND role_id IN (?)
            List<UserRole> existingAssignments = userRoleRepository.findByTenantIdAndUserIdInAndRoleIdIn(
                    tenantId,
                    request.userIds(),
                    request.roleIds()
            );

            // Build a Set for O(1) duplicate checking
            // Format: "userId:roleId"
            Set<String> existingPairs = existingAssignments.stream()
                    .map(ur -> ur.getUserId() + ":" + ur.getRoleId())
                    .collect(Collectors.toSet());

            logger.debug()
                    .message("Found existing assignments")
                    .detail("existingCount", String.valueOf(existingAssignments.size()))
                    .log();

            // Create assignments in batches of 5000 (database insert optimization)
            List<UserRole> assignmentsToCreate = new ArrayList<>();
            int successCount = 0;
            int skippedCount = 0;

            // Generate cartesian product: M roles × N users
            for (UUID roleId : request.roleIds()) {
                for (UUID userId : request.userIds()) {
                    // Check if assignment already exists
                    String pair = userId + ":" + roleId;
                    if (existingPairs.contains(pair)) {
                        skippedCount++;
                        continue; // Skip duplicate
                    }

                    // Create new assignment
                    UserRole assignment = new UserRole(
                            userId,
                            roleId,
                            tenantId,
                            AssignmentType.MANUAL,
                            batchJob.getId(), // Link to batch job
                            request.validityDays()
                    );

                    assignmentsToCreate.add(assignment);
                    successCount++;

                    // Batch insert every 5000 records (performance optimization)
                    if (assignmentsToCreate.size() >= 5000) {
                        userRoleRepository.saveAll(assignmentsToCreate);
                        assignmentsToCreate.clear();

                        logger.debug()
                                .message("Saved batch of assignments")
                                .detail("savedCount", "5000")
                                .log();
                    }
                }
            }

            // Save remaining assignments
            if (!assignmentsToCreate.isEmpty()) {
                userRoleRepository.saveAll(assignmentsToCreate);

                logger.debug()
                        .message("Saved final batch of assignments")
                        .detail("savedCount", String.valueOf(assignmentsToCreate.size()))
                        .log();
            }

            // Update batch job status to COMPLETED
            batchJob.setStatus("COMPLETED");
            batchJob.setCompletedCount(successCount);
            batchJob.setFailedCount(skippedCount); // Skipped = already exists
            batchJob.setCompletedAt(Instant.now());
            batchJobRepository.save(batchJob);

            logger.info()
                    .message("Batch processing completed successfully")
                    .detail("batchId", batchJob.getId().toString())
                    .detail("successCount", String.valueOf(successCount))
                    .detail("skippedCount", String.valueOf(skippedCount))
                    .log();

            // Return response
            return new BulkMultiRoleAssignmentResponse(
                    batchJob.getId(),
                    "COMPLETED",
                    batchJob.getTotalAssignments(),
                    successCount,
                    skippedCount,
                    batchJob.getCreatedAt(),
                    batchJob.getStartedAt(),
                    batchJob.getCompletedAt(),
                    null, // No estimate needed (already completed)
                    "/api/user-roles/batch-status/" + batchJob.getId()
            );

        } catch (Exception e) {
            // Update batch job status to FAILED
            batchJob.setStatus("FAILED");
            batchJob.setErrorMessage(e.getMessage());
            batchJob.setCompletedAt(Instant.now());
            batchJobRepository.save(batchJob);

            logger.error(e)
                    .message("Failed to process batch synchronously")
                    .detail("batchId", batchJob.getId().toString())
                    .detail("error", e.getMessage())
                    .log();

            throw new RuntimeException("Failed to process batch assignment", e);
        }
    }

    /**
     * Gets the status of a batch job by ID.
     * Used for polling batch progress.
     *
     * @param batchId the batch job UUID
     * @return BatchJobResponse with current status and progress
     * @throws IllegalArgumentException if batch job not found or belongs to different tenant
     */
    @Override
    @Transactional(readOnly = true)
    public BatchJobResponse getBatchJobStatus(UUID batchId) {
        try {
            // Extract tenant ID from JWT for security
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.debug()
                    .message("Fetching batch job status")
                    .detail("batchId", batchId.toString())
                    .detail("tenantId", tenantId)
                    .log();

            // Find batch job (with tenant validation for security)
            BatchJob batchJob = batchJobRepository.findByIdAndTenantId(batchId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Batch job not found or does not belong to tenant"
                    ));

            // Calculate estimated completion time if still processing
            String estimatedCompletion = null;
            if ("PROCESSING".equals(batchJob.getStatus()) && batchJob.getStartedAt() != null) {
                // Calculate based on current progress rate
                int completed = batchJob.getCompletedCount() + batchJob.getFailedCount();
                if (completed > 0) {
                    long elapsedSeconds = Duration.between(batchJob.getStartedAt(), Instant.now()).getSeconds();
                    double rate = (double) completed / elapsedSeconds; // assignments/second
                    int remaining = batchJob.getTotalAssignments() - completed;
                    long estimatedSecondsLeft = (long) (remaining / rate);
                    estimatedCompletion = Instant.now().plusSeconds(estimatedSecondsLeft).toString();
                }
            }

            // Build response
            return new BatchJobResponse(
                    batchJob.getId(),
                    batchJob.getStatus(),
                    batchJob.getTotalAssignments(),
                    batchJob.getCompletedCount(),
                    batchJob.getFailedCount(),
                    batchJob.getCreatedAt(),
                    batchJob.getStartedAt(),
                    batchJob.getCompletedAt(),
                    estimatedCompletion,
                    "/api/user-roles/batch-status/" + batchJob.getId()
            );

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to fetch batch job status")
                    .detail("batchId", batchId.toString())
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }
}