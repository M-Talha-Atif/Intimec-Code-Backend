package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.event.RoleEventPublisher;
import com.aidentx.applicationpermissions.roles_management.exception.DuplicatePermissionException;
import com.aidentx.applicationpermissions.roles_management.exception.PermissionNotFoundException;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignPermissionToRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentItem;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignPermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevokePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevocationResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleMembersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleOwnersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetUsersByRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.OwnedRoleDTO;
import com.aidentx.applicationpermissions.roles_management.model.dto.OwnedRolesResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.PermissionDetailsResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleMemberDto;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleStatsDTO;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdatePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UserWithOwnershipDto;
import com.aidentx.applicationpermissions.roles_management.model.dto.UserWithRoleDto;
import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.ConditionalRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.model.entity.RoleOwners;
import com.aidentx.applicationpermissions.roles_management.model.entity.RolePermission;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.ActionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.ConditionalRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PageRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PermissionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleOwnersRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RolePermissionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.UserRoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.RoleService;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.shared.grpc.dto.UsersResponse;
import com.aidentx.applicationpermissions.shared.security.JwtUtil;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.auth.grpc.GetUsersByTenantResponse;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * Implementation of RoleService for managing roles with tenant-aware access control.
 * 
 * <p>This service ensures that all role operations are scoped to the current user's tenant
 * as extracted from the JWT token. It provides complete tenant isolation for role management.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    // ThreadLocal to store filter stats for the current request
    private static final ThreadLocal<Map<String, Long>> filterStats = new ThreadLocal<>();
    
    public static Map<String, Long> getFilterStats() {
        return filterStats.get();
    }
    
    public static void clearFilterStats() {
        filterStats.remove();
    }

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleOwnersRepository roleOwnersRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ConditionalRuleRepository conditionalRuleRepository;
    private final ActionRepository actionRepository;
    private final PageRepository pageRepository;
    private final DynamicRuleRepository dynamicRuleRepository;
    private final TenantContextService tenantContextService;
    private final JwtUtil jwtUtil;
    private final UserGrpcClient userGrpcClient;
    private final RoleEventPublisher roleEventPublisher;
    private final IUnifiedLogger logger;
    private final org.springframework.web.client.RestTemplate restTemplate;
    
    @Value("${workflows-service.url:http://localhost:8098}")
    private String workflowsServiceUrl;

    /**
     * Constructor for RoleServiceImpl.
     * 
     * @param roleRepository repository for role entities
     * @param userRoleRepository repository for user role assignments
     * @param roleOwnersRepository repository for role ownership relationships
     * @param permissionRepository repository for permission entities
     * @param rolePermissionRepository repository for role-permission associations
     * @param conditionalRuleRepository repository for conditional rule entities
     * @param dynamicRuleRepository repository for dynamic rule entities
     * @param tenantContextService service for extracting tenant context from JWT
     * @param userGrpcClient client for gRPC calls to auth-service
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository,
                          UserRoleRepository userRoleRepository,
                          RoleOwnersRepository roleOwnersRepository,
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository,
                          ConditionalRuleRepository conditionalRuleRepository,
                          ActionRepository actionRepository,
                          PageRepository pageRepository,
                          DynamicRuleRepository dynamicRuleRepository,
                          TenantContextService tenantContextService,
                          JwtUtil jwtUtil,
                          UserGrpcClient userGrpcClient,
                          @Autowired(required = false) RoleEventPublisher roleEventPublisher,
                          UnifiedLoggerFactory loggerFactory,
                          org.springframework.web.client.RestTemplate restTemplate) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleOwnersRepository = roleOwnersRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.conditionalRuleRepository = conditionalRuleRepository;
        this.actionRepository = actionRepository;
        this.pageRepository = pageRepository;
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.tenantContextService = tenantContextService;
        this.jwtUtil = jwtUtil;
        this.userGrpcClient = userGrpcClient;
        this.roleEventPublisher = roleEventPublisher;
        this.logger = loggerFactory.getLogger(AppConstants.ROLE_SERVICE_LOGGER_NAME, AppConstants.LOG_CATEGORY_ROLE_MANAGEMENT);
        this.restTemplate = restTemplate;
    }
    
    /**
     * Validates that a workflow has the correct configuration for role creation approval.
     * Checks that the workflow contains an approval.multi node with approverType: "role_owner".
     * 
     * @param workflowId the workflow ID to validate
     * @param ownerCount the number of role owners
     * @throws IllegalArgumentException if the workflow is incompatible
     */
    private void validateWorkflowForRoleCreation(String workflowId, int ownerCount) {
        try {
            // Fetch workflow from workflows-service
            String workflowUrl = workflowsServiceUrl + "/api/v1/workflows/" + workflowId;
            
            logger.info()
                .message("Validating workflow for role creation")
                .detail("workflowId", workflowId)
                .detail("ownerCount", String.valueOf(ownerCount))
                .objectType("WORKFLOW_VALIDATION")
                .log();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(workflowUrl, Map.class);
            
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new IllegalArgumentException("Workflow not found: " + workflowId);
            }
            
            // Extract workflow data from response
            @SuppressWarnings("unchecked")
            Map<String, Object> workflow = (Map<String, Object>) response.get("data");
            
            if (workflow == null) {
                throw new IllegalArgumentException("Workflow data is null: " + workflowId);
            }
            
            // Check workflow nodes for approval nodes (either single or multi)
            // For role creation workflows:
            // - approval.single: works when role has exactly 1 owner
            // - approval.multi with approverType=role_owner: works with any number of owners (1 or more)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflow.get("nodes");
            
            if (nodes == null || nodes.isEmpty()) {
                throw new IllegalArgumentException(
                    "Workflow does not contain any nodes. Please select a workflow with approval nodes.");
            }
            
            boolean hasApprovalNode = false;
            String approvalNodeType = null;
            boolean isApprovalSingle = false;
            boolean isApprovalMulti = false;
            
            for (Map<String, Object> node : nodes) {
                String nodeType = (String) node.get("type");
                
                // Check for approval.single node
                if ("approval.single".equals(nodeType)) {
                    hasApprovalNode = true;
                    approvalNodeType = "approval.single";
                    isApprovalSingle = true;
                    break;
                }
                
                // Check for approval.multi with role_owner approverType
                if ("approval.multi".equals(nodeType)) {
                    // Check both node.data and node.config for approverType field
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nodeData = (Map<String, Object>) node.get("data");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nodeConfig = (Map<String, Object>) node.get("config");
                    
                    String approverType = null;
                    if (nodeData != null) {
                        approverType = (String) nodeData.get("approverType");
                    }
                    if (approverType == null && nodeConfig != null) {
                        approverType = (String) nodeConfig.get("approverType");
                    }
                    
                    if ("role_owner".equals(approverType)) {
                        hasApprovalNode = true;
                        approvalNodeType = "approval.multi (role_owner)";
                        isApprovalMulti = true;
                        break;
                    }
                }
            }
            
            if (!hasApprovalNode) {
                logger.warn()
                    .message("Workflow validation failed: Missing approval node")
                    .detail("workflowId", workflowId)
                    .detail("workflowName", (String) workflow.get("name"))
                    .objectType("WORKFLOW_VALIDATION")
                    .log();
                
                throw new IllegalArgumentException(
                    "The selected workflow is not compatible with role creation approval. " +
                    "It must contain either an 'approval.single' node or an 'approval.multi' node with approverType set to 'role_owner'. " +
                    "Please select a different workflow or create a new one with the correct configuration.");
            }
            
            // Validate approval node matches owner count
            // Note: approval.single requires exactly 1 owner
            // approval.multi works with any number of owners (including 1)
            if (isApprovalSingle && ownerCount > 1) {
                logger.warn()
                    .message("Workflow validation failed: approval.single node cannot handle multiple owners")
                    .detail("workflowId", workflowId)
                    .detail("workflowName", (String) workflow.get("name"))
                    .detail("ownerCount", String.valueOf(ownerCount))
                    .detail("approvalNodeType", approvalNodeType)
                    .objectType("WORKFLOW_VALIDATION")
                    .log();
                
                throw new IllegalArgumentException(
                    "The selected workflow uses 'approval.single' which only supports roles with 1 owner. " +
                    "You have selected " + ownerCount + " owners. " +
                    "Please either: (1) Select a workflow with 'approval.multi' node, or (2) Select only 1 owner for this role.");
            }
            
            logger.info()
                .message("Workflow validation successful: Found approval node")
                .detail("approvalNodeType", approvalNodeType)
                .objectType("WORKFLOW_VALIDATION")
                .log();
            
            logger.info()
                .message("Workflow validation successful")
                .detail("workflowId", workflowId)
                .detail("workflowName", (String) workflow.get("name"))
                .objectType("WORKFLOW_VALIDATION")
                .log();
                
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            logger.error()
                .message("Failed to validate workflow")
                .detail("workflowId", workflowId)
                .detail("error", e.getMessage())
                .objectType("WORKFLOW_VALIDATION")
                .log();
            
            throw new IllegalArgumentException(
                "Unable to validate workflow: " + e.getMessage() + 
                ". Please ensure the workflow exists and is accessible.", e);
        }
    }

    /**
     * Creates a new role for the current tenant.
     * 
     * @param request the role creation request
     * @return the created role response
     * @throws RuntimeException if role creation fails
     */
    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        try {
            // Extract tenant ID and user info from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            String createdByName = tenantContextService.getCurrentUsername();
            String createdByIdStr = tenantContextService.getCurrentUserId();
            UUID createdById = UUID.fromString(createdByIdStr);
            
            logger.info()
                .message(AppConstants.LOG_ROLE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, request.getRoleType().name())
                .detail(AppConstants.DETAIL_KEY_CREATED_BY, createdByName)
                .detail("submitForApproval", request.getSubmitForApproval())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Check if this is a submit-for-approval operation (no immediate role creation)
            if (request.getSubmitForApproval() != null && request.getSubmitForApproval()) {
                return handleSubmitForApproval(request, tenantId, createdByName, createdById);
            }

            // Block creation of SYSTEM roles
            if (AppConstants.ROLE_TYPE_SYSTEM.equals(request.getRoleType().name())) {
                logger.warn()
                    .message(AppConstants.LOG_SYSTEM_ROLE_CREATION_ATTEMPTED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                    .detail(AppConstants.DETAIL_KEY_CREATED_BY, createdByName)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.SYSTEM_ROLE_CREATION_FORBIDDEN);
            }

            // Validate role name length
            if (request.getRoleName() != null && request.getRoleName().length() > 128) {
                logger.warn()
                    .message("Role name exceeds maximum length")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                    .detail("nameLength", request.getRoleName().length())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                throw new IllegalArgumentException("Role name must not exceed 128 characters");
            }

            // Validate role name uniqueness
            if (roleRepository.existsByRoleNameAndTenantId(request.getRoleName(), tenantId)) {
                logger.warn()
                    .message(AppConstants.ROLE_NAME_ALREADY_EXISTS)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.ROLE_NAME_ALREADY_EXISTS);
            }

            boolean hasOwners = request.getOwnerIds() != null && !request.getOwnerIds().isEmpty();

            // Create role entity (always active for direct creation)
            Role role = new Role(
                tenantId, 
                request.getRoleType(), 
                request.getRoleName(), 
                request.getDescription(),
                request.getBusinessJustification(), 
                request.getRequireOwnerApproval(), 
                request.getTags(), 
                request.getRiskLevel(), 
                true, // Always active for direct creation
                createdByName, 
                createdById
            );
            
            // Save role to database
            Role savedRole = roleRepository.save(role);
            
            // Create role ownership relationships if owner IDs are provided
            if (hasOwners) {
                validateAndCreateRoleOwners(savedRole.getId(), request.getOwnerIds(), tenantId);
            }
            
            logger.info()
                .message(AppConstants.LOG_ROLE_CREATED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, savedRole.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, savedRole.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, savedRole.getRoleType().name())
                .detail(AppConstants.DETAIL_KEY_RISK_LEVEL, savedRole.getRiskLevel().name())
                .detail(AppConstants.DETAIL_KEY_CREATED_BY, savedRole.getCreatedByName())
                .detail(AppConstants.DETAIL_KEY_OWNER_COUNT, request.getOwnerIds() != null ? request.getOwnerIds().size() : 0)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return convertToResponse(savedRole);
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation exceptions directly (like duplicate role name)
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, request.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_CREATION_FAILED, e);
        }
    }

    /**
     * Updates an existing role for the current tenant.
     * 
     * @param id the role ID to update
     * @param request the role update request
     * @return the updated role response
     * @throws RuntimeException if role is not found or belongs to different tenant
     */
    @Override
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_UPDATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, request.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Validate role name length
            if (request.getRoleName() != null && request.getRoleName().length() > 128) {
                logger.warn()
                    .message("Role name exceeds maximum length")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                    .detail("nameLength", request.getRoleName().length())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                throw new IllegalArgumentException("Role name must not exceed 128 characters");
            }

            // Find and validate role
            Role role = findAndValidateRole(id, tenantId);
            
            // Block roleType changes for SYSTEM roles
            if (AppConstants.ROLE_TYPE_SYSTEM.equals(role.getRoleType().name()) && 
                !role.getRoleType().equals(request.getRoleType())) {
                logger.warn()
                    .message(AppConstants.LOG_SYSTEM_ROLE_UPDATE_ATTEMPTED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .detail("attemptedRoleType", request.getRoleType().name())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                throw new IllegalArgumentException(AppConstants.SYSTEM_ROLE_UPDATE_FORBIDDEN);
            }
            
            // Block changing any role TO SYSTEM type
            if (AppConstants.ROLE_TYPE_SYSTEM.equals(request.getRoleType().name()) && 
                !role.getRoleType().name().equals(AppConstants.ROLE_TYPE_SYSTEM)) {
                logger.warn()
                    .message(AppConstants.LOG_SYSTEM_ROLE_CREATION_ATTEMPTED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                throw new IllegalArgumentException(AppConstants.SYSTEM_ROLE_CREATION_FORBIDDEN);
            }
            
            // Validate role name uniqueness if name is being changed
            if (!role.getRoleName().equals(request.getRoleName())) {
                if (roleRepository.existsByRoleNameAndTenantId(request.getRoleName(), tenantId)) {
                    logger.warn()
                        .message(AppConstants.ROLE_NAME_ALREADY_EXISTS)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                        .log();
                    throw new IllegalArgumentException(AppConstants.ROLE_NAME_ALREADY_EXISTS);
                }
            }
            
            // Extract user information from JWT token for audit purposes
            String updatedByName = tenantContextService.getCurrentUsername();
            String updatedByIdStr = tenantContextService.getCurrentUserId();
            UUID updatedById = UUID.fromString(updatedByIdStr);
            
            // Update role fields
            role.setRoleType(request.getRoleType());
            role.setRoleName(request.getRoleName());
            role.setDescription(request.getDescription());
            role.setBusinessJustification(request.getBusinessJustification());
            role.setRequireOwnerApproval(request.getRequireOwnerApproval());
            role.setTags(request.getTags());
            role.setRiskLevel(request.getRiskLevel());
            role.setIsActive(request.getIsActive());
            
            // Update audit fields
            role.setUpdatedAt(System.currentTimeMillis());
            role.setUpdatedByName(updatedByName);
            role.setUpdatedById(updatedById);
            
            logger.debug()
                .message(AppConstants.LOG_ROLE_UPDATE_AUDIT_INFO_SET)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .detail(AppConstants.DETAIL_KEY_UPDATED_BY_NAME, updatedByName)
                .detail(AppConstants.DETAIL_KEY_UPDATED_BY_ID, updatedById.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Save updated role
            Role updatedRole = roleRepository.save(role);
            
            // Sync dynamic rule's ruleIsActive if one exists
            List<DynamicRule> dynamicRules = dynamicRuleRepository.findByRoleId(id);
            if (!dynamicRules.isEmpty()) {
                for (DynamicRule dynamicRule : dynamicRules) {
                    if (!updatedRole.getIsActive().equals(dynamicRule.getRuleIsActive())) {
                        dynamicRule.setRuleIsActive(updatedRole.getIsActive());
                        dynamicRuleRepository.save(dynamicRule);
                        logger.info()
                            .message("Synced dynamic rule ruleIsActive with role isActive status")
                            .detail("roleId", updatedRole.getId().toString())
                            .detail("roleName", updatedRole.getRoleName())
                            .detail("dynamicRuleId", dynamicRule.getId().toString())
                            .detail("newRuleIsActive", updatedRole.getIsActive().toString())
                            .log();
                    }
                }
            }
            
            // Handle owner updates if provided
            if (request.getOwnerIds() != null) {
                logger.debug()
                    .message(AppConstants.LOG_UPDATING_ROLE_OWNERS)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_NEW_OWNER_IDS_COUNT, request.getOwnerIds().size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                // Delete all existing owners first
                List<RoleOwners> existingOwners = roleOwnersRepository.findByRoleId(id);
                if (!existingOwners.isEmpty()) {
                    logger.debug()
                        .message(AppConstants.LOG_DELETING_EXISTING_ROLE_OWNERS)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                        .detail(AppConstants.DETAIL_KEY_EXISTING_OWNERS_COUNT, existingOwners.size())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                        .log();
                    
                    roleOwnersRepository.deleteAll(existingOwners);
                }
                
                // Create new owner relationships if owner IDs are provided
                if (!request.getOwnerIds().isEmpty()) {
                    validateAndCreateRoleOwners(id, request.getOwnerIds(), tenantId);
                }
            }
            
            logger.info()
                .message(AppConstants.LOG_ROLE_UPDATED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, updatedRole.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, updatedRole.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, updatedRole.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return convertToResponse(updatedRole);
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation exceptions directly (like duplicate role name)
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_UPDATE_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_UPDATE_FAILED, e);
        }
    }

    /**
     * Retrieves a role by ID for the current tenant.
     * 
     * @param id the role ID to retrieve
     * @return the role response
     * @throws RuntimeException if role is not found or belongs to different tenant
     */
    @Override
    public RoleResponse getRoleById(UUID id) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Find and validate role
            Role role = findAndValidateRole(id, tenantId);
            
            logger.info()
                .message(AppConstants.LOG_ROLE_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, role.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return convertToResponse(role);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_RETRIEVAL_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_RETRIEVAL_FAILED, e);
        }
    }

    /**
     * Retrieves all roles for the current tenant.
     * 
     * @return list of role responses
     * @throws RuntimeException if role retrieval fails
     */
    @Override
    public List<RoleResponse> getAllRoles() {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLES_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Find all roles for the tenant
            List<Role> roles = roleRepository.findByTenantId(tenantId);
            
            logger.info()
                .message(AppConstants.LOG_ROLES_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_COUNT, roles.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            // Sort roles: SYSTEM roles first, then others
            return roles.stream()
                .sorted((r1, r2) -> {
                    boolean isR1System = AppConstants.ROLE_TYPE_SYSTEM.equals(r1.getRoleType().name());
                    boolean isR2System = AppConstants.ROLE_TYPE_SYSTEM.equals(r2.getRoleType().name());
                    if (isR1System && !isR2System) return -1;
                    if (!isR1System && isR2System) return 1;
                    return 0;
                })
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_RETRIEVAL_FAILED)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_RETRIEVAL_FAILED, e);
        }
    }

    /**
     * Retrieves all roles for the current tenant with pagination.
     * 
     * @param page the page number (0-based)
     * @param size the page size
     * @return page of role responses
     * @throws RuntimeException if role retrieval fails
     */
    @Override
    public Page<RoleResponse> getAllRolesPaginated(int page, int size) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLES_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("page", page)
                .detail("size", size)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "updatedAt"));
            
            Page<Role> rolesPage = roleRepository.findByTenantId(tenantId, pageable);
            
            logger.info()
                .message(AppConstants.LOG_ROLES_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_COUNT, rolesPage.getContent().size())
                .detail("totalElements", rolesPage.getTotalElements())
                .detail("totalPages", rolesPage.getTotalPages())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            List<RoleResponse> roleResponses = rolesPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return new PageImpl<>(roleResponses, pageable, rolesPage.getTotalElements());
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_RETRIEVAL_FAILED)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_RETRIEVAL_FAILED, e);
        }
    }

    /**
     * Retrieves all roles for the current tenant with pagination and filters.
     * 
     * @param page the page number (0-based)
     * @param size the page size
     * @param search search term for role name
     * @param roleType filter by role type
     * @param isActive filter by active status
     * @param riskLevel filter by risk level
     * @param tags filter by tags
     * @return page of role responses
     * @throws RuntimeException if role retrieval fails
     */
    @Override
    public Page<RoleResponse> getAllRolesPaginated(int page, int size, String search, String roleType, 
                                                   Boolean isActive, String riskLevel, List<String> tags) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLES_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("page", page)
                .detail("size", size)
                .detail("search", search)
                .detail("roleType", roleType)
                .detail("isActive", isActive)
                .detail("riskLevel", riskLevel)
                .detail("tags", tags)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "updatedAt"));
            
            // Get ALL roles for the tenant (not paginated yet)
            List<Role> allRoles = roleRepository.findByTenantId(tenantId);
            
            // Apply filters to ALL roles
            List<Role> filteredRoles = allRoles.stream()
                .filter(role -> {
                    // Filter by search (role name)
                    if (search != null && !search.trim().isEmpty()) {
                        if (!role.getRoleName().toLowerCase().contains(search.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by role type (supports multiple types separated by comma)
                    if (roleType != null && !roleType.trim().isEmpty()) {
                        String[] roleTypes = roleType.split(",");
                        boolean matchesRoleType = false;
                        for (String type : roleTypes) {
                            if (role.getRoleType().name().equals(type.trim())) {
                                matchesRoleType = true;
                                break;
                            }
                        }
                        if (!matchesRoleType) {
                            return false;
                        }
                    }
                    
                    // Filter by isActive status - if null, show all
                    if (isActive != null) {
                        Boolean roleIsActive = role.getIsActive();
                        if (roleIsActive == null) roleIsActive = true;
                        if (!roleIsActive.equals(isActive)) {
                            return false;
                        }
                    }
                    
                    // Filter by risk level (supports multiple levels separated by comma)
                    if (riskLevel != null && !riskLevel.trim().isEmpty()) {
                        if (role.getRiskLevel() == null) {
                            return false;
                        }
                        String[] riskLevels = riskLevel.split(",");
                        boolean matchesRiskLevel = false;
                        for (String level : riskLevels) {
                            if (role.getRiskLevel().name().equals(level.trim())) {
                                matchesRiskLevel = true;
                                break;
                            }
                        }
                        if (!matchesRiskLevel) {
                            return false;
                        }
                    }
                    
                    // Filter by tags (role must have at least one of the requested tags)
                    if (tags != null && !tags.isEmpty()) {
                        List<String> roleTags = role.getTags();
                        if (roleTags == null || roleTags.isEmpty()) {
                            return false;
                        }
                        // Check if role has at least one of the requested tags
                        boolean hasTag = roleTags.stream()
                            .anyMatch(tags::contains);
                        if (!hasTag) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                // Sort: SYSTEM roles first, then by updatedAt descending
                .sorted((r1, r2) -> {
                    boolean isR1System = AppConstants.ROLE_TYPE_SYSTEM.equals(r1.getRoleType().name());
                    boolean isR2System = AppConstants.ROLE_TYPE_SYSTEM.equals(r2.getRoleType().name());
                    if (isR1System && !isR2System) return -1;
                    if (!isR1System && isR2System) return 1;
                    // If both are SYSTEM or both are not SYSTEM, sort by updatedAt
                    return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
                })
                .collect(Collectors.toList());
            
            // Calculate stats from ALL filtered results
            long totalFiltered = filteredRoles.size();
            long inactiveCount = filteredRoles.stream()
                .filter(role -> role.getIsActive() != null && !role.getIsActive())
                .count();
            long activeCount = filteredRoles.stream()
                .filter(role -> role.getIsActive() != null && role.getIsActive())
                .count();
            long highRiskCount = filteredRoles.stream()
                .filter(role -> role.getRiskLevel() != null && role.getRiskLevel().name().equals("High"))
                .count();
            
            // Store stats in ThreadLocal for controller to access
            Map<String, Long> stats = new HashMap<>();
            stats.put(AppConstants.STATS_FIELD_INACTIVE, inactiveCount);
            stats.put(AppConstants.STATS_FIELD_ACTIVE, activeCount);
            stats.put(AppConstants.STATS_FIELD_HIGH_RISK, highRiskCount);
            filterStats.set(stats);
            
            // Now paginate the filtered results
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredRoles.size());
            List<Role> paginatedRoles = filteredRoles.subList(start, end);
            
            logger.info()
                .message(AppConstants.LOG_ROLES_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("totalFiltered", totalFiltered)
                .detail("activeCount", activeCount)
                .detail("highRiskCount", highRiskCount)
                .detail("pageSize", paginatedRoles.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            List<RoleResponse> roleResponses = paginatedRoles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            // Create custom PageImpl with stats metadata
            PageImpl<RoleResponse> pageResult = new PageImpl<>(roleResponses, pageable, totalFiltered);
            
            // Store stats in a way that can be retrieved by controller
            // We'll add them as custom attributes in the response
            return pageResult;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_RETRIEVAL_FAILED)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_RETRIEVAL_FAILED, e);
        }
    }

    /**
     * Retrieves all unique tags from roles in the current tenant.
     * 
     * @return list of unique tags
     * @throws RuntimeException if tag retrieval fails
     */
    @Override
    public List<String> getAllUniqueTags() {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_TAGS_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            List<Role> roles = roleRepository.findByTenantId(tenantId);
            
            // Collect all unique tags
            List<String> uniqueTags = roles.stream()
                .filter(role -> role.getTags() != null)
                .flatMap(role -> role.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            logger.info()
                .message(AppConstants.LOG_ROLE_TAGS_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("tagCount", uniqueTags.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return uniqueTags;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ROLE_TAGS_RETRIEVAL_FAILED)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_TAGS_RETRIEVAL_FAILED, e);
        }
    }

    /**
     * Deletes a role by ID or submits it for approval if required.
     * 
     * @param id the role ID to delete
     * @param executionId the workflow execution ID if called from within a workflow (null for user-initiated)
     * @return true if role was deleted immediately, false if submitted for approval
     * @throws RuntimeException if role is not found or belongs to different tenant
     */
    @Override
    public boolean deleteRole(UUID id, UUID executionId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_DELETE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Find and validate role
            Role role = findAndValidateRole(id, tenantId);
            
            // Block deletion of SYSTEM roles
            if (AppConstants.ROLE_TYPE_SYSTEM.equals(role.getRoleType().name())) {
                logger.warn()
                    .message(AppConstants.LOG_SYSTEM_ROLE_DELETION_ATTEMPTED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                throw new IllegalArgumentException(AppConstants.SYSTEM_ROLE_DELETION_FORBIDDEN);
            }
            
            // Extract role owners BEFORE deletion (needed for workflow approval)
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(id);
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .collect(Collectors.toList());
            
            logger.info()
                .message("Role deletion check")
                .detail("requireOwnerApproval", role.getRequireOwnerApproval())
                .detail("ownerIdsCount", ownerIds.size())
                .detail("roleEventPublisherNotNull", roleEventPublisher != null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Extract current user info for event
            String deletedBy = tenantContextService.getCurrentUsername();
            String deletedByIdStr = tenantContextService.getCurrentUserId();
            UUID deletedById = UUID.fromString(deletedByIdStr);
            
            // Publish event if role requires approval AND this is NOT a workflow-originated deletion
            // When executionId is present, it means the deletion is coming from a workflow execution
            // and we should proceed with actual deletion instead of publishing another event
            if (role.getRequireOwnerApproval() && roleEventPublisher != null && !ownerIds.isEmpty() && executionId == null) {
                logger.info()
                    .message("Role deletion requires approval - publishing event to trigger workflow")
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("ownerCount", ownerIds.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                // Publish event (workflow will handle actual deletion after approval)
                roleEventPublisher.publishRoleDeletedEvent(
                    role,
                    ownerIds,
                    tenantId,
                    deletedBy,
                    deletedById,
                    null  // workflowId is null for initial user-triggered deletion
                );
                
                // Do NOT delete the role yet - workflow will call DELETE API after approval
                logger.info()
                    .message("Role deletion event published - awaiting workflow approval")
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                return false;  // Return false to indicate submitted for approval
            }
            
            // If we reach here, either:
            // 1. Role doesn't require approval, OR
            // 2. executionId is present (workflow-originated deletion)
            // In both cases, proceed with actual deletion
            if (executionId != null) {
                logger.info()
                    .message("Workflow-originated deletion - proceeding with actual role deletion")
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail("executionId", executionId.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
            }
            
            // CASCADE DELETE: Delete all related entities
            
            // 1. Delete role owners (reuse already fetched list)
            if (!roleOwners.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_DELETING_ROLE_OWNERS)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_COUNT, roleOwners.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                roleOwnersRepository.deleteAll(roleOwners);
            }
            
            // 2. Delete user-role assignments
            List<UserRole> userRoles = 
                userRoleRepository.findByRoleId(id);
            if (!userRoles.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_DELETING_USER_ROLE_ASSIGNMENTS)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_COUNT, userRoles.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                userRoleRepository.deleteAll(userRoles);
            }
            
            // 3. Delete dynamic rules
            List<DynamicRule> dynamicRules = 
                dynamicRuleRepository.findByRoleId(id);
            if (!dynamicRules.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_DELETING_DYNAMIC_RULES)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_COUNT, dynamicRules.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                dynamicRuleRepository.deleteAll(dynamicRules);
            }
            
            // 4. Delete role-permission associations and their related permissions/conditional rules
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(id);
            if (!rolePermissions.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_DELETING_ROLE_PERMISSION_ASSOCIATIONS)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_COUNT, rolePermissions.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                // Collect permission IDs that need to be deleted
                List<UUID> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId)
                    .collect(Collectors.toList());
                
                // Delete role-permission associations first
                rolePermissionRepository.deleteAll(rolePermissions);
                
                // For each permission, check if it's used by other roles before deleting
                for (UUID permissionId : permissionIds) {
                    // Check if this permission is still used by other roles
                    Long roleCount = permissionRepository.countRolesByPermissionId(permissionId);
                    
                    if (roleCount == 0) {
                        // No other role uses this permission, safe to delete
                        logger.debug()
                            .message(AppConstants.LOG_DELETING_ORPHANED_PERMISSION)
                            .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permissionId.toString())
                            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                            .log();
                        
                        // 4a. Delete conditional rules for this permission
                        List<ConditionalRule> conditionalRules = 
                            conditionalRuleRepository.findByPermissionId(permissionId);
                        if (!conditionalRules.isEmpty()) {
                            conditionalRuleRepository.deleteAll(conditionalRules);
                        }
                        
                        // 4b. Delete the permission itself
                        permissionRepository.deleteById(permissionId);
                    } else {
                        logger.debug()
                            .message(AppConstants.LOG_PERMISSION_STILL_IN_USE)
                            .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permissionId.toString())
                            .detail(AppConstants.DETAIL_KEY_ROLE_COUNT, roleCount)
                            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                            .log();
                    }
                }
            }
            
            // 5. Finally, delete the role itself
            roleRepository.delete(role);
            
            logger.info()
                .message(AppConstants.LOG_ROLE_DELETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, role.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, role.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            return true;  // Return true to indicate role was deleted immediately
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ROLE_DELETION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_DELETION_FAILED, e);
        }
    }

    /**
     * Deletes multiple roles in bulk for the specified tenant.
     * 
     * @param roleIds list of role IDs to delete
     * @param tenantId the tenant ID
     * @return map containing deletion results
     */
    @Override
    public Map<String, Object> bulkDeleteRoles(List<UUID> roleIds, String tenantId) {
        logger.info()
            .message(AppConstants.LOG_BULK_ROLE_DELETION_STARTED)
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail("roleIdsCount", String.valueOf(roleIds.size()))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        int successCount = 0;
        int failedCount = 0;
        List<Map<String, String>> successfulDeletions = new ArrayList<>();
        List<Map<String, String>> failedDeletions = new ArrayList<>();
        
        for (UUID roleId : roleIds) {
            try {
                logger.debug()
                    .message(AppConstants.LOG_PROCESSING_ROLE_DELETION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                // Find and validate role
                Role role = findAndValidateRole(roleId, tenantId);
                String roleName = role.getRoleName();
                
                // Block deletion of SYSTEM roles in bulk operations
                if (AppConstants.ROLE_TYPE_SYSTEM.equals(role.getRoleType().name())) {
                    logger.warn()
                        .message(AppConstants.LOG_SYSTEM_ROLE_DELETION_ATTEMPTED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                        .log();
                    throw new IllegalArgumentException(AppConstants.SYSTEM_ROLE_DELETION_FORBIDDEN);
                }
                
                // Delete the role (this will handle all cascade deletions)
                deleteRole(roleId, null);
                
                successCount++;
                Map<String, String> successInfo = new HashMap<>();
                successInfo.put("roleId", roleId.toString());
                successInfo.put("roleName", roleName);
                successfulDeletions.add(successInfo);
                
                logger.info()
                    .message(AppConstants.LOG_ROLE_DELETED_SUCCESSFULLY)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, roleName)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                    
            } catch (Exception e) {
                failedCount++;
                Map<String, String> failureInfo = new HashMap<>();
                failureInfo.put("roleId", roleId.toString());
                failureInfo.put("error", e.getMessage());
                failedDeletions.add(failureInfo);
                
                logger.error(e)
                    .message(AppConstants.LOG_ROLE_DELETION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail("error", e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("successfulDeletions", successfulDeletions);
        result.put("failedDeletions", failedDeletions);
        
        logger.info()
            .message(AppConstants.LOG_BULK_ROLE_DELETION_SUCCESSFUL)
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail("totalRoles", String.valueOf(roleIds.size()))
            .detail("successCount", String.valueOf(successCount))
            .detail("failedCount", String.valueOf(failedCount))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();
        
        return result;
    }

    /**
     * Creates a copy of an existing role with all its properties and owners.
     * 
     * <p>This method duplicates a role including its metadata, tags, and owner relationships.
     * The new role name will have " (Copy)" appended to distinguish it from the original.
     * Permissions are NOT copied - only the role definition and ownership.</p>
     * 
     * @param roleId the ID of the role to copy
     * @return the newly created role response
     * @throws RuntimeException if role is not found or belongs to different tenant
     */
    @Override
    public RoleResponse copyRole(UUID roleId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_COPY_REQUESTED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            // Find and validate source role
            Role sourceRole = findAndValidateRole(roleId, tenantId);
            
            // Get current user info for audit fields
            String createdByName = tenantContextService.getCurrentUsername();
            String createdByIdStr = tenantContextService.getCurrentUserId();
            UUID createdById = UUID.fromString(createdByIdStr);
            
            // Create a new copy of the tags list to avoid shared reference issues
            List<String> copiedTags = sourceRole.getTags() != null 
                ? new ArrayList<>(sourceRole.getTags()) 
                : null;
            
            // Determine role type for the copied role
            // If source role is SYSTEM type, change to VIEWER for the copy
            // Otherwise, keep the same role type
            RoleType copiedRoleType = sourceRole.getRoleType() == RoleType.SYSTEM 
                ? RoleType.VIEWER 
                : sourceRole.getRoleType();
            
            logger.debug()
                .message(AppConstants.LOG_ROLE_TYPE_ADJUSTED_FOR_COPY)
                .detail(AppConstants.DETAIL_KEY_SOURCE_ROLE_TYPE, sourceRole.getRoleType().toString())
                .detail(AppConstants.DETAIL_KEY_COPIED_ROLE_TYPE, copiedRoleType.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Generate a unique copy name
            String baseCopyName = sourceRole.getRoleName() + " (Copy)";
            String newRoleName = baseCopyName;
            int copyNumber = 2;
            
            // Check if the name exists and increment until we find a unique name
            while (roleRepository.existsByRoleNameAndTenantId(newRoleName, tenantId)) {
                newRoleName = sourceRole.getRoleName() + " (Copy " + copyNumber + ")";
                copyNumber++;
            }
            
            // Create new role with copied data using constructor (sets timestamps)
            Role newRole = new Role(
                sourceRole.getTenantId(),
                copiedRoleType,
                newRoleName,
                sourceRole.getDescription(),
                sourceRole.getBusinessJustification(),
                sourceRole.getRequireOwnerApproval(),
                copiedTags,
                sourceRole.getRiskLevel(),
                sourceRole.getIsActive(),
                createdByName,
                createdById
            );
            
            // Save the new role
            Role savedRole = roleRepository.save(newRole);
            
            logger.debug()
                .message(AppConstants.LOG_NEW_ROLE_CREATED_FROM_COPY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_NEW_ROLE_ID, savedRole.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, savedRole.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Copy role owners
            List<RoleOwners> sourceOwners = roleOwnersRepository.findByRoleId(roleId);
            if (!sourceOwners.isEmpty()) {
                List<RoleOwners> newOwners = sourceOwners.stream()
                    .map(sourceOwner -> {
                        RoleOwners newOwner = new RoleOwners();
                        newOwner.setId(UUID.randomUUID()); // Generate new ID
                        newOwner.setRoleId(savedRole.getId()); // Point to new role
                        newOwner.setUserId(sourceOwner.getUserId()); // Keep same user
                        return newOwner;
                    })
                    .collect(Collectors.toList());
                
                roleOwnersRepository.saveAll(newOwners);
                
                logger.debug()
                    .message(AppConstants.LOG_ROLE_OWNERS_COPIED)
                    .detail(AppConstants.DETAIL_KEY_NEW_ROLE_ID, savedRole.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_COUNT, newOwners.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
            }
            
            // Copy permissions and conditional rules
            List<RolePermission> sourceRolePermissions = rolePermissionRepository.findByRoleId(roleId);
            if (!sourceRolePermissions.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_STARTING_PERMISSION_COPY)
                    .detail(AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_NEW_ROLE_ID, savedRole.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_COUNT, sourceRolePermissions.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                for (RolePermission sourceRolePermission : sourceRolePermissions) {
                    // Get the source permission
                    Permission sourcePermission = permissionRepository.findById(sourceRolePermission.getPermissionId())
                        .orElse(null);
                    
                    if (sourcePermission != null) {
                        // Create new permission with same actionId
                        Permission newPermission = new Permission(sourcePermission.getActionId());
                        Permission savedPermission = permissionRepository.save(newPermission);
                        
                        logger.trace()
                            .message(AppConstants.LOG_PERMISSION_COPIED)
                            .detail(AppConstants.DETAIL_KEY_SOURCE_PERMISSION_ID, sourcePermission.getId().toString())
                            .detail(AppConstants.DETAIL_KEY_NEW_PERMISSION_ID, savedPermission.getId().toString())
                            .detail(AppConstants.DETAIL_KEY_ACTION_ID, sourcePermission.getActionId().toString())
                            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                            .log();
                        
                        // Create new role-permission association
                        RolePermission newRolePermission = new RolePermission(savedRole.getId(), savedPermission.getId());
                        rolePermissionRepository.save(newRolePermission);
                        
                        // Copy conditional rules if they exist
                        List<ConditionalRule> sourceConditionalRules = conditionalRuleRepository.findByPermissionId(sourcePermission.getId());
                        if (!sourceConditionalRules.isEmpty()) {
                            List<ConditionalRule> newConditionalRules = sourceConditionalRules.stream()
                                .map(sourceRule -> new ConditionalRule(
                                    savedPermission.getId(),
                                    sourceRule.getConditionScript(),
                                    sourceRule.getJustification()
                                ))
                                .collect(Collectors.toList());
                            
                            conditionalRuleRepository.saveAll(newConditionalRules);
                            
                            logger.trace()
                                .message(AppConstants.LOG_CONDITIONAL_RULES_COPIED)
                                .detail(AppConstants.DETAIL_KEY_NEW_PERMISSION_ID, savedPermission.getId().toString())
                                .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_COUNT, newConditionalRules.size())
                                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                                .log();
                        }
                    }
                }
                
                logger.info()
                    .message(AppConstants.LOG_ALL_PERMISSIONS_COPIED)
                    .detail(AppConstants.DETAIL_KEY_NEW_ROLE_ID, savedRole.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSIONS_COPIED, sourceRolePermissions.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
            }
            
            // Convert to response
            RoleResponse response = convertToResponse(savedRole);
            
            logger.info()
                .message(AppConstants.LOG_ROLE_COPIED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_SOURCE_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_NEW_ROLE_ID, savedRole.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, savedRole.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            return response;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ROLE_COPY_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ROLE_COPY_FAILED, e);
        }
    }

    /**
     * Finds a role by ID and validates that it belongs to the specified tenant.
     * 
     * @param id the role ID
     * @param tenantId the tenant ID to validate against
     * @return the role entity
     * @throws RuntimeException if role is not found or belongs to different tenant
     */
    private Role findAndValidateRole(UUID id, String tenantId) {
        logger.debug()
            .message(AppConstants.LOG_ROLE_ACCESS_VALIDATION)
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        Role role = roleRepository.findById(id)
            .orElseThrow(() -> {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                return new RuntimeException(AppConstants.ROLE_NOT_FOUND);
            });

        // Validate tenant access
        if (!role.getTenantId().equals(tenantId)) {
            logger.warn()
                .message(AppConstants.LOG_ROLE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
                .detail(AppConstants.DETAIL_KEY_USER_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_TENANT_ID, role.getTenantId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw new RuntimeException(AppConstants.ROLE_ACCESS_DENIED);
        }

        logger.debug()
            .message(AppConstants.LOG_ROLE_VALIDATION_SUCCESS)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, id.toString())
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        return role;
    }

    /**
     * Converts a Role entity to RoleResponse DTO with user counts, permission counts, and owners data.
     * 
     * @param role the role entity
     * @return the role response DTO
     */
    private RoleResponse convertToResponse(Role role) {
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
     * Assigns a permission to a role with optional conditional rules.
     * 
     * <p>This method creates a new Permission for the specified Action,
     * associates it with the Role via RolePermission, and optionally
     * creates a ConditionalRule if conditionScript is provided.</p>
     * 
     * <p><strong>Cache Eviction:</strong> Clears all permission caches when a new permission is assigned
     * to ensure users immediately get the new permission on next check.
     * 
     * @param roleId the role ID to assign permission to
     * @param request the permission assignment request containing actionId, justification, and optional conditionScript
     * @return a map containing the created entities' IDs
     * @throws DuplicatePermissionException if the exact permission assignment already exists
     * @throws RuntimeException if role not found or other permission assignment failures
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Override
    public Map<String, UUID> assignPermissionToRole(UUID roleId, AssignPermissionToRoleRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ASSIGNING_PERMISSION_TO_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(request.hasConditionalRule()))
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, request.getConditionScript() != null ? request.getConditionScript() : "null")
                .detail(AppConstants.DETAIL_KEY_JUSTIFICATION, request.getJustification())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate that the role exists and belongs to the current tenant
            Role role = findAndValidateRole(roleId, tenantId);

            // Check if role is active
            logger.debug()
                .message(AppConstants.LOG_CHECKING_ROLE_ACTIVE_STATUS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_IS_ACTIVE, role.getIsActive())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            if (role.getIsActive() == null || !role.getIsActive()) {
                logger.warn()
                    .message(AppConstants.LOG_ROLE_IS_INACTIVE_CANNOT_ASSIGN)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .detail(AppConstants.DETAIL_KEY_IS_ACTIVE, role.getIsActive())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                throw new IllegalArgumentException(AppConstants.CANNOT_ASSIGN_PERMISSION_TO_INACTIVE_ROLE);
            }

            logger.debug()
                .message(AppConstants.LOG_ROLE_IS_ACTIVE_PROCEEDING)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Normalize condition script for consistent comparison
            String normalizedConditionScript = request.getConditionScript();
            if (normalizedConditionScript != null && !normalizedConditionScript.trim().isEmpty()) {
                normalizedConditionScript = normalizedConditionScript.trim();
            } else {
                normalizedConditionScript = null; // Normalize empty/whitespace to null
            }

            // Check if role already has ANY permission for this action (regardless of condition/justification)
            List<Permission> existingPermissions = permissionRepository.findAllByRoleIdAndActionId(roleId, request.getActionId());
            
            if (!existingPermissions.isEmpty()) {
                // Permission already exists for this role-action combination - show error
                logger.warn()
                    .message(AppConstants.PERMISSION_ALREADY_EXISTS_FOR_ROLE_ACTION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                    .detail(AppConstants.DETAIL_KEY_EXISTING_PERMISSION_COUNT, String.valueOf(existingPermissions.size()))
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                    
                throw new DuplicatePermissionException(
                    AppConstants.PERMISSION_ALREADY_ASSIGNED_TO_ROLE,
                    roleId.toString(),
                    request.getActionId().toString()
                );
            }

            logger.debug()
                .message(AppConstants.NO_EXISTING_PERMISSION_FOUND_FOR_ROLE_ACTION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, normalizedConditionScript != null ? normalizedConditionScript : "null")
                .detail(AppConstants.DETAIL_KEY_JUSTIFICATION, request.getJustification())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Create a new Permission for the action
            Permission permission = new Permission(request.getActionId());
            Permission savedPermission = permissionRepository.save(permission);

            logger.debug()
                .message(AppConstants.LOG_CREATED_NEW_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, savedPermission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            // Create RolePermission association
            RolePermission rolePermission = new RolePermission(roleId, savedPermission.getId());
            RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);

            logger.debug()
                .message(AppConstants.LOG_CREATED_ROLE_PERMISSION_ASSOCIATION)
                .detail(AppConstants.DETAIL_KEY_ROLE_PERMISSION_ID, savedRolePermission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, savedPermission.getId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_PERMISSION)
                .log();

            // Prepare response map
            Map<String, UUID> result = new HashMap<>();
            result.put("permissionId", savedPermission.getId());
            result.put("rolePermissionId", savedRolePermission.getId());

            // Create ConditionalRule (conditionScript can be null)
            ConditionalRule conditionalRule = new ConditionalRule(
                savedPermission.getId(),
                normalizedConditionScript,
                request.getJustification()
            );
            ConditionalRule savedConditionalRule = conditionalRuleRepository.save(conditionalRule);

            result.put("conditionalRuleId", savedConditionalRule.getId());

            logger.debug()
                .message(AppConstants.LOG_CREATED_CONDITIONAL_RULE)
                .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, savedConditionalRule.getId().toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, savedPermission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, normalizedConditionScript != null ? normalizedConditionScript : "null")
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_ASSIGNED_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, savedPermission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, savedConditionalRule.getId().toString())
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT_PROVIDED, String.valueOf(normalizedConditionScript != null))
                .detail(AppConstants.DETAIL_KEY_JUSTIFICATION, request.getJustification())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return result;

        } catch (DuplicatePermissionException e) {
            // Re-throw DuplicatePermissionException without wrapping
            logger.debug()
                .message(AppConstants.LOG_RETHROWING_DUPLICATE_EXCEPTION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw e;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_ASSIGN_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw new RuntimeException(AppConstants.PERMISSION_ASSIGNMENT_FAILED + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> submitPermissionAssignmentForApproval(UUID roleId, SubmitPermissionAssignmentForApprovalRequest request) {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Submit permission assignment for approval: Publishing event only")
            .detail("roleId", roleId.toString())
            .detail("actionId", request.getActionId().toString())
            .detail("workflowId", request.getWorkflowId())
            .detail("tenantId", tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            // Validate workflow ID
            if (request.getWorkflowId() == null || request.getWorkflowId().trim().isEmpty()) {
                throw new IllegalArgumentException("Workflow ID is required when submitting for approval.");
            }
            
            // Verify role exists and is active
            Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
            
            if (!role.getIsActive()) {
                throw new IllegalArgumentException("Cannot assign permissions to an inactive role");
            }
            
            // Verify action exists
            Action action = actionRepository.findById(request.getActionId())
                .orElseThrow(() -> new RuntimeException("Action not found with ID: " + request.getActionId()));
            
            // Check if role has owners
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(roleId);
            if (roleOwners == null || roleOwners.isEmpty()) {
                throw new IllegalArgumentException("Role has no owners. Cannot submit for approval.");
            }
            
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .toList();
            
            // Extract current user info
            String createdBy = tenantContextService.getCurrentUsername();
            String createdById = tenantContextService.getCurrentUserId();
            
            // Publish event to trigger workflow (NO permission assignment in DB)
            if (roleEventPublisher == null) {
                throw new RuntimeException("Cannot submit for approval: Event publishing is not available.");
            }
            
            roleEventPublisher.publishPermissionAssignedEvent(
                roleId,
                role.getRoleName(),
                request.getActionId(),
                action.getActionName(),
                request.getJustification(),
                request.getConditionScript(),
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                createdBy,
                createdById
            );
            
            logger.info()
                .message("Permission assignment submitted for approval workflow. Event published.")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Return response
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("actionId", request.getActionId());
            result.put("actionName", action.getActionName());
            result.put("workflowId", request.getWorkflowId());
            result.put("status", "PENDING_APPROVAL");
            result.put("ownerIds", ownerIds);
            
            return result;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit permission assignment for approval")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> submitPermissionRevocationForApproval(UUID roleId, SubmitPermissionRevocationForApprovalRequest request) {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Submit permission revocation for approval: Publishing event only")
            .detail("roleId", roleId.toString())
            .detail("actionId", request.getActionId().toString())
            .detail("workflowId", request.getWorkflowId())
            .detail("tenantId", tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            // Validate workflow ID
            if (request.getWorkflowId() == null || request.getWorkflowId().trim().isEmpty()) {
                throw new IllegalArgumentException("Workflow ID is required when submitting for approval.");
            }
            
            // Verify role exists and is active
            Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
            
            if (!role.getIsActive()) {
                throw new IllegalArgumentException("Cannot revoke permissions from an inactive role");
            }
            
            // Verify action exists
            Action action = actionRepository.findById(request.getActionId())
                .orElseThrow(() -> new RuntimeException("Action not found with ID: " + request.getActionId()));
            
            // Verify permission exists for this role and action
            Permission permission = permissionRepository.findByRoleIdAndActionId(roleId, request.getActionId())
                .orElseThrow(() -> new RuntimeException("Permission not found for role and action"));
            
            // Check if role has owners
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(roleId);
            if (roleOwners == null || roleOwners.isEmpty()) {
                throw new IllegalArgumentException("Role has no owners. Cannot submit for approval.");
            }
            
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .toList();
            
            // Extract current user info
            String createdBy = tenantContextService.getCurrentUsername();
            String createdById = tenantContextService.getCurrentUserId();
            
            // Publish event to trigger workflow (NO permission revocation in DB)
            if (roleEventPublisher == null) {
                throw new RuntimeException("Cannot submit for approval: Event publishing is not available.");
            }
            
            roleEventPublisher.publishPermissionRevokedEvent(
                roleId,
                role.getRoleName(),
                request.getActionId(),
                action.getActionName(),
                request.getJustification(),
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                createdBy,
                createdById
            );
            
            logger.info()
                .message("Permission revocation submitted for approval workflow. Event published.")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Return response
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("actionId", request.getActionId());
            result.put("actionName", action.getActionName());
            result.put("workflowId", request.getWorkflowId());
            result.put("status", "PENDING_APPROVAL");
            result.put("ownerIds", ownerIds);
            
            return result;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit permission revocation for approval")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            throw e;
        }
    }

    /**
     * Submits bulk permission assignments for approval workflow.
     * 
     * <p>This method validates the request but does NOT execute the assignments immediately.
     * It publishes an event to trigger the approval workflow.</p>
     * 
     * @param roleId the role ID
     * @param request the bulk assignment approval request
     * @return result containing workflow information and pending status
     */
    @Override
    public Map<String, Object> submitBulkPermissionAssignmentForApproval(UUID roleId, SubmitBulkPermissionAssignmentForApprovalRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Submitting bulk permission assignment for approval")
                .detail("roleId", roleId.toString())
                .detail("tenantId", tenantId)
                .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Verify role exists and is active
            Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
            
            if (!role.getIsActive()) {
                throw new IllegalArgumentException("Cannot assign permissions to an inactive role");
            }
            
            // Validate all actions exist
            List<UUID> actionIds = request.getAssignments().stream()
                .map(item -> item.getActionId())
                .toList();
            
            List<Action> actions = actionRepository.findAllById(actionIds);
            if (actions.size() != actionIds.size()) {
                List<UUID> foundIds = actions.stream().map(Action::getId).toList();
                List<UUID> missingIds = actionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
                throw new RuntimeException("Actions not found: " + missingIds);
            }
            
            // Check if role has owners
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(roleId);
            if (roleOwners == null || roleOwners.isEmpty()) {
                throw new IllegalArgumentException("Role has no owners. Cannot submit for approval.");
            }
            
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .toList();
            
            // Extract current user info
            String createdBy = tenantContextService.getCurrentUsername();
            String createdById = tenantContextService.getCurrentUserId();
            
            // Convert assignments to event payload format
            List<Map<String, Object>> assignmentPayloads = request.getAssignments().stream()
                .map(item -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("actionId", item.getActionId().toString());
                    // Find action name
                    actions.stream()
                        .filter(a -> a.getId().equals(item.getActionId()))
                        .findFirst()
                        .ifPresent(a -> payload.put("actionName", a.getActionName()));
                    payload.put("justification", item.getJustification());
                    payload.put("conditionScript", item.getConditionScript());
                    return payload;
                })
                .toList();
            
            // Publish event (NO database operation yet)
            if (roleEventPublisher == null) {
                throw new RuntimeException("Cannot submit for approval: Event publishing is not available.");
            }
            
            roleEventPublisher.publishBulkPermissionAssignedEvent(
                roleId,
                role.getRoleName(),
                assignmentPayloads,
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                createdBy,
                createdById
            );
            
            logger.info()
                .message("Bulk permission assignment submitted for approval workflow")
                .detail("roleId", roleId.toString())
                .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Return pending status
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("assignmentCount", request.getAssignmentCount());
            result.put("workflowId", request.getWorkflowId());
            result.put("status", "PENDING_APPROVAL");
            result.put("ownerIds", ownerIds);
            
            return result;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit bulk permission assignment for approval")
                .detail("roleId", roleId.toString())
                .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
                .detail("error", e.getMessage())
                .log();
            throw e;
        }
    }

    /**
     * Assigns multiple permissions to a role in a single atomic transaction.
     * 
     * <p>This method performs bulk permission assignments with the following guarantees:</p>
     * <ul>
     *   <li><strong>Atomicity:</strong> All assignments succeed or none do (rollback on any failure)</li>
     *   <li><strong>Validation:</strong> All actions are validated before any assignments</li>
     *   <li><strong>Consistency:</strong> Duplicate assignments are detected and prevented</li>
     *   <li><strong>Performance:</strong> Optimized batch processing with single cache eviction</li>
     * </ul>
     * 
     * <p><strong>Cache Eviction:</strong> Clears all permission caches after successful bulk assignment
     * to ensure new permissions take effect immediately.</p>
     * 
     * @param roleId the role ID to assign permissions to
     * @param request the bulk assignment request containing list of assignments
     * @return result object containing success status, permission IDs, and any errors
     * @throws IllegalArgumentException if role is inactive or request is invalid
     * @throws RuntimeException if transaction fails or database error occurs
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BulkAssignmentResult assignPermissionsBulk(UUID roleId, BulkAssignPermissionRequest request) {
        long startTime = System.currentTimeMillis();
        String tenantId = null;
        
        try {
            // Extract tenant ID from JWT token
            tenantId = tenantContextService.getCurrentTenantId();
            
            int totalRequested = request.getAssignmentCount();
            
            logger.info()
                .message("Starting bulk permission assignment")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("assignmentCount", String.valueOf(totalRequested))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate that the role exists and belongs to the current tenant
            Role role = findAndValidateRole(roleId, tenantId);

            // Check if role is active
            if (role.getIsActive() == null || !role.getIsActive()) {
                String errorMsg = AppConstants.CANNOT_ASSIGN_PERMISSION_TO_INACTIVE_ROLE;
                logger.warn()
                    .message("Bulk assignment failed: Role is inactive")
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return BulkAssignmentResult.failure(totalRequested, errorMsg);
            }

            // Phase 1: Validate all actions exist before starting assignments
            logger.debug()
                .message("Phase 1: Validating all actions exist")
                .detail("assignmentCount", String.valueOf(totalRequested))
                .log();

            List<UUID> actionIds = request.getAssignments().stream()
                .map(item -> item.getActionId())
                .toList();

            // Batch fetch all actions
            List<Action> actions = actionRepository.findAllById(actionIds);
            if (actions.size() != actionIds.size()) {
                List<UUID> foundIds = actions.stream().map(Action::getId).toList();
                List<UUID> missingIds = actionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
                
                String errorMsg = "Actions not found: " + missingIds;
                logger.error()
                    .message("Bulk assignment validation failed: Some actions not found")
                    .detail("missingActionIds", missingIds.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION)
                    .log();
                
                return BulkAssignmentResult.failure(totalRequested, errorMsg);
            }

            // Phase 2: Check for existing permissions to prevent duplicates
            logger.debug()
                .message("Phase 2: Checking for duplicate assignments")
                .log();

            List<Permission> existingPermissions = permissionRepository.findByRoleIdAndActionIdIn(roleId, actionIds);
            if (!existingPermissions.isEmpty()) {
                List<UUID> existingActionIds = existingPermissions.stream()
                    .map(Permission::getActionId)
                    .toList();
                
                String errorMsg = "Duplicate permissions detected for actions: " + existingActionIds;
                logger.error()
                    .message("Bulk assignment validation failed: Duplicate permissions")
                    .detail("duplicateActionIds", existingActionIds.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                    .log();
                
                return BulkAssignmentResult.failure(totalRequested, errorMsg);
            }

            // Phase 3: Execute all assignments atomically
            logger.debug()
                .message("Phase 3: Executing bulk assignments")
                .log();

            List<UUID> permissionIds = new ArrayList<>();
            int assignedCount = 0;

            for (BulkAssignmentItem item : request.getAssignments()) {
                try {
                    // Convert to single assignment request and reuse existing logic
                    AssignPermissionToRoleRequest singleRequest = item.toAssignPermissionRequest();
                    
                    // Call the existing single assignment method (without cache eviction - we do it once at the end)
                    Map<String, UUID> result = assignPermissionToRoleInternal(roleId, singleRequest, role);
                    
                    UUID permissionId = result.get("permissionId");
                    if (permissionId != null) {
                        permissionIds.add(permissionId);
                        assignedCount++;
                    }
                    
                } catch (Exception e) {
                    // Any failure causes full rollback due to @Transactional
                    String errorMsg = "Failed to assign permission for action " + item.getActionId() + ": " + e.getMessage();
                    logger.error(e)
                        .message("Bulk assignment failed during execution")
                        .detail("failedActionId", item.getActionId().toString())
                        .detail("assignedSoFar", String.valueOf(assignedCount))
                        .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                        .log();
                    
                    throw new RuntimeException(errorMsg, e);
                }
            }

            // Success - all assignments completed
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info()
                .message("Successfully completed bulk permission assignment")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail("assignedCount", String.valueOf(assignedCount))
                .detail("totalRequested", String.valueOf(totalRequested))
                .detail("durationMs", String.valueOf(duration))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return BulkAssignmentResult.success(permissionIds, totalRequested);

        } catch (Exception e) {
            logger.error(e)
                .message("Bulk permission assignment failed")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId != null ? tenantId : "unknown")
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Return failure result instead of throwing exception
            return BulkAssignmentResult.failure(
                request.getAssignmentCount(),
                "Bulk assignment failed: " + e.getMessage()
            );
        }
    }

    /**
     * Internal method for assigning single permission without cache eviction.
     * Used by bulk operations to avoid repeated cache clearing.
     * 
     * @param roleId the role ID
     * @param request the assignment request
     * @param role the validated role entity (to avoid re-fetching)
     * @return map containing created entity IDs
     */
    private Map<String, UUID> assignPermissionToRoleInternal(UUID roleId, AssignPermissionToRoleRequest request, Role role) {
        // Normalize condition script
        String normalizedConditionScript = request.getConditionScript();
        if (normalizedConditionScript != null && !normalizedConditionScript.trim().isEmpty()) {
            normalizedConditionScript = normalizedConditionScript.trim();
        } else {
            normalizedConditionScript = null;
        }

        // Find the Action
        Action action = actionRepository.findById(request.getActionId())
            .orElseThrow(() -> new RuntimeException(AppConstants.ACTION_NOT_FOUND + ": " + request.getActionId()));

        // Check for existing permission
        Optional<Permission> existingPermission = permissionRepository.findByRoleIdAndActionId(roleId, request.getActionId());
        if (existingPermission.isPresent()) {
            throw new DuplicatePermissionException(
                "Permission already exists for role " + role.getRoleName() + 
                " and action " + action.getActionName(),
                roleId.toString(),
                request.getActionId().toString()
            );
        }

        // Create Permission
        Permission permission = new Permission(request.getActionId());
        Permission savedPermission = permissionRepository.save(permission);

        // Create RolePermission association
        RolePermission rolePermission = new RolePermission(roleId, savedPermission.getId());
        RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);

        // Create ConditionalRule
        ConditionalRule conditionalRule = new ConditionalRule(
            savedPermission.getId(),
            normalizedConditionScript,
            request.getJustification()
        );
        ConditionalRule savedConditionalRule = conditionalRuleRepository.save(conditionalRule);

        // Prepare result
        Map<String, UUID> result = new HashMap<>();
        result.put("permissionId", savedPermission.getId());
        result.put("rolePermissionId", savedRolePermission.getId());
        result.put("conditionalRuleId", savedConditionalRule.getId());

        return result;
    }

    /**
     * Submits bulk permission revocations for approval workflow.
     * 
     * <p>This method validates the request but does NOT execute the revocations immediately.
     * It publishes an event to trigger the approval workflow.</p>
     * 
     * @param roleId the role ID
     * @param request the bulk revocation approval request
     * @return result containing workflow information and pending status
     */
    @Override
    public Map<String, Object> submitBulkPermissionRevocationForApproval(UUID roleId, SubmitBulkPermissionRevocationForApprovalRequest request) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Submitting bulk permission revocation for approval")
                .detail("roleId", roleId.toString())
                .detail("tenantId", tenantId)
                .detail("revocationCount", String.valueOf(request.getRevocationCount()))
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Verify role exists and is active
            Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
            
            if (!role.getIsActive()) {
                throw new IllegalArgumentException("Cannot revoke permissions from an inactive role");
            }
            
            // Validate all actions exist
            List<Action> actions = actionRepository.findAllById(request.getActionIds());
            if (actions.size() != request.getActionIds().size()) {
                List<UUID> foundIds = actions.stream().map(Action::getId).toList();
                List<UUID> missingIds = request.getActionIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
                throw new RuntimeException("Actions not found: " + missingIds);
            }
            
            // Check if role has owners
            List<RoleOwners> roleOwners = roleOwnersRepository.findByRoleId(roleId);
            if (roleOwners == null || roleOwners.isEmpty()) {
                throw new IllegalArgumentException("Role has no owners. Cannot submit for approval.");
            }
            
            List<UUID> ownerIds = roleOwners.stream()
                .map(RoleOwners::getUserId)
                .toList();
            
            // Extract current user info
            String createdBy = tenantContextService.getCurrentUsername();
            String createdById = tenantContextService.getCurrentUserId();
            
            // Publish event (NO database operation yet)
            if (roleEventPublisher == null) {
                throw new RuntimeException("Cannot submit for approval: Event publishing is not available.");
            }
            
            roleEventPublisher.publishBulkPermissionRevokedEvent(
                roleId,
                role.getRoleName(),
                request.getActionIds(),
                request.getJustification(),
                ownerIds,
                tenantId,
                request.getWorkflowId(),
                createdBy,
                createdById
            );
            
            logger.info()
                .message("Bulk permission revocation submitted for approval workflow")
                .detail("roleId", roleId.toString())
                .detail("revocationCount", String.valueOf(request.getRevocationCount()))
                .detail("workflowId", request.getWorkflowId())
                .log();
            
            // Return pending status
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("revocationCount", request.getRevocationCount());
            result.put("actionIds", request.getActionIds());
            result.put("justification", request.getJustification());
            result.put("workflowId", request.getWorkflowId());
            result.put("status", "PENDING_APPROVAL");
            result.put("ownerIds", ownerIds);
            
            return result;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to submit bulk permission revocation for approval")
                .detail("roleId", roleId.toString())
                .detail("revocationCount", String.valueOf(request.getRevocationCount()))
                .detail("error", e.getMessage())
                .log();
            throw e;
        }
    }

    /**
     * Revokes multiple permissions from a role in a single atomic operation.
     * 
     * <p>This method removes multiple permissions efficiently in phases:</p>
     * <ul>
     *   <li>Phase 1: Validate all action IDs exist</li>
     *   <li>Phase 2: Verify all permissions exist for the role</li>
     *   <li>Phase 3: Delete all permissions atomically (ConditionalRule -> RolePermission -> Permission)</li>
     * </ul>
     * 
     * <p>Transaction behavior: If any revocation fails, entire operation is rolled back.</p>
     * 
     * @param roleId the role ID to revoke permissions from
     * @param request the bulk revocation request containing list of action IDs (1-100 items)
     * @return result object containing success status, revoked permission IDs, and any errors
     * @throws IllegalArgumentException if role is inactive or request is invalid
     * @throws RuntimeException if transaction fails or database error occurs
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BulkRevocationResult revokePermissionsBulk(UUID roleId, BulkRevokePermissionRequest request) {
        long startTime = System.currentTimeMillis();
        String tenantId = null;
        
        try {
            // Extract tenant ID from JWT token
            tenantId = tenantContextService.getCurrentTenantId();
            
            int totalRequested = request.getRevocationCount();
            
            logger.info()
                .message("Starting bulk permission revocation")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("revocationCount", String.valueOf(totalRequested))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate that the role exists and belongs to the current tenant
            Role role = findAndValidateRole(roleId, tenantId);

            // Check if role is active
            if (role.getIsActive() == null || !role.getIsActive()) {
                String errorMsg = AppConstants.CANNOT_ASSIGN_PERMISSION_TO_INACTIVE_ROLE;
                logger.warn()
                    .message("Bulk revocation failed: Role is inactive")
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_NAME, role.getRoleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return BulkRevocationResult.failure(totalRequested, errorMsg);
            }

            List<UUID> actionIds = request.getActionIds();

            // Phase 1: Validate all actions exist
            logger.debug()
                .message("Phase 1: Validating all actions exist")
                .detail("revocationCount", String.valueOf(totalRequested))
                .log();

            List<Action> actions = actionRepository.findAllById(actionIds);
            if (actions.size() != actionIds.size()) {
                List<UUID> foundIds = actions.stream().map(Action::getId).toList();
                List<UUID> missingIds = actionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
                
                String errorMsg = "Actions not found: " + missingIds;
                logger.error()
                    .message("Bulk revocation validation failed: Some actions not found")
                    .detail("missingActionIds", missingIds.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION)
                    .log();
                
                return BulkRevocationResult.failure(totalRequested, errorMsg);
            }

            // Phase 2: Verify all permissions exist for the role
            logger.debug()
                .message("Phase 2: Verifying permissions exist")
                .log();

            List<Permission> existingPermissions = permissionRepository.findByRoleIdAndActionIdIn(roleId, actionIds);
            if (existingPermissions.size() != actionIds.size()) {
                List<UUID> foundActionIds = existingPermissions.stream()
                    .map(Permission::getActionId)
                    .toList();
                List<UUID> missingPermissionActionIds = actionIds.stream()
                    .filter(id -> !foundActionIds.contains(id))
                    .toList();
                
                String errorMsg = "Permissions not found for actions: " + missingPermissionActionIds;
                logger.error()
                    .message("Bulk revocation validation failed: Some permissions don't exist")
                    .detail("missingPermissionActionIds", missingPermissionActionIds.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                    .log();
                
                return BulkRevocationResult.failure(totalRequested, errorMsg);
            }

            // Phase 3: Execute all revocations atomically
            logger.debug()
                .message("Phase 3: Executing bulk revocations")
                .log();

            List<UUID> deletedPermissionIds = new ArrayList<>();
            int revokedCount = 0;

            for (Permission permission : existingPermissions) {
                try {
                    // Step 1: Delete conditional rule if it exists
                    List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
                    if (!conditionalRules.isEmpty()) {
                        conditionalRuleRepository.deleteAll(conditionalRules);
                        
                        logger.debug()
                            .message("Deleted conditional rules for permission")
                            .detail("conditionalRuleCount", String.valueOf(conditionalRules.size()))
                            .detail("permissionId", permission.getId().toString())
                            .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                            .log();
                    }

                    // Step 2: Delete role-permission association
                    Optional<RolePermission> rolePermissionOpt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permission.getId());
                    if (rolePermissionOpt.isPresent()) {
                        RolePermission rolePermission = rolePermissionOpt.get();
                        rolePermissionRepository.delete(rolePermission);
                        
                        logger.debug()
                            .message("Deleted role-permission association")
                            .detail("rolePermissionId", rolePermission.getId().toString())
                            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                            .detail("permissionId", permission.getId().toString())
                            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_PERMISSION)
                            .log();
                    }

                    // Step 3: Delete the permission itself
                    permissionRepository.delete(permission);
                    deletedPermissionIds.add(permission.getId());
                    revokedCount++;
                    
                    logger.debug()
                        .message("Deleted permission")
                        .detail("permissionId", permission.getId().toString())
                        .detail("actionId", permission.getActionId().toString())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                        .log();
                    
                } catch (Exception e) {
                    // Any failure causes full rollback due to @Transactional
                    String errorMsg = "Failed to revoke permission for action " + permission.getActionId() + ": " + e.getMessage();
                    logger.error(e)
                        .message("Bulk revocation failed during execution")
                        .detail("failedActionId", permission.getActionId().toString())
                        .detail("failedPermissionId", permission.getId().toString())
                        .detail("revokedSoFar", String.valueOf(revokedCount))
                        .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                        .log();
                    
                    throw new RuntimeException(errorMsg, e);
                }
            }

            // Success - all revocations completed
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info()
                .message("Successfully completed bulk permission revocation")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail("revokedCount", String.valueOf(revokedCount))
                .detail("totalRequested", String.valueOf(totalRequested))
                .detail("durationMs", String.valueOf(duration))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return BulkRevocationResult.success(deletedPermissionIds, totalRequested);

        } catch (Exception e) {
            logger.error(e)
                .message("Bulk permission revocation failed")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId != null ? tenantId : "unknown")
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            // Return failure result instead of throwing exception
            return BulkRevocationResult.failure(
                request.getRevocationCount(),
                "Bulk revocation failed: " + e.getMessage()
            );
        }
    }

    /**
     * Deletes a permission from a role.
     * 
     * <p>This method removes the Permission, RolePermission association, and ConditionalRule
     * for the specified role and action combination.</p>
     * 
     * <p><strong>Cache Eviction:</strong> Clears all permission caches when a permission is revoked
     * to ensure users immediately lose the permission on next check.
     * 
     * @param roleId the role ID to remove permission from
     * @param actionId the action ID to remove permission for
     * @return a map containing the IDs of deleted entities
     * @throws PermissionNotFoundException if permission not found for this role and action
     * @throws RuntimeException if role not found or other deletion failures
     */
    @CacheEvict(value = {"permissionChecks", "userRoles", "pageAccess"}, allEntries = true)
    @Override
    public Map<String, UUID> deletePermissionFromRole(UUID roleId, UUID actionId) {
        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_DELETING_PERMISSION_FROM_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate that the role exists and belongs to the current tenant
            Role role = findAndValidateRole(roleId, tenantId);

            // Find the permission for this role and action
            Permission permission = permissionRepository.findByRoleIdAndActionId(roleId, actionId)
                .orElseThrow(() -> {
                    logger.warn()
                        .message(AppConstants.LOG_PERMISSION_NOT_FOUND_FOR_DELETION)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                        .log();
                    return new PermissionNotFoundException(
                        AppConstants.PERMISSION_NOT_FOUND_FOR_ROLE,
                        roleId.toString(),
                        actionId.toString()
                    );
                });

            Map<String, UUID> result = new HashMap<>();
            result.put("permissionId", permission.getId());

            // Find and delete conditional rule first
            List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
            if (!conditionalRules.isEmpty()) {
                ConditionalRule conditionalRule = conditionalRules.get(0); // Should be only one
                conditionalRuleRepository.delete(conditionalRule);
                result.put("conditionalRuleId", conditionalRule.getId());
                
                logger.debug()
                    .message(AppConstants.LOG_DELETED_CONDITIONAL_RULE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, conditionalRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            }

            // Find and delete role-permission association
            Optional<RolePermission> rolePermissionOpt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permission.getId());
            if (rolePermissionOpt.isPresent()) {
                RolePermission rolePermission = rolePermissionOpt.get();
                rolePermissionRepository.delete(rolePermission);
                result.put("rolePermissionId", rolePermission.getId());
                
                logger.debug()
                    .message(AppConstants.LOG_DELETED_ROLE_PERMISSION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_PERMISSION_ID, rolePermission.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_PERMISSION)
                    .log();
            }

            // Finally delete the permission
            permissionRepository.delete(permission);
            
            logger.debug()
                .message(AppConstants.LOG_DELETED_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_DELETED_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return result;

        } catch (PermissionNotFoundException e) {
            // Re-throw PermissionNotFoundException without wrapping
            throw e;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_DELETE_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw new RuntimeException(AppConstants.PERMISSION_DELETION_FAILED + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing permission assignment for a role.
     * 
     * <p>This method modifies the conditional rule (script and justification) for an existing
     * permission. The permission must already be assigned to the role.</p>
     * 
     * @param roleId the role ID to update permission for
     * @param actionId the action ID of the permission to update
     * @param request the permission update request containing new condition script and justification
     * @return a map containing the updated entities' IDs (permissionId, conditionalRuleId)
     * @throws PermissionNotFoundException if permission not found for this role and action
     * @throws RuntimeException if update fails
     */
    @Override
    @Transactional
    public Map<String, UUID> updatePermissionForRole(UUID roleId, UUID actionId, AssignPermissionToRoleRequest request) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "updatePermissionForRole")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_UPDATING_PERMISSION_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(request.hasConditionalRule()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Find the permission
            Optional<Permission> permissionOpt = permissionRepository.findByRoleIdAndActionId(roleId, actionId);
            if (!permissionOpt.isPresent()) {
                logger.warn()
                    .message(AppConstants.LOG_PERMISSION_NOT_FOUND_FOR_UPDATE)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                throw new PermissionNotFoundException(
                    AppConstants.PERMISSION_NOT_FOUND_FOR_ROLE,
                    roleId.toString(),
                    actionId.toString()
                );
            }

            Permission permission = permissionOpt.get();
            Map<String, UUID> result = new HashMap<>();
            result.put("permissionId", permission.getId());

            logger.debug()
                .message(AppConstants.LOG_UPDATING_PERMISSION_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            // Handle conditional rule updates
            handleConditionalRuleUpdate(permission, request, result);

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_UPDATED_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(result.containsKey("conditionalRuleId")))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD, "updatePermissionForRole")
                .detail(AppConstants.DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return result;

        } catch (PermissionNotFoundException e) {
            logger.debug()
                .message(AppConstants.LOG_RETHROWING_DUPLICATE_EXCEPTION)
                .detail(AppConstants.DETAIL_KEY_EXCEPTION_TYPE, "PermissionNotFoundException")
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_UPDATE_PERMISSION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            throw new RuntimeException(AppConstants.PERMISSION_UPDATE_FAILED + ": " + e.getMessage(), e);
        }
    }

    /**
     * Handles updating or creating conditional rules for a permission.
     * 
     * <p>This method updates existing conditional rules or creates new ones based on whether
     * a condition script is provided in the request. If no condition script is provided,
     * the rule is created/updated with only justification.</p>
     * 
     * @param permission the permission entity to update conditional rules for
     * @param request the permission assignment request containing condition script and justification
     * @param result the result map to store the conditional rule ID
     */
    private void handleConditionalRuleUpdate(Permission permission, AssignPermissionToRoleRequest request, Map<String, UUID> result) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "handleConditionalRuleUpdate")
            .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
            .log();

        // Find existing conditional rule
        List<ConditionalRule> existingRules = conditionalRuleRepository.findByPermissionId(permission.getId());
        Optional<ConditionalRule> existingRuleOpt = existingRules.isEmpty() ? Optional.empty() : Optional.of(existingRules.get(0));

        if (request.hasConditionalRule()) {
            // Update or create conditional rule
            if (existingRuleOpt.isPresent()) {
                // Update existing rule
                ConditionalRule existingRule = existingRuleOpt.get();
                
                logger.debug()
                    .message(AppConstants.LOG_UPDATING_PERMISSION_FOR_ROLE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, existingRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_OLD_CONDITION_SCRIPT, existingRule.getConditionScript())
                    .detail(AppConstants.DETAIL_KEY_NEW_CONDITION_SCRIPT, request.getConditionScript())
                    .detail(AppConstants.DETAIL_KEY_OLD_JUSTIFICATION, existingRule.getJustification())
                    .detail(AppConstants.DETAIL_KEY_NEW_JUSTIFICATION, request.getJustification())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();

                existingRule.setConditionScript(request.getConditionScript());
                existingRule.setJustification(request.getJustification());
                existingRule = conditionalRuleRepository.save(existingRule);
                result.put("conditionalRuleId", existingRule.getId());

                logger.debug()
                    .message(AppConstants.LOG_UPDATED_CONDITIONAL_RULE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, existingRule.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            } else {
                // Create new conditional rule
                ConditionalRule newRule = new ConditionalRule();
                newRule.setPermissionId(permission.getId());
                newRule.setConditionScript(request.getConditionScript());
                newRule.setJustification(request.getJustification());
                newRule = conditionalRuleRepository.save(newRule);
                result.put("conditionalRuleId", newRule.getId());

                logger.debug()
                    .message(AppConstants.LOG_CREATED_NEW_CONDITIONAL_RULE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, newRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            }
        } else {
            // Update or create conditional rule with just justification (no condition script)
            if (existingRuleOpt.isPresent()) {
                // Update existing rule to remove condition script but keep justification
                ConditionalRule existingRule = existingRuleOpt.get();
                
                logger.debug()
                    .message(AppConstants.LOG_UPDATING_PERMISSION_FOR_ROLE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, existingRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_OLD_CONDITION_SCRIPT, existingRule.getConditionScript())
                    .detail(AppConstants.DETAIL_KEY_NEW_CONDITION_SCRIPT, "null")
                    .detail(AppConstants.DETAIL_KEY_OLD_JUSTIFICATION, existingRule.getJustification())
                    .detail(AppConstants.DETAIL_KEY_NEW_JUSTIFICATION, request.getJustification())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();

                existingRule.setConditionScript(null);
                existingRule.setJustification(request.getJustification());
                existingRule = conditionalRuleRepository.save(existingRule);
                result.put("conditionalRuleId", existingRule.getId());

                logger.debug()
                    .message(AppConstants.LOG_UPDATED_CONDITIONAL_RULE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, existingRule.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            } else {
                // Create new conditional rule with just justification
                ConditionalRule newRule = new ConditionalRule();
                newRule.setPermissionId(permission.getId());
                newRule.setConditionScript(null);
                newRule.setJustification(request.getJustification());
                newRule = conditionalRuleRepository.save(newRule);
                result.put("conditionalRuleId", newRule.getId());

                logger.debug()
                    .message(AppConstants.LOG_CREATED_NEW_CONDITIONAL_RULE)
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, newRule.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            }
        }

        logger.trace()
            .message(AppConstants.LOG_TRACE_EXITING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "handleConditionalRuleUpdate")
            .detail(AppConstants.DETAIL_KEY_SUCCESS, true)
            .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
            .log();
    }

    /**
     * Updates an existing permission assignment for a role (overloaded version).
     * 
     * <p>This is a convenience method that converts UpdatePermissionRequest to 
     * AssignPermissionToRoleRequest and delegates to the primary update method.</p>
     * 
     * @param roleId the role ID to update permission for
     * @param actionId the action ID of the permission to update
     * @param request the update permission request
     * @return a map containing the updated entities' IDs (permissionId, conditionalRuleId)
     * @throws PermissionNotFoundException if permission not found for this role and action
     * @throws RuntimeException if update fails
     */
    @Override
    @Transactional
    public Map<String, UUID> updatePermissionForRole(UUID roleId, UUID actionId, UpdatePermissionRequest request) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "updatePermissionForRole[UpdatePermissionRequest]")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        // Convert UpdatePermissionRequest to AssignPermissionToRoleRequest
        AssignPermissionToRoleRequest assignRequest = new AssignPermissionToRoleRequest(
            actionId,
            request.getJustification(),
            request.getConditionScript()
        );

        logger.debug()
            .message(AppConstants.CONVERTING_UPDATE_PERMISSION_REQUEST)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
            .detail(AppConstants.DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(request.hasConditionalRule()))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        // Delegate to the existing implementation
        Map<String, UUID> result = updatePermissionForRole(roleId, actionId, assignRequest);

        logger.trace()
            .message(AppConstants.LOG_TRACE_EXITING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "updatePermissionForRole[UpdatePermissionRequest]")
            .detail(AppConstants.DETAIL_KEY_SUCCESS, true)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        return result;
    }

    /**
     * Retrieves permission details for a specific role and action.
     * 
     * @param roleId the role ID to get permission details for
     * @param actionId the action ID to get permission details for
     * @return permission details including justification and conditional script
     */
    @Override
    public PermissionDetailsResponse getPermissionDetails(UUID roleId, UUID actionId) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD, "getPermissionDetails")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.PROCESSING_GET_PERMISSION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            // Validate role exists and belongs to tenant
            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED_FOR_PERMISSION_DETAILS)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                throw new PermissionNotFoundException(
                    AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED,
                    roleId.toString(),
                    actionId.toString()
                );
            }

            Role role = roleOpt.get();

            // Find the permission for this role and action combination
            logger.debug()
                .message(AppConstants.SEARCHING_FOR_PERMISSION_BY_ROLE_AND_ACTION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            Optional<Permission> permissionOpt = permissionRepository.findByRoleIdAndActionId(roleId, actionId);
            if (permissionOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.PERMISSION_NOT_FOUND_FOR_ROLE_AND_ACTION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                    .log();
                throw new PermissionNotFoundException(
                    AppConstants.PERMISSION_NOT_ASSIGNED_TO_ROLE_FOR_ACTION,
                    roleId.toString(),
                    actionId.toString()
                );
            }

            Permission permission = permissionOpt.get();

            // Fetch conditional rule if exists
            List<ConditionalRule> conditionalRules = conditionalRuleRepository.findByPermissionId(permission.getId());
            
            String justification = null;
            String conditionScript = null;
            boolean hasConditionalRule = false;
            UUID conditionalRuleId = null;

            if (!conditionalRules.isEmpty()) {
                ConditionalRule conditionalRule = conditionalRules.get(0);
                justification = conditionalRule.getJustification();
                conditionScript = conditionalRule.getConditionScript();
                hasConditionalRule = true;
                conditionalRuleId = conditionalRule.getId();

                logger.debug()
                    .message(AppConstants.CONDITIONAL_RULE_FOUND_FOR_PERMISSION)
                    .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_CONDITIONAL_RULE_ID, conditionalRuleId.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            }

            // Build response
            PermissionDetailsResponse response = 
                new PermissionDetailsResponse(
                    permission.getId(),
                    actionId,
                    null, // Action name would require joining with Action table
                    roleId,
                    role.getRoleName(),
                    justification,
                    conditionScript,
                    hasConditionalRule,
                    conditionalRuleId
                );

            logger.info()
                .message(AppConstants.PERMISSION_DETAILS_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.DETAIL_KEY_PERMISSION_ID, permission.getId().toString())
                .detail(AppConstants.DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(hasConditionalRule))
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD, "getPermissionDetails")
                .detail(AppConstants.DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();

            return response;

        } catch (PermissionNotFoundException e) {
            logger.error(e)
                .message(AppConstants.PERMISSION_NOT_FOUND_EXCEPTION)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.FAILED_TO_GET_PERMISSION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PERMISSION)
                .log();
            throw new RuntimeException(AppConstants.FAILED_TO_GET_PERMISSION_DETAILS_WITH_REASON + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all users in the tenant with their role assignment status for a specific role.
     * 
     * @param roleId the role ID to check assignments for
     * @param page page number (0-indexed)
     * @param size page size
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing all users with hasRole status
     */
    @Override
    public GetUsersByRoleResponse getUsersByRole(UUID roleId, Integer page, Integer size, String statusFilter, String search) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_PROCESSING_GET_USERS_BY_ROLE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", statusFilter)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate page size (must be 5, 10, 25, or 50)
            if (size != 5 && size != 10 && size != 25 && size != 50) {
                logger.warn()
                    .message("Invalid page size requested, defaulting to 10")
                    .detail("requestedSize", size)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                size = 10;
            }

            // Validate status filter
            if (statusFilter == null || (!statusFilter.equalsIgnoreCase("ALL") 
                && !statusFilter.equalsIgnoreCase("ACTIVE") 
                && !statusFilter.equalsIgnoreCase("INACTIVE"))) {
                logger.warn()
                    .message("Invalid status filter, defaulting to ALL")
                    .detail("requestedFilter", statusFilter)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                statusFilter = "ALL";
            }

            // First, validate that the role exists and belongs to the tenant
            logger.debug()
                .message(AppConstants.LOG_ROLE_VALIDATION_FOR_USERS_RETRIEVAL)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND_FOR_USERS_RETRIEVAL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return new GetUsersByRoleResponse(roleId.toString(), tenantId, AppConstants.ROLE_NOT_FOUND_FOR_USERS_RETRIEVAL);
            }

            // Fetch all users in the tenant via gRPC
            logger.debug()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_VIA_GRPC)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();

            GetUsersByTenantResponse grpcResponse = userGrpcClient.getUsersByTenant(tenantId);
            
            // gRPC response is valid if we get here without exception
            if (grpcResponse.getUsersList().isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_NO_USERS_FOUND_IN_TENANT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
            }

            // Fetch user IDs who have this specific role
            logger.debug()
                .message(AppConstants.LOG_FETCHING_ROLE_ASSIGNMENTS_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UUID> userIdsWithRole = userRoleRepository.findUserIdsByRoleIdAndTenantId(roleId, tenantId);
            Set<UUID> userIdsWithRoleSet = new HashSet<>(userIdsWithRole);

            logger.debug()
                .message(AppConstants.LOG_FOUND_USERS_WITH_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, userIdsWithRole.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Convert gRPC users to UserWithRoleDto and apply status filter
            logger.debug()
                .message(AppConstants.LOG_MAPPING_USERS_WITH_ROLE_STATUS)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, grpcResponse.getTotalCount())
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, userIdsWithRole.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UserWithRoleDto> usersWithRole = new ArrayList<>();
            List<UserWithRoleDto> usersWithoutRole = new ArrayList<>();

            for (var userInfo : grpcResponse.getUsersList()) {
                UserDto userDto = convertToUserDto(userInfo);
                
                // Apply status filter
                if (!statusFilter.equalsIgnoreCase("ALL")) {
                    if (statusFilter.equalsIgnoreCase("ACTIVE") && !userDto.getStatus().equalsIgnoreCase("ACTIVE")) {
                        continue;
                    }
                    if (statusFilter.equalsIgnoreCase("INACTIVE") && !userDto.getStatus().equalsIgnoreCase("INACTIVE")) {
                        continue;
                    }
                }
                
                // Apply search filter (case-insensitive search in username, displayName, and email)
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase().trim();
                    boolean matchesSearch = false;
                    
                    if (userDto.getUsername() != null && userDto.getUsername().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getDisplayName() != null && userDto.getDisplayName().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getEmail() != null && userDto.getEmail().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    
                    if (!matchesSearch) {
                        continue;
                    }
                }
                
                UUID userId = UUID.fromString(userDto.getId());
                boolean hasRole = userIdsWithRoleSet.contains(userId);
                
                UserWithRoleDto userWithRoleDto = new UserWithRoleDto(userDto, hasRole);
                
                if (hasRole) {
                    usersWithRole.add(userWithRoleDto);
                } else {
                    usersWithoutRole.add(userWithRoleDto);
                }
            }

            // Sort users: those with role first, then those without
            logger.debug()
                .message(AppConstants.LOG_SORTING_USERS_BY_ROLE_STATUS)
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, usersWithRole.size())
                .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_ROLE_COUNT, usersWithoutRole.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UserWithRoleDto> sortedUsers = new ArrayList<>();
            sortedUsers.addAll(usersWithRole);
            sortedUsers.addAll(usersWithoutRole);

            // Calculate pagination
            int totalFilteredUsers = sortedUsers.size();
            int totalPages = (int) Math.ceil((double) totalFilteredUsers / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalFilteredUsers);

            // Apply pagination
            List<UserWithRoleDto> paginatedUsers = new ArrayList<>();
            if (startIndex < totalFilteredUsers) {
                paginatedUsers = sortedUsers.subList(startIndex, endIndex);
            }

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_USERS_BY_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, totalFilteredUsers)
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, usersWithRole.size())
                .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_ROLE_COUNT, usersWithoutRole.size())
                .detail("page", page)
                .detail("size", size)
                .detail("totalPages", totalPages)
                .detail("currentPageSize", paginatedUsers.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetUsersByRoleResponse(
                paginatedUsers,
                totalFilteredUsers,
                usersWithRole.size(),
                usersWithoutRole.size(),
                page,
                size,
                totalPages,
                roleId.toString(),
                tenantId,
                AppConstants.USERS_BY_ROLE_RETRIEVED_SUCCESS
            );

        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_USERS_BY_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetUsersByRoleResponse(roleId.toString(), tenantId, AppConstants.FAILED_TO_RETRIEVE_USERS_BY_ROLE + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves manually assigned users for a specific role.
     * 
     * <p>This method returns only users who have been manually assigned to the role
     * (assignmentType = MANUAL). Users assigned via dynamic rules are excluded from the results.
     * The returned users can be filtered by status (ACTIVE/INACTIVE) and searched by username,
     * displayName, or email.</p>
     * 
     * @param roleId the role ID to retrieve manual assignments for
     * @param page page number (0-indexed)
     * @param size page size (must be 5, 10, 25, or 50)
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing manually assigned users with pagination details
     */
    @Override
    public GetUsersByRoleResponse getManuallyAssignedUsersByRole(UUID roleId, Integer page, Integer size, String statusFilter, String search) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_PROCESSING_GET_MANUAL_USERS_BY_ROLE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", statusFilter)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate page size (must be 5, 10, 25, or 50)
            if (size != 5 && size != 10 && size != 25 && size != 50) {
                logger.warn()
                    .message(AppConstants.LOG_INVALID_PAGE_SIZE_DEFAULTING)
                    .detail("requestedSize", size)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                size = 10;
            }

            // Validate status filter
            if (statusFilter == null || (!statusFilter.equalsIgnoreCase("ALL") 
                && !statusFilter.equalsIgnoreCase("ACTIVE") 
                && !statusFilter.equalsIgnoreCase("INACTIVE"))) {
                logger.warn()
                    .message(AppConstants.LOG_INVALID_STATUS_FILTER_DEFAULTING)
                    .detail("requestedFilter", statusFilter)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                statusFilter = "ALL";
            }

            // Validate that the role exists and belongs to the tenant
            logger.debug()
                .message(AppConstants.LOG_ROLE_VALIDATION_FOR_USERS_RETRIEVAL)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND_FOR_USERS_RETRIEVAL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return new GetUsersByRoleResponse(roleId.toString(), tenantId, AppConstants.ROLE_NOT_FOUND_FOR_USERS_RETRIEVAL);
            }

            // Fetch all users in the tenant via gRPC
            logger.debug()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_VIA_GRPC)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();

            GetUsersByTenantResponse grpcResponse = userGrpcClient.getUsersByTenant(tenantId);
            
            if (grpcResponse.getUsersList().isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_NO_USERS_FOUND_IN_TENANT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
            }

            // Fetch user IDs who have this role with MANUAL assignment type only
            logger.debug()
                .message(AppConstants.LOG_FETCHING_MANUAL_USERS_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("assignmentType", "MANUAL")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UUID> userIdsWithRole = userRoleRepository.findUserIdsByRoleIdAndTenantIdAndAssignmentType(
                roleId, tenantId, AssignmentType.MANUAL);
            Set<UUID> userIdsWithRoleSet = new HashSet<>(userIdsWithRole);

            logger.debug()
                .message(AppConstants.LOG_FOUND_MANUAL_USERS_WITH_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, userIdsWithRole.size())
                .detail("assignmentType", "MANUAL")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Convert gRPC users to UserWithRoleDto and apply filters
            List<UserWithRoleDto> usersWithRole = new ArrayList<>();

            for (var userInfo : grpcResponse.getUsersList()) {
                UserDto userDto = convertToUserDto(userInfo);
                UUID userId = UUID.fromString(userDto.getId());
                
                // Only include users who have this role with MANUAL assignment
                if (!userIdsWithRoleSet.contains(userId)) {
                    continue;
                }
                
                // Apply status filter
                if (!statusFilter.equalsIgnoreCase("ALL")) {
                    if (statusFilter.equalsIgnoreCase("ACTIVE") && !userDto.getStatus().equalsIgnoreCase("ACTIVE")) {
                        continue;
                    }
                    if (statusFilter.equalsIgnoreCase("INACTIVE") && !userDto.getStatus().equalsIgnoreCase("INACTIVE")) {
                        continue;
                    }
                }
                
                // Apply search filter
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase().trim();
                    boolean matchesSearch = false;
                    
                    if (userDto.getUsername() != null && userDto.getUsername().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getDisplayName() != null && userDto.getDisplayName().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getEmail() != null && userDto.getEmail().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    
                    if (!matchesSearch) {
                        continue;
                    }
                }
                
                UserWithRoleDto userWithRoleDto = new UserWithRoleDto(userDto, true);
                usersWithRole.add(userWithRoleDto);
            }

            // Calculate pagination
            int totalFilteredUsers = usersWithRole.size();
            int totalPages = (int) Math.ceil((double) totalFilteredUsers / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalFilteredUsers);

            // Apply pagination
            List<UserWithRoleDto> paginatedUsers = new ArrayList<>();
            if (startIndex < totalFilteredUsers) {
                paginatedUsers = usersWithRole.subList(startIndex, endIndex);
            }

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_MANUAL_USERS_BY_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, totalFilteredUsers)
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, usersWithRole.size())
                .detail("page", page)
                .detail("size", size)
                .detail("totalPages", totalPages)
                .detail("currentPageSize", paginatedUsers.size())
                .detail("assignmentType", "MANUAL")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetUsersByRoleResponse(
                paginatedUsers,
                totalFilteredUsers,
                usersWithRole.size(),
                0, // usersWithoutRole count is 0 for this endpoint
                page,
                size,
                totalPages,
                roleId.toString(),
                tenantId,
                AppConstants.MANUALLY_ASSIGNED_USERS_RETRIEVED_SUCCESS
            );

        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_MANUAL_USERS_BY_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetUsersByRoleResponse(roleId.toString(), tenantId, AppConstants.FAILED_TO_RETRIEVE_MANUAL_USERS + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves role members with their assignment type information.
     * 
     * <p>This method returns all users assigned to a specific role along with their
     * assignment type (MANUAL or DYNAMIC). Supports pagination, filtering by status,
     * and searching by username, displayName, or email.</p>
     * 
     * @param roleId the role ID to retrieve members for
     * @param page page number (0-indexed)
     * @param size page size (must be 5, 10, 25, or 50)
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing role members with assignment type information
     */
    @Override
    public GetRoleMembersResponse getRoleMembers(UUID roleId, Integer page, Integer size, String statusFilter, String search) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_PROCESSING_GET_ROLE_MEMBERS_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", statusFilter)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Validate page size (must be 5, 10, 25, or 50)
            if (size != 5 && size != 10 && size != 25 && size != 50) {
                logger.warn()
                    .message(AppConstants.LOG_INVALID_PAGE_SIZE_DEFAULTING)
                    .detail("requestedSize", size)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                size = 10;
            }

            // Validate status filter
            if (statusFilter == null || (!statusFilter.equalsIgnoreCase("ALL") 
                && !statusFilter.equalsIgnoreCase("ACTIVE") 
                && !statusFilter.equalsIgnoreCase("INACTIVE"))) {
                logger.warn()
                    .message(AppConstants.LOG_INVALID_STATUS_FILTER_DEFAULTING)
                    .detail("requestedFilter", statusFilter)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                statusFilter = "ALL";
            }

            // Validate that the role exists and belongs to the tenant
            logger.debug()
                .message(AppConstants.LOG_ROLE_VALIDATION_FOR_USERS_RETRIEVAL)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND_FOR_MEMBERS_RETRIEVAL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return new GetRoleMembersResponse(roleId.toString(), tenantId, AppConstants.ROLE_NOT_FOUND_FOR_MEMBERS_RETRIEVAL);
            }

            // Fetch all users in the tenant via gRPC
            logger.debug()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_VIA_GRPC)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();

            GetUsersByTenantResponse grpcResponse = userGrpcClient.getUsersByTenant(tenantId);
            
            if (grpcResponse.getUsersList().isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_NO_USERS_FOUND_IN_TENANT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
            }

            // Fetch all user role assignments for this role
            logger.debug()
                .message(AppConstants.LOG_FETCHING_ROLE_MEMBERS_WITH_ASSIGNMENT_TYPE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UserRole> userRoles = userRoleRepository.findByRoleIdAndTenantId(roleId, tenantId);
            
            // Create a map of userId to assignment type for quick lookup
            Map<UUID, AssignmentType> userAssignmentMap = new HashMap<>();
            for (UserRole userRole : userRoles) {
                userAssignmentMap.put(userRole.getUserId(), userRole.getAssignmentType());
            }

            logger.debug()
                .message(AppConstants.LOG_FOUND_ROLE_MEMBERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TOTAL_MEMBERS_COUNT, userRoles.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Convert gRPC users to RoleMemberDto and apply filters
            List<RoleMemberDto> roleMembers = new ArrayList<>();
            int manualCount = 0;
            int dynamicCount = 0;

            logger.debug()
                .message(AppConstants.LOG_FILTERING_ROLE_MEMBERS)
                .detail("statusFilter", statusFilter)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            for (var userInfo : grpcResponse.getUsersList()) {
                UserDto userDto = convertToUserDto(userInfo);
                UUID userId = UUID.fromString(userDto.getId());
                
                // Only include users who have this role
                if (!userAssignmentMap.containsKey(userId)) {
                    continue;
                }

                AssignmentType assignmentType = userAssignmentMap.get(userId);
                
                // Apply status filter
                if (!statusFilter.equalsIgnoreCase("ALL")) {
                    if (statusFilter.equalsIgnoreCase("ACTIVE") && !userDto.getStatus().equalsIgnoreCase("ACTIVE")) {
                        continue;
                    }
                    if (statusFilter.equalsIgnoreCase("INACTIVE") && !userDto.getStatus().equalsIgnoreCase("INACTIVE")) {
                        continue;
                    }
                }
                
                // Apply search filter
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.toLowerCase().trim();
                    boolean matchesSearch = false;
                    
                    if (userDto.getUsername() != null && userDto.getUsername().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getDisplayName() != null && userDto.getDisplayName().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    if (userDto.getEmail() != null && userDto.getEmail().toLowerCase().contains(searchLower)) {
                        matchesSearch = true;
                    }
                    
                    if (!matchesSearch) {
                        continue;
                    }
                }
                
                // Create RoleMemberDto with assignment type
                RoleMemberDto roleMemberDto = new RoleMemberDto(userDto, assignmentType);
                roleMembers.add(roleMemberDto);
                
                // Count assignment types
                if (assignmentType == AssignmentType.MANUAL) {
                    manualCount++;
                } else {
                    dynamicCount++;
                }
            }

            // Calculate pagination
            int totalFilteredMembers = roleMembers.size();
            int totalPages = (int) Math.ceil((double) totalFilteredMembers / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalFilteredMembers);

            // Apply pagination
            List<RoleMemberDto> paginatedMembers = new ArrayList<>();
            if (startIndex < totalFilteredMembers) {
                paginatedMembers = roleMembers.subList(startIndex, endIndex);
            }

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_ROLE_MEMBERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_MEMBERS_COUNT, totalFilteredMembers)
                .detail(AppConstants.DETAIL_KEY_MANUAL_ASSIGNMENT_COUNT, manualCount)
                .detail(AppConstants.DETAIL_KEY_DYNAMIC_ASSIGNMENT_COUNT, dynamicCount)
                .detail("page", page)
                .detail("size", size)
                .detail("totalPages", totalPages)
                .detail("currentPageSize", paginatedMembers.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetRoleMembersResponse(
                paginatedMembers,
                totalFilteredMembers,
                manualCount,
                dynamicCount,
                page,
                size,
                totalPages,
                roleId.toString(),
                tenantId,
                AppConstants.ROLE_MEMBERS_RETRIEVED_SUCCESS
            );

        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_ROLE_MEMBERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetRoleMembersResponse(roleId.toString(), tenantId, AppConstants.FAILED_TO_RETRIEVE_ROLE_MEMBERS + ": " + e.getMessage());
        }
    }

    /**
     * Converts gRPC UserInfo to UserDto.
     * 
     * @param userInfo gRPC UserInfo object
     * @return UserDto object
     */
    private UserDto convertToUserDto(com.aidentx.auth.grpc.UserInfo userInfo) {
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
     * Validates that the provided owner IDs are valid users in the correct tenant
     * and creates role ownership relationships.
     * 
     * @param roleId the ID of the role
     * @param ownerIds list of user IDs to validate and assign as owners
     * @param tenantId the tenant ID to validate against
     * @throws RuntimeException if any owner ID is invalid or from wrong tenant
     */
    private void validateAndCreateRoleOwners(UUID roleId, List<UUID> ownerIds, String tenantId) {
        logger.info()
            .message(AppConstants.LOG_VALIDATING_ROLE_OWNERS_VIA_GRPC)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_KEY_OWNER_COUNT, ownerIds.size())
            .log();

        try {
            // Get all tenant users via gRPC
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create a set of valid user IDs for quick lookup
            Set<UUID> validUserIds = new HashSet<>();
            for (UserInfo user : tenantUsers) {
                validUserIds.add(UUID.fromString(user.getId()));
            }
            
            // Validate each owner ID
            List<UUID> invalidOwnerIds = new ArrayList<>();
            for (UUID ownerId : ownerIds) {
                if (!validUserIds.contains(ownerId)) {
                    invalidOwnerIds.add(ownerId);
                }
            }
            
            if (!invalidOwnerIds.isEmpty()) {
                logger.error()
                    .message(AppConstants.LOG_INVALID_OWNER_IDS_PROVIDED)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_INVALID_OWNER_IDS, invalidOwnerIds.toString())
                    .log();
                throw new IllegalArgumentException(AppConstants.EXCEPTION_INVALID_OWNER_IDS + invalidOwnerIds + 
                    AppConstants.EXCEPTION_USERS_MUST_BELONG_TO_TENANT + tenantId);
            }
            
            // Create role ownership records for valid users
            List<RoleOwners> roleOwnersList = new ArrayList<>();
            for (UUID ownerId : ownerIds) {
                RoleOwners roleOwner = new RoleOwners(roleId, ownerId);
                roleOwnersList.add(roleOwner);
            }
            
            // Save all role ownership records
            List<RoleOwners> savedOwners = roleOwnersRepository.saveAll(roleOwnersList);
            
            logger.info()
                .message(AppConstants.LOG_ROLE_OWNERSHIP_CREATED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_OWNERS_CREATED, savedOwners.size())
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_VALIDATE_CREATE_OWNERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_OWNER_COUNT, ownerIds.size())
                .log();
            throw new RuntimeException(AppConstants.EXCEPTION_FAILED_TO_VALIDATE_ROLE_OWNERS + e.getMessage(), e);
        }
    }

    /**
     * Retrieves owners data for a specific role by fetching owner IDs from database
     * and getting their full user information via gRPC.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant ID for validation
     * @return list of UserDto objects representing role owners
     */
    private List<UserDto> getRoleOwnersData(UUID roleId, String tenantId) {
        try {
            // Get owner user IDs from role_owners table
            List<UUID> ownerIds = roleOwnersRepository.findUserIdsByRoleId(roleId);
            
            if (ownerIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            logger.debug()
                .message(AppConstants.LOG_FETCHING_OWNERS_DATA_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_OWNER_COUNT, ownerIds.size())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .log();
            
            // Get all tenant users via gRPC
            List<UserInfo> tenantUsers = userGrpcClient.getUserListByTenant(tenantId);
            
            // Create a map for quick lookup of users by ID
            Map<UUID, UserInfo> userMap = new HashMap<>();
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
                } else {
                    logger.warn()
                        .message(AppConstants.LOG_ROLE_OWNER_USER_NOT_FOUND)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.DETAIL_KEY_OWNER_ID, ownerId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .log();
                }
            }
            
            logger.debug()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_OWNERS_DATA)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_VALID_OWNERS_COUNT, ownersData.size())
                .detail(AppConstants.DETAIL_KEY_TOTAL_OWNER_IDS, ownerIds.size())
                .log();
            
            return ownersData;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_OWNERS_DATA)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .log();
            
            // Return empty list instead of throwing exception to avoid breaking role retrieval
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves all users in the tenant with their ownership status for a specific role.
     * 
     * <p>This method returns all users in the tenant, with each user marked as either
     * an owner or non-owner of the specified role. Owners are sorted first in the result list.</p>
     * 
     * @param roleId the role ID to check ownership for
     * @return response containing all users with ownership status
     */
    @Override
    public GetRoleOwnersResponse getRoleOwners(UUID roleId) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_PROCESSING_GET_ROLE_OWNERS_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // First, validate that the role exists and belongs to the tenant
            logger.debug()
                .message(AppConstants.LOG_ROLE_VALIDATION_FOR_OWNERS_RETRIEVAL)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            Optional<Role> roleOpt = roleRepository.findByIdAndTenantId(roleId, tenantId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ROLE_NOT_FOUND_FOR_OWNERS_RETRIEVAL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                return new GetRoleOwnersResponse(
                    roleId.toString(), tenantId, AppConstants.ROLE_NOT_FOUND_FOR_OWNERS_RETRIEVAL);
            }

            // Fetch all users in the tenant via gRPC
            logger.debug()
                .message(AppConstants.LOG_FETCHING_TENANT_USERS_VIA_GRPC)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                .log();

            GetUsersByTenantResponse grpcResponse = userGrpcClient.getUsersByTenant(tenantId);
            
            if (grpcResponse.getUsersList().isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_NO_USERS_FOUND_IN_TENANT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.OBJECT_TYPE_GRPC_USER_DATA)
                    .log();
            }

            // Fetch user IDs who are owners of this role
            logger.debug()
                .message(AppConstants.LOG_FETCHING_ROLE_OWNERS_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<RoleOwners> roleOwnersList = roleOwnersRepository.findByRoleId(roleId);
            Set<UUID> ownerUserIds = roleOwnersList.stream()
                .map(RoleOwners::getUserId)
                .collect(Collectors.toSet());

            logger.debug()
                .message(AppConstants.LOG_FOUND_OWNERS_FOR_ROLE)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_OWNERS_COUNT, ownerUserIds.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            // Convert gRPC users to UserWithOwnershipDto
            logger.debug()
                .message(AppConstants.LOG_MAPPING_USERS_WITH_OWNERSHIP_STATUS)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, grpcResponse.getTotalCount())
                .detail(AppConstants.DETAIL_KEY_OWNERS_COUNT, ownerUserIds.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UserWithOwnershipDto> usersWithOwnership = new ArrayList<>();
            List<UserWithOwnershipDto> usersWithoutOwnership = new ArrayList<>();

            for (var userInfo : grpcResponse.getUsersList()) {
                UserDto userDto = convertToUserDto(userInfo);
                UUID userId = UUID.fromString(userDto.getId());
                boolean isOwner = ownerUserIds.contains(userId);
                
                UserWithOwnershipDto userWithOwnershipDto = 
                    new UserWithOwnershipDto(userDto, isOwner);
                
                if (isOwner) {
                    usersWithOwnership.add(userWithOwnershipDto);
                } else {
                    usersWithoutOwnership.add(userWithOwnershipDto);
                }
            }

            // Sort users: owners first, then non-owners
            logger.debug()
                .message(AppConstants.LOG_SORTING_USERS_BY_OWNERSHIP_STATUS)
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_OWNERSHIP_COUNT, usersWithOwnership.size())
                .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_OWNERSHIP_COUNT, usersWithoutOwnership.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            List<UserWithOwnershipDto> sortedUsers = new ArrayList<>();
            sortedUsers.addAll(usersWithOwnership);
            sortedUsers.addAll(usersWithoutOwnership);

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_ROLE_OWNERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, sortedUsers.size())
                .detail(AppConstants.DETAIL_KEY_USERS_WITH_OWNERSHIP_COUNT, usersWithOwnership.size())
                .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_OWNERSHIP_COUNT, usersWithoutOwnership.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetRoleOwnersResponse(
                sortedUsers,
                sortedUsers.size(),
                usersWithOwnership.size(),
                usersWithoutOwnership.size(),
                roleId.toString(),
                tenantId,
                AppConstants.ROLE_OWNERS_RETRIEVED_SUCCESS
            );

        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_ROLE_OWNERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return new GetRoleOwnersResponse(
                roleId.toString(), tenantId, AppConstants.FAILED_TO_RETRIEVE_ROLE_OWNERS + ": " + e.getMessage());
        }
    }

    /**
     * Handles submit-for-approval flow: only publishes event, does not create role in database.
     * The workflow will be responsible for creating the role after approval.
     * 
     * @param request the role creation request
     * @param tenantId the tenant ID
     * @param createdByName the username of the creator
     * @param createdById the user ID of the creator
     * @return a placeholder role response indicating submission success
     */
    private RoleResponse handleSubmitForApproval(CreateRoleRequest request, String tenantId, 
                                                 String createdByName, UUID createdById) {
        logger.info()
            .message("Submit for approval: Publishing event only (no role creation)")
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
            .detail(AppConstants.DETAIL_KEY_ROLE_TYPE, request.getRoleType().name())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        // Validate required fields
        boolean hasOwners = request.getOwnerIds() != null && !request.getOwnerIds().isEmpty();
        
        if (!hasOwners) {
            throw new IllegalArgumentException("Owner IDs are required when submitting a role for approval.");
        }
        
        if (request.getWorkflowId() == null || request.getWorkflowId().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID is required when submitting a role for approval.");
        }
        
        // Validate workflow configuration
        validateWorkflowForRoleCreation(request.getWorkflowId(), request.getOwnerIds().size());
        
        // Validate role name doesn't already exist
        if (roleRepository.existsByRoleNameAndTenantId(request.getRoleName(), tenantId)) {
            throw new IllegalArgumentException(AppConstants.ROLE_NAME_ALREADY_EXISTS);
        }
        
        // Publish event to trigger workflow (NO role creation in DB)
        if (roleEventPublisher == null) {
            throw new RuntimeException("Cannot submit role for approval: Event publishing is not available.");
        }
        
        // Create temporary role object for event payload (NOT saved to DB)
        Role tempRole = new Role(
            tenantId, 
            request.getRoleType(), 
            request.getRoleName(), 
            request.getDescription(),
            request.getBusinessJustification(), 
            request.getRequireOwnerApproval(), 
            request.getTags(), 
            request.getRiskLevel(), 
            false, // Pending approval
            createdByName, 
            createdById
        );
        tempRole.setId(UUID.randomUUID()); // Temporary ID for event
        
        roleEventPublisher.publishRoleCreatedEvent(
            tempRole, 
            request.getOwnerIds(), 
            tenantId, 
            createdByName, 
            createdById,
            request.getWorkflowId()
        );
        
        logger.info()
            .message("Role submitted for approval workflow. Event published, no database record created.")
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_KEY_ROLE_NAME, request.getRoleName())
            .detail("workflowId", request.getWorkflowId())
            .log();
        
        // Return response with temporary role data (not persisted to DB)
        return convertToResponse(tempRole);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleStatsDTO getRoleStats() {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        try {
            logger.info()
                .message(AppConstants.LOG_FETCHING_ROLE_STATS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .log();

            // Get total roles count (all roles for the tenant)
            Long totalRoles = roleRepository.countByTenantId(tenantId);
            
            // Get inactive roles count (where is_active = false)
            Long inactiveRoles = roleRepository.countByIsActiveAndTenantId(false, tenantId);
            
            // Get administrator roles count
            Long activeAdmins = roleRepository.countByRoleTypeAndTenantId(
                RoleType.ADMINISTRATOR, 
                tenantId
            );

            logger.info()
                .message(AppConstants.LOG_ROLE_STATS_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("totalRoles", totalRoles)
                .detail("inactiveRoles", inactiveRoles)
                .detail("activeAdmins", activeAdmins)
                .log();

            return RoleStatsDTO.builder()
                .totalRoles(totalRoles)
                .inactiveRoles(inactiveRoles)
                .activeAdmins(activeAdmins)
                .tenantId(tenantId)
                .build();

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ROLE_STATS_FAILED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .log();
            
            throw new RuntimeException(AppConstants.ROLE_STATS_RETRIEVAL_FAILED, e);
        }
    }

    @Override
    public OwnedRolesResponse getOwnedRolesByUser(UUID userId) {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Fetching owned roles for user")
            .detail("userId", userId)
            .detail("tenantId", tenantId)
            .log();
        
        try {
            // Fetch roles owned by user
            List<Role> ownedRoles = roleRepository.findByOwnerIdAndTenantId(userId, tenantId);
            
            logger.info()
                .message("Found owned roles")
                .detail("userId", userId)
                .detail("roleCount", ownedRoles.size())
                .log();
            
            // Map to DTOs
            List<OwnedRoleDTO> roleDTOs = ownedRoles.stream().map(role -> {
                try {
                    // Fetch permissions for this role with full entity
                    List<Permission> permissions = permissionRepository.findByRoleId(role.getId());
                    
                    // Fetch all actions for these permissions in one batch
                    List<UUID> actionIds = permissions.stream()
                        .map(Permission::getActionId)
                        .distinct()
                        .collect(Collectors.toList());
                    
                    Map<UUID, Action> actionMap = new HashMap<>();
                    if (!actionIds.isEmpty()) {
                        List<Action> actions = actionRepository.findAllById(actionIds);
                        actionMap = actions.stream()
                            .collect(Collectors.toMap(Action::getId, a -> a));
                    }
                    
                    // Fetch all pages for these actions in one batch
                    List<UUID> pageIds = actionMap.values().stream()
                        .map(Action::getPageId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                    
                    Map<UUID, com.aidentx.applicationpermissions.roles_management.model.entity.Page> pageMap = new HashMap<>();
                    if (!pageIds.isEmpty()) {
                        List<com.aidentx.applicationpermissions.roles_management.model.entity.Page> pages = pageRepository.findAllById(pageIds);
                        pageMap = pages.stream()
                            .collect(Collectors.toMap(com.aidentx.applicationpermissions.roles_management.model.entity.Page::getId, p -> p));
                    }
                    
                    // Build readable permission strings: "PageName - ActionName"
                    final Map<UUID, Action> finalActionMap = actionMap;
                    final Map<UUID, com.aidentx.applicationpermissions.roles_management.model.entity.Page> finalPageMap = pageMap;
                    List<String> permissionStrings = permissions.stream()
                        .map(permission -> {
                            Action action = finalActionMap.get(permission.getActionId());
                            if (action == null) {
                                return "Unknown Action";
                            }
                            
                            String actionName = action.getActionName();
                            UUID pageId = action.getPageId();
                            
                            if (pageId != null) {
                                com.aidentx.applicationpermissions.roles_management.model.entity.Page page = finalPageMap.get(pageId);
                                if (page != null) {
                                    return page.getPageName() + " - " + actionName;
                                }
                            }
                            
                            return actionName;
                        })
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                    
                    // Map RoleType to scope
                    String scope = mapRoleTypeToScope(role.getRoleType());
                    
                    // Determine if role is critical (SYSTEM or ADMINISTRATOR with HIGH risk)
                    boolean isCritical = (role.getRoleType() == RoleType.SYSTEM || 
                                         role.getRoleType() == RoleType.ADMINISTRATOR) &&
                                         role.getRiskLevel() == RiskLevel.High;
                    
                    // Format last modified date as ISO 8601
                    String lastModified = role.getUpdatedAt() != null 
                        ? Instant.ofEpochMilli(role.getUpdatedAt()).toString() 
                        : Instant.ofEpochMilli(role.getCreatedAt()).toString();
                    
                    // Create DTO using constructor
                    OwnedRoleDTO dto = new OwnedRoleDTO();
                    dto.setId(role.getId());
                    dto.setName(role.getRoleName());
                    dto.setRoleType(role.getRoleType());
                    dto.setScope(scope);
                    dto.setPermissions(permissionStrings);
                    dto.setCritical(isCritical);
                    dto.setLastModified(lastModified);
                    dto.setRiskLevel(role.getRiskLevel() != null ? role.getRiskLevel() : RiskLevel.Medium);
                    dto.setUserCount(null); // Not available - would require expensive user role query
                    dto.setPermissionCount((long) permissionStrings.size());
                    dto.setIsActive(role.getIsActive());
                    
                    return dto;
                        
                } catch (Exception e) {
                    logger.error(e)
                        .message("Error mapping role to DTO")
                        .detail("roleId", role.getId())
                        .detail("error", e.getMessage())
                        .log();
                    return null;
                }
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
            
            logger.info()
                .message("Mapped owned roles to DTOs")
                .detail("userId", userId)
                .detail("dtoCount", roleDTOs.size())
                .log();
            
            return new OwnedRolesResponse(
                roleDTOs,
                roleDTOs.size(),
                true,
                "Successfully retrieved owned roles"
            );
                
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch owned roles")
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .detail("error", e.getMessage())
                .log();
            
            return new OwnedRolesResponse(
                Collections.emptyList(),
                0,
                false,
                "Failed to retrieve owned roles: " + e.getMessage()
            );
        }
    }

    private String mapRoleTypeToScope(RoleType roleType) {
        switch (roleType) {
            case SYSTEM:
                return "Organization";
            case ADMINISTRATOR:
                return "System";
            case REVIEWER:
            case OPERATOR:
            case VIEWER:
                return "Application";
            default:
                return "Custom";
        }
    }
}