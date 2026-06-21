package com.aidentx.iam.access_roles_management.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleCompositionConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleEntitlementConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleExportConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleRiskInsightsConstants;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleSaveSimulationRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.ManualRoleMemberGrantRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.ManageRoleCompositionsRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.ManageRoleEntitlementsRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDeleteImpactResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDetailResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDirectorySummaryResponseDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleGroupedMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberEntitlementResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMembersSummaryResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRiskInsightsResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleSaveSimulationResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.ApiResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.GroupedRoleEntitlementsDto;
import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncEntitlementsResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncManualMembersResultDto;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipDirection;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipMode;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.access_roles_management.service.AccessRoleExportService;
import com.aidentx.iam.access_roles_management.service.AccessRoleManualAssignmentService;
import com.aidentx.iam.access_roles_management.service.AccessRoleRiskInsightsService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.AccessRoleSyncService;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFilters;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFormat;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for access role management operations.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>Role CRUD operations</li>
 *   <li>Entitlement link management</li>
 *   <li>Role composition (child/parent relationship) management</li>
 *   <li>Desired-state synchronization for entitlements and child roles</li>
 *   <li>Persistent audit history retrieval</li>
 * </ul>
 *
 * <p>All responses are wrapped in {@link ApiResponse} for consistent JSON structure.</p>
 */
@RestController
@RequestMapping(AccessRoleConstants.BASE_PATH)
public class AccessRoleController {

    private final AccessRoleService roleService;
    private final AccessRoleEntitlementService entitlementService;
    private final AccessRoleCompositionService compositionService;
    private final AccessRoleSyncService syncService;
    private final AccessRoleManualAssignmentService manualAssignmentService;
    private final AccessRoleAuditLogService auditLogService;
    private final AccessRoleExportService exportService;
    private final AccessRoleRiskInsightsService riskInsightsService;
    private final IUnifiedLogger logger;

    /**
     * Constructs the controller with all required collaborators.
     *
     * @param roleService        service for role CRUD operations
     * @param entitlementService service for entitlement link operations
     * @param syncService        service for desired-state synchronization operations
     * @param compositionService service for role composition operations
     * @param auditLogService    service for persistent role audit history queries
     * @param loggerFactory      unified logger factory
     */
    public AccessRoleController(
            AccessRoleService roleService,
            AccessRoleEntitlementService entitlementService,
            AccessRoleSyncService syncService,
            AccessRoleManualAssignmentService manualAssignmentService,
            AccessRoleCompositionService compositionService,
            AccessRoleAuditLogService auditLogService,
            AccessRoleExportService exportService,
            AccessRoleRiskInsightsService riskInsightsService,
            UnifiedLoggerFactory loggerFactory) {
        this.roleService = roleService;
        this.syncService = syncService;
        this.entitlementService = entitlementService;
        this.manualAssignmentService = manualAssignmentService;
        this.compositionService = compositionService;
        this.auditLogService = auditLogService;
        this.exportService = exportService;
        this.riskInsightsService = riskInsightsService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleConstants.CONTROLLER_LOG_CATEGORY);
    }

    // ===================================================================
    //  Role CRUD Endpoints
    // ===================================================================

    /**
     * Creates a new access role.
     *
     * @param tenantId the tenant identifier from request header
     * @param request  the role creation request body
     * @return the created role wrapped in ApiResponse with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccessRoleResponse>> createRole(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody CreateAccessRoleRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_CREATE_ROLE)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId
                ))
                .log();

        AccessRoleResponse data = roleService.createRole(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_CREATED));
    }

    /**
     * Retrieves the full detail of a role including entitlements and child/parent roles.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return the detailed role response wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_ROLE_ID)
    public ResponseEntity<ApiResponse<AccessRoleDetailResponse>> getRoleDetail(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_DETAIL)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRoleDetailResponse data = roleService.getRoleDetail(tenantId, roleId);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_FETCHED));
    }

    /**
     * Retrieves active members of a role enriched with effective entitlement and path details.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param search   optional text search across member, entitlement, application, and path names
     * @param pageable pagination parameters
     * @return paginated enriched role-member rows wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_MEMBERS)
    public ResponseEntity<ApiResponse<Page<AccessRoleMemberResponse>>> getRoleMembers(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> identityTypes,
            @RequestParam(required = false) List<String> identityRiskLevels,
            @RequestParam(required = false) List<String> accessPathTypes,
            @RequestParam(required = false) List<String> assignmentSourceTypes,
            @RequestParam(required = false) List<UUID> applicationIds,
            @RequestParam(required = false) List<UUID> entitlementIds,
            Pageable pageable) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_MEMBERS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.LOG_DETAIL_SEARCH, search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(pageable.getPageNumber()),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(pageable.getPageSize())
                ))
                .log();

        Page<AccessRoleMemberResponse> data = roleService.getRoleMembers(
                tenantId,
                roleId,
                search,
                identityTypes,
                identityRiskLevels,
                accessPathTypes,
                assignmentSourceTypes,
                applicationIds,
                entitlementIds,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_MEMBERS_FETCHED));
    }

    /**
     * Retrieves role member count and assignment-mode summary.
     *
     * @param tenantId the tenant identifier
     * @param roleId             the role ID
     * @param activityWindowDays optional lookback window in days; defaults to 90
     * @return role member summary wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_MEMBERS_SUMMARY)
    public ResponseEntity<ApiResponse<AccessRoleMembersSummaryResponse>> getRoleMembersSummary(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = AccessRoleConstants.DEFAULT_ACTIVITY_WINDOW_DAYS_PARAM) Integer activityWindowDays) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_MEMBERS_SUMMARY)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        "activityWindowDays", String.valueOf(activityWindowDays)
                ))
                .log();

        AccessRoleMembersSummaryResponse data = roleService.getRoleMembersSummary(tenantId, roleId, activityWindowDays);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_MEMBERS_SUMMARY_FETCHED));
    }

    /**
     * Retrieves active members of a role as lightweight member summaries.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param search   optional text search across member or employee name
     * @param pageable pagination parameters applied at member level
     * @return paginated grouped role members wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_MEMBERS_GROUPED)
    public ResponseEntity<ApiResponse<Page<AccessRoleGroupedMemberResponse>>> getRoleMembersGrouped(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_MEMBERS_GROUPED)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.LOG_DETAIL_SEARCH, search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(pageable.getPageNumber()),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(pageable.getPageSize())
                ))
                .log();

        Page<AccessRoleGroupedMemberResponse> data = roleService.getRoleMembersGrouped(
                tenantId,
                roleId,
                search,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_MEMBERS_GROUPED_FETCHED));
    }

    /**
     * Retrieves effective entitlements for one active role-member assignment.
     *
     * @param tenantId        the tenant identifier
     * @param roleId          the role ID
     * @param assignmentId    the assignment ID representing one member of the role
     * @param search          optional text search across entitlement, application, and child-role names
     * @param accessPathTypes optional access-path filters
     * @param applicationIds  optional application filters
     * @param entitlementIds  optional entitlement filters
     * @param pageable        pagination parameters applied to entitlement rows
     * @return paginated member entitlement rows wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_MEMBER_ENTITLEMENTS)
    public ResponseEntity<ApiResponse<Page<AccessRoleMemberEntitlementResponse>>> getRoleMemberEntitlements(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> accessPathTypes,
            @RequestParam(required = false) List<UUID> applicationIds,
            @RequestParam(required = false) List<UUID> entitlementIds,
            Pageable pageable) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_MEMBER_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.DETAIL_ASSIGNMENT_ID, assignmentId.toString(),
                        AccessRoleConstants.LOG_DETAIL_SEARCH, search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(pageable.getPageNumber()),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(pageable.getPageSize())
                ))
                .log();

        Page<AccessRoleMemberEntitlementResponse> data = roleService.getRoleMemberEntitlements(
                tenantId,
                roleId,
                assignmentId,
                search,
                accessPathTypes,
                applicationIds,
                entitlementIds,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_MEMBER_ENTITLEMENTS_FETCHED));
    }

    /**
     * Grants a role manually to multiple identities in one request.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  bulk identity IDs to grant manually
     * @return added/skipped/failed identity results wrapped in ApiResponse
     */
    @PostMapping(AccessRoleConstants.PATH_MEMBERS_MANUAL)
    public ResponseEntity<ApiResponse<ManualRoleMemberGrantResultDto>> manualGrantRoleMembers(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(value = AccessRoleConstants.HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = AccessRoleConstants.HEADER_USERNAME, required = false) String userName,
            @RequestHeader(value = AccessRoleConstants.HEADER_USER, required = false) String gatewayUser,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManualRoleMemberGrantRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_MANUAL_GRANT_ROLE_MEMBERS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.DETAIL_COUNT, String.valueOf(request.getIdentityIds() != null ? request.getIdentityIds().size() : 0)
                ))
                .log();

        ManualRoleMemberGrantResultDto result = manualAssignmentService.grantRoleToMembers(
                tenantId,
                roleId,
                request.getIdentityIds(),
                userId,
                firstNonBlank(userName, gatewayUser, userId));

        if (result.hasFailures()) {
            return ResponseEntity.status(207)
                    .body(ApiResponse.success(
                            result,
                            AccessRoleConstants.RESPONSE_ROLE_MEMBERS_MANUAL_GRANTED_PARTIAL));
        }

        return ResponseEntity.ok(ApiResponse.success(
                result,
                AccessRoleConstants.RESPONSE_ROLE_MEMBERS_MANUAL_GRANTED));
    }

    /**
     * Synchronizes manual member assignments for a role to the desired state.
     *
     * <p>The request body contains the complete list of identity IDs that should
     * hold MANUAL assignments after this operation. The backend computes additions
     * and revocations, and logs entitlement impact for each identity.</p>
     *
     * <p>Returns 200 if all identities were processed successfully.
     * Returns 207 (Multi-Status) if some identities failed.</p>
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  the desired identity IDs
     * @return sync result with added, revoked, skipped, failed members and entitlement impact
     */
    @PutMapping(AccessRoleConstants.PATH_MEMBERS_MANUAL)
    public ResponseEntity<ApiResponse<SyncManualMembersResultDto>> syncManualMembers(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(value = AccessRoleConstants.HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = AccessRoleConstants.HEADER_USERNAME, required = false) String userName,
            @RequestHeader(value = AccessRoleConstants.HEADER_USER, required = false) String gatewayUser,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManualRoleMemberGrantRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_SYNC_MANUAL_MEMBERS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.DETAIL_COUNT, String.valueOf(
                                request.getIdentityIds() != null ? request.getIdentityIds().size() : 0)
                ))
                .log();

        SyncManualMembersResultDto result = manualAssignmentService.syncManualMembers(
                tenantId,
                roleId,
                request.getIdentityIds(),
                userId,
                firstNonBlank(userName, gatewayUser, userId));

        if (result.hasFailures()) {
            return ResponseEntity.status(207)
                    .body(ApiResponse.success(
                            result,
                            AccessRoleConstants.RESPONSE_ROLE_MEMBERS_MANUAL_SYNCED_PARTIAL));
        }

        return ResponseEntity.ok(ApiResponse.success(
                result,
                AccessRoleConstants.RESPONSE_ROLE_MEMBERS_MANUAL_SYNCED));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Lists roles for a tenant with optional filtering and pagination.
     *
     * <p>This endpoint supports generic listing as well as directory-node loading
     * by combining optional filters such as role type, role status, business unit,
     * risk level, and free-text search.</p>
     *
     * @param tenantId      the tenant identifier
     * @param types         optional role type filters
     * @param statuses      optional role status filters
     * @param businessUnits optional business-unit filters
     * @param riskLevels    optional risk-level filters
     * @param search        optional case-insensitive search text matched against name, display name, and description
     * @param pageable      pagination parameters
     * @return a page of role responses wrapped in ApiResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccessRoleResponse>>> listRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> businessUnits,
            @RequestParam(required = false) List<String> riskLevels,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_LIST_ROLES)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_TYPE,
                        types != null && !types.isEmpty() ? String.join(",", types) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_STATUS,
                        statuses != null && !statuses.isEmpty() ? String.join(",", statuses) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_BUSINESS_UNIT,
                        businessUnits != null && !businessUnits.isEmpty() ? String.join(",", businessUnits) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_RISK_LEVEL,
                        riskLevels != null && !riskLevels.isEmpty() ? String.join(",", riskLevels) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_SEARCH,
                        search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(pageable.getPageNumber()),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(pageable.getPageSize())
                ))
                .log();

        Page<AccessRoleResponse> data = roleService.listRoles(
                tenantId,
                types,
                statuses,
                businessUnits,
                riskLevels,
                search,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLES_LISTED));
    }

    /**
     * Updates governance metadata on an existing role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  the update request body
     * @return the updated role response wrapped in ApiResponse
     */
    @PutMapping(AccessRoleConstants.PATH_ROLE_ID)
    public ResponseEntity<ApiResponse<AccessRoleResponse>> updateRole(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateAccessRoleRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_UPDATE_ROLE)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRoleResponse data = roleService.updateRole(tenantId, roleId, request);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_UPDATED));
    }

    /**
     * Deletes a role and all its associated links.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return success message wrapped in ApiResponse with HTTP 200
     */
    @DeleteMapping(AccessRoleConstants.PATH_ROLE_ID)
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_DELETE_ROLE)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        roleService.deleteRole(tenantId, roleId);
        return ResponseEntity.ok(ApiResponse.success(AccessRoleConstants.RESPONSE_ROLE_DELETED));
    }

    /**
     * Returns counts-only impact preview for deleting one access role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return delete impact counts wrapped in ApiResponse with HTTP 200
     */
    @GetMapping(AccessRoleConstants.PATH_DELETE_IMPACT)
    public ResponseEntity<ApiResponse<AccessRoleDeleteImpactResponse>> getDeleteImpact(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_DELETE_IMPACT)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRoleDeleteImpactResponse data = roleService.getDeleteImpact(tenantId, roleId);
        return ResponseEntity.ok(ApiResponse.success(
                data,
                AccessRoleConstants.RESPONSE_ROLE_DELETE_IMPACT_FETCHED));
    }

    /**
     * Simulates the identity and entitlement impact of saving proposed role edits.
     *
     * @param tenantId         the tenant identifier
     * @param roleId           the role being edited
     * @param request          proposed role desired state
     * @param includeUnchanged whether unchanged entitlement rows should be returned
     * @return read-only save impact simulation wrapped in ApiResponse
     */
    @PostMapping(AccessRoleConstants.PATH_IMPACT_SIMULATION)
    public ResponseEntity<ApiResponse<AccessRoleSaveSimulationResponse>> simulateRoleSaveImpact(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody AccessRoleSaveSimulationRequest request,
            @RequestParam(defaultValue = "false") boolean includeUnchanged) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_SIMULATE_ROLE_IMPACT)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        "includeUnchanged", String.valueOf(includeUnchanged)
                ))
                .log();

        AccessRoleSaveSimulationResponse data = roleService.simulateRoleSaveImpact(
                tenantId,
                roleId,
                request,
                includeUnchanged);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_IMPACT_SIMULATED));
    }

    /**
     * Returns a lightweight directory summary grouped by business unit and role type.
     *
     * <p>This endpoint is intended for the role navigator and returns only
     * business units, type buckets, and counts.</p>
     *
     * @param tenantId   the tenant identifier
     * @param statuses   optional role status filters
     * @param riskLevels optional risk-level filters
     * @param search     optional case-insensitive search text matched against name, display name, and description
     * @return grouped directory summary wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_DIRECTORY_SUMMARY)
    public ResponseEntity<ApiResponse<AccessRoleDirectorySummaryResponseDto>> getRoleDirectorySummary(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> riskLevels,
            @RequestParam(required = false) String search) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ROLE_DIRECTORY_SUMMARY)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_STATUS,
                        statuses != null && !statuses.isEmpty() ? String.join(",", statuses) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_RISK_LEVEL,
                        riskLevels != null && !riskLevels.isEmpty() ? String.join(",", riskLevels) : AccessRoleConstants.DEFAULT_FILTER_ALL,
                        AccessRoleConstants.LOG_DETAIL_SEARCH,
                        search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL
                ))
                .log();

        AccessRoleDirectorySummaryResponseDto data = roleService.getRoleDirectorySummary(
                tenantId,
                statuses,
                riskLevels,
                search
        );

        return ResponseEntity.ok(
                ApiResponse.success(data, AccessRoleConstants.RESPONSE_ROLE_DIRECTORY_SUMMARY_FETCHED)
        );
    }

    // ===================================================================
    //  Entitlement Link Endpoints
    // ===================================================================

    /**
     * Adds entitlements to a role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  the entitlement IDs to add
     * @return success message wrapped in ApiResponse
     */
    @PostMapping(AccessRoleConstants.PATH_ENTITLEMENTS)
    public ResponseEntity<ApiResponse<Void>> addEntitlements(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleEntitlementsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_ADD_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        entitlementService.addEntitlements(tenantId, roleId, request.getEntitlementIds());
        return ResponseEntity.ok(ApiResponse.success(AccessRoleEntitlementConstants.RESPONSE_ENTITLEMENTS_ADDED));
    }

    /**
     * Removes entitlements from a role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  the entitlement IDs to remove
     * @return success message wrapped in ApiResponse
     */
    @DeleteMapping(AccessRoleConstants.PATH_ENTITLEMENTS)
    public ResponseEntity<ApiResponse<Void>> removeEntitlements(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleEntitlementsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_REMOVE_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        entitlementService.removeEntitlements(tenantId, roleId, request.getEntitlementIds());
        return ResponseEntity.ok(ApiResponse.success(AccessRoleEntitlementConstants.RESPONSE_ENTITLEMENTS_REMOVED));
    }

    /**
     * Retrieves paginated entitlements linked to a role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param page     page number (zero-based, default 0)
     * @param size     page size (default 10)
     * @return paginated entitlement details wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_ENTITLEMENTS)
    public ResponseEntity<ApiResponse<Page<EntitlementDtos.EntitlementResponseDto>>> getEntitlementDetails(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(page),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(size)
                ))
                .log();

        Pageable pageable = PageRequest.of(page, size);
        Page<EntitlementDtos.EntitlementResponseDto> data =
                entitlementService.getEntitlementDetails(roleId, tenantId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(data, AccessRoleEntitlementConstants.RESPONSE_ENTITLEMENTS_FETCHED));
    }

    /**
     * Retrieves role entitlements grouped by application with two-level pagination.
     *
     * <p>App-level pagination controls which applications are visible.
     * Entitlement-level pagination controls how many entitlements per
     * application are returned.</p>
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param appPage  application page number (zero-based, default 0)
     * @param appSize  applications per page (default 5)
     * @param entPage  entitlement page number within each app (zero-based, default 0)
     * @param entSize  entitlements per application per page (default 10)
     * @param search   optional search across application name and entitlement fields
     * @return grouped entitlement details with nested pagination
     */
    @GetMapping(AccessRoleConstants.PATH_ENTITLEMENTS + "/grouped")
    public ResponseEntity<ApiResponse<GroupedRoleEntitlementsDto>> getGroupedEntitlementDetails(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = "0") int appPage,
            @RequestParam(defaultValue = "10") int appSize,
            @RequestParam(defaultValue = "0") int entPage,
            @RequestParam(defaultValue = "10") int entSize,
            @RequestParam(required = false) String search) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_GROUPED_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.LOG_DETAIL_SEARCH,
                        search != null ? search : AccessRoleConstants.DEFAULT_FILTER_ALL
                ))
                .log();

        Pageable appPageable = PageRequest.of(appPage, appSize);
        Pageable entPageable = PageRequest.of(entPage, entSize);

        GroupedRoleEntitlementsDto data = entitlementService.getGroupedEntitlementDetails(
                roleId, tenantId, appPageable, entPageable, search);

        return ResponseEntity.ok(
                ApiResponse.success(data, AccessRoleEntitlementConstants.RESPONSE_GROUPED_ENTITLEMENTS_FETCHED));
    }

    /**
     * Retrieves direct and inherited entitlement distribution for a role.
     *
     * <p>Counts are unique by entitlement ID by default. Direct counts include
     * entitlements attached to this role. Inherited counts include entitlements
     * contributed through child/source roles.</p>
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return role entitlement distribution summary wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_ENTITLEMENT_DISTRIBUTION)
    public ResponseEntity<ApiResponse<AccessRoleEntitlementDistributionResponse>> getEntitlementDistribution(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_ENTITLEMENT_DISTRIBUTION)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRoleEntitlementDistributionResponse data =
                entitlementService.getEntitlementDistribution(tenantId, roleId);

        return ResponseEntity.ok(ApiResponse.success(
                data,
                AccessRoleConstants.RESPONSE_ROLE_ENTITLEMENT_DISTRIBUTION_FETCHED));
    }

    /**
     * Compares overlapping direct/inherited effective entitlements between two roles.
     *
     * @param tenantId     the tenant identifier
     * @param roleAId      first role ID
     * @param roleBId      second role ID
     * @param sourceFilter optional filter: ALL, DIRECT, or INHERITED
     * @param page         page number (zero-based, default 0)
     * @param size         page size (default 20)
     * @return paginated overlapping entitlement comparison wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_COMPARE_ENTITLEMENTS)
    public ResponseEntity<ApiResponse<AccessRoleEntitlementComparisonResponse>> compareRoleEntitlements(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam UUID roleAId,
            @RequestParam UUID roleBId,
            @RequestParam(defaultValue = "ALL") String sourceFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_COMPARE_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        "roleAId", roleAId.toString(),
                        "roleBId", roleBId.toString(),
                        "sourceFilter", sourceFilter,
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(page),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(size)
                ))
                .log();

        AccessRoleEntitlementComparisonResponse data = entitlementService.compareRoleEntitlements(
                tenantId,
                roleAId,
                roleBId,
                sourceFilter,
                PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(
                data,
                AccessRoleConstants.RESPONSE_ROLE_ENTITLEMENT_COMPARISON_FETCHED));
    }

    // ===================================================================
    //  Composition Endpoints
    // ===================================================================

    /**
     * Adds child roles to a parent role with partial success support.
     *
     * <p>Returns 200 if all children were added or skipped successfully.
     * Returns 207 (Multi-Status) if some children failed validation while
     * others succeeded.</p>
     *
     * @param tenantId the tenant identifier
     * @param roleId   the parent role ID
     * @param request  the child role IDs to add
     * @return result with added, skipped, and failed lists wrapped in ApiResponse
     */
    @PostMapping(AccessRoleConstants.PATH_COMPOSITIONS)
    public ResponseEntity<ApiResponse<AddChildRolesResultDto>> addChildRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleCompositionsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_ADD_CHILDREN)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AddChildRolesResultDto result = compositionService.addChildRoles(
                tenantId, roleId, request.getChildRoleIds());

        if (result.hasFailures()) {
            return ResponseEntity.status(207)
                    .body(ApiResponse.success(result, AccessRoleCompositionConstants.RESPONSE_CHILDREN_PARTIAL));
        }

        return ResponseEntity.ok(
                ApiResponse.success(result, AccessRoleCompositionConstants.RESPONSE_CHILDREN_ADDED));
    }

    /**
     * Removes child roles from a parent role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the parent role ID
     * @param request  the child role IDs to remove
     * @return success message wrapped in ApiResponse
     */
    @DeleteMapping(AccessRoleConstants.PATH_COMPOSITIONS)
    public ResponseEntity<ApiResponse<Void>> removeChildRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleCompositionsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_REMOVE_CHILDREN)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        compositionService.removeChildRoles(tenantId, roleId, request.getChildRoleIds());
        return ResponseEntity.ok(ApiResponse.success(AccessRoleCompositionConstants.RESPONSE_CHILDREN_REMOVED));
    }

    /**
     * Retrieves all child roles of a parent role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the parent role ID
     * @return list of child role responses wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_COMPOSITIONS)
    public ResponseEntity<ApiResponse<List<AccessRoleResponse>>> getChildRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_CHILDREN)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        List<AccessRoleResponse> data = compositionService.getChildRoles(roleId);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleCompositionConstants.RESPONSE_CHILDREN_FETCHED));
    }

    /**
     * Retrieves all parent roles of a given role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the child role ID
     * @return list of parent role responses wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_PARENTS)
    public ResponseEntity<ApiResponse<List<AccessRoleResponse>>> getParentRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_PARENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        List<AccessRoleResponse> data = compositionService.getParentRoles(roleId);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleCompositionConstants.RESPONSE_PARENTS_FETCHED));
    }

    /**
     * Retrieves a unified role relationship view for direct, hierarchy, or conflict mode.
     *
     * @param tenantId  the tenant identifier
     * @param roleId    root role ID
     * @param mode      DIRECT, HIERARCHY, or CONFLICT
     * @param direction BOTH, CHILDREN, or PARENTS
     * @return unified relationship response wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_RELATIONSHIPS)
    public ResponseEntity<ApiResponse<AccessRoleRelationshipResponse>> getUnifiedRelationships(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = "DIRECT") String mode,
            @RequestParam(defaultValue = "BOTH") String direction) {

        RoleRelationshipMode relationshipMode = RoleRelationshipMode.from(mode);
        RoleRelationshipDirection relationshipDirection = RoleRelationshipDirection.from(direction);
        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_RELATIONSHIPS_FETCHED)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleCompositionConstants.DETAIL_MODE, relationshipMode.name(),
                        AccessRoleCompositionConstants.DETAIL_DIRECTION, relationshipDirection.name()
                ))
                .log();

        AccessRoleRelationshipResponse data = compositionService.getUnifiedRelationships(
                tenantId,
                roleId,
                relationshipMode,
                relationshipDirection);
        return ResponseEntity.ok(ApiResponse.success(data, AccessRoleCompositionConstants.RESPONSE_RELATIONSHIPS_FETCHED));
    }

    // ===================================================================
    //  Sync Endpoints — Desired-State PUT for sub-resources
    // ===================================================================

    /**
     * Synchronizes a role's entitlement assignments to the desired state.
     *
     * <p>The request body contains the complete list of entitlement IDs the role
     * should have after this operation. The backend computes additions and removals.</p>
     *
     * @param tenantId the tenant identifier from the request header
     * @param roleId   the role whose entitlements to sync
     * @param request  the desired entitlement IDs
     * @return sync result wrapped in ApiResponse
     */
    @PutMapping("/{roleId}/entitlements")
    public ResponseEntity<ApiResponse<SyncEntitlementsResultDto>> syncEntitlements(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleEntitlementsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_SYNC_ENTITLEMENTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        SyncEntitlementsResultDto result = syncService.syncEntitlements(
                tenantId, roleId, request.getEntitlementIds());

        return ResponseEntity.ok(
                ApiResponse.success(result, AccessRoleConstants.RESPONSE_ENTITLEMENTS_SYNCED));
    }

    /**
     * Synchronizes a role's child role composition to the desired state.
     *
     * <p>The request body contains the complete list of child role IDs the parent role
     * should have. The backend computes the diff and applies changes.</p>
     *
     * @param tenantId the tenant identifier from the request header
     * @param roleId   the parent role whose children to sync
     * @param request  the desired child role IDs
     * @return sync result wrapped in ApiResponse
     */
    @PutMapping("/{roleId}/compositions")
    public ResponseEntity<ApiResponse<SyncChildRolesResultDto>> syncChildRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody ManageRoleCompositionsRequest request) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_SYNC_CHILDREN)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        SyncChildRolesResultDto result = syncService.syncChildRoles(
                tenantId, roleId, request.getChildRoleIds());

        if (result.hasFailures()) {
            return ResponseEntity.status(207).body(
                    ApiResponse.success(result, AccessRoleConstants.RESPONSE_CHILDREN_PARTIAL));
        }

        return ResponseEntity.ok(
                ApiResponse.success(result, AccessRoleConstants.RESPONSE_CHILDREN_SYNCED));
    }

    // ===================================================================
    //  Audit History Endpoints
    // ===================================================================

    /**
     * Retrieves persistent audit history for a specific access role.
     *
     * <p>Returns DB-backed role audit records for create, update, delete,
     * entitlement sync, and child composition sync operations. Results are
     * tenant-scoped, paginated, and optionally filterable by action.</p>
     *
     * <p>The response exposes user-friendly activity labels such as:</p>
     * <ul>
     *   <li>Business Role Created</li>
     *   <li>Technical Role Updated</li>
     *   <li>Business Role Entitlements Updated</li>
     * </ul>
     *
     * @param tenantId the tenant identifier from request header
     * @param roleId   the target role UUID
     * @param action   optional raw action filter (e.g. ROLE_UPDATED, CHILDREN_UPDATED)
     * @param search   optional search across audit actor username/id
     * @param pageable pagination and sorting information
     * @return page of UI-friendly audit history entries wrapped in ApiResponse
     */
    @GetMapping(AccessRoleConstants.PATH_AUDIT_HISTORY)
    public ResponseEntity<ApiResponse<Page<AccessRoleAuditHistoryDto>>> getAuditHistory(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_CONTROLLER_GET_AUDIT_HISTORY)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString(),
                        AccessRoleConstants.LOG_DETAIL_ACTION, action != null ? action : "ALL",
                        AccessRoleConstants.LOG_DETAIL_SEARCH, search != null ? search : "ALL",
                        AccessRoleConstants.LOG_DETAIL_PAGE, String.valueOf(pageable.getPageNumber()),
                        AccessRoleConstants.LOG_DETAIL_SIZE, String.valueOf(pageable.getPageSize())
                ))
                .log();

        Page<AccessRoleAuditHistoryDto> data =
                auditLogService.findAuditHistory(tenantId, roleId, action, search, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(data, AccessRoleConstants.RESPONSE_AUDIT_HISTORY_FETCHED));
    }

    // ===================================================================
    //  Risk Insights Endpoint
    // ===================================================================

    /**
     * Returns risk insights for a single access role: high-risk and admin
     * entitlement counts, overlap conflicts inside the role's child tree,
     * per-depth overlap distribution, and a simple {@code riskScore}.
     *
     * @param tenantId the tenant identifier from request header
     * @param roleId   the target role ID
     * @return risk insights wrapped in {@link ApiResponse}
     */
    @GetMapping(AccessRoleRiskInsightsConstants.PATH_RISK_INSIGHTS)
    public ResponseEntity<ApiResponse<AccessRoleRiskInsightsResponse>> getRiskInsights(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleRiskInsightsConstants.LOG_CONTROLLER_GET_INSIGHTS)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRoleRiskInsightsResponse data = riskInsightsService.getRiskInsights(tenantId, roleId);
        return ResponseEntity.ok(ApiResponse.success(
                data, AccessRoleRiskInsightsConstants.RESPONSE_INSIGHTS_FETCHED));
    }

    // ===================================================================
    //  Export Endpoint
    // ===================================================================

    /**
     * Streams the tenant's access-role catalog as CSV or JSON.
     *
     * <p>Filter parameters mirror {@link #listRoles}. The response is streamed
     * via {@link StreamingResponseBody} so memory use stays bounded regardless
     * of tenant size. If the soft row cap is hit the response includes the
     * {@code X-Export-Truncated} header.</p>
     *
     * @param tenantId      the tenant identifier
     * @param format        export format token ({@code csv} or {@code json}); defaults to {@code csv}
     * @param columns       optional comma-separated subset of column keys; defaults to all
     * @param types         optional role type filters
     * @param statuses      optional role status filters
     * @param businessUnits optional business-unit filters
     * @param riskLevels    optional risk-level filters
     * @param search        optional case-insensitive search text
     * @return streaming response with attachment headers
     */
    @GetMapping(AccessRoleExportConstants.PATH_EXPORT)
    public ResponseEntity<StreamingResponseBody> exportRoles(
            @RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(name = AccessRoleExportConstants.PARAM_FORMAT,
                    defaultValue = AccessRoleExportConstants.FORMAT_CSV) String format,
            @RequestParam(name = AccessRoleExportConstants.PARAM_COLUMNS, required = false) List<String> columns,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> businessUnits,
            @RequestParam(required = false) List<String> riskLevels,
            @RequestParam(required = false) String search) {

        AccessRoleExportFormat exportFormat = AccessRoleExportFormat.fromToken(format)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(AccessRoleExportConstants.ERROR_UNSUPPORTED_FORMAT, format)));

        logger.info()
                .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                .message(AccessRoleExportConstants.LOG_CONTROLLER_EXPORT)
                .details(Map.of(
                        AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                        AccessRoleExportConstants.DETAIL_FORMAT, exportFormat.token()
                ))
                .log();

        AccessRoleExportFilters filters =
                new AccessRoleExportFilters(types, statuses, businessUnits, riskLevels, search);

        String filename = buildExportFilename(tenantId, exportFormat);

        StreamingResponseBody body = outputStream -> {
            AccessRoleExportService.ExportResult result =
                    exportService.streamExport(tenantId, filters, exportFormat, columns, outputStream);
            if (result.truncated()) {
                logger.warn()
                        .category(AccessRoleConstants.CONTROLLER_LOG_CATEGORY)
                        .message(AccessRoleExportConstants.LOG_EXPORT_TRUNCATED)
                        .details(Map.of(
                                AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                                AccessRoleExportConstants.DETAIL_ROW_COUNT, String.valueOf(result.rowCount())
                        ))
                        .log();
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportFormat.mimeType()))
                .header(AccessRoleExportConstants.HEADER_CONTENT_DISPOSITION,
                        String.format(AccessRoleExportConstants.CONTENT_DISPOSITION_TEMPLATE, filename))
                .body(body);
    }

    private static String buildExportFilename(String tenantId, AccessRoleExportFormat format) {
        String timestamp = DateTimeFormatter
                .ofPattern(AccessRoleExportConstants.FILENAME_TIMESTAMP_PATTERN)
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String safeTenant = tenantId == null ? "tenant" : tenantId.replaceAll("[^A-Za-z0-9_-]", "_");
        return String.format(AccessRoleExportConstants.FILENAME_TEMPLATE,
                safeTenant, timestamp, format.extension());
    }
}

