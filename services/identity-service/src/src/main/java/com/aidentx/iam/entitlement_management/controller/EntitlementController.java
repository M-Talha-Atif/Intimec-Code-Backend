package com.aidentx.iam.entitlement_management.controller;

import com.aidentx.iam.access_roles_management.model.dto.request.ManageEntitlementRolesRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleAssignmentResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleResponse;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementMemberDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementUpdateDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.FilterOptionDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentResponse;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ShadowEntitlementDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersResponse;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementMemberConstants;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import com.aidentx.iam.entitlement_management.service.EntitlementAuditLogService;
import com.aidentx.iam.entitlement_management.service.EntitlementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementAuditHistoryDto;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementController {

    private final EntitlementService entitlementService;
    private final EntitlementAuditLogService entitlementAuditLogService;
    private final AccessRoleEntitlementService accessRoleEntitlementService;

    public EntitlementController(EntitlementService entitlementService,
                                 EntitlementAuditLogService entitlementAuditLogService,
                                 AccessRoleEntitlementService accessRoleEntitlementService) {
        this.entitlementService = entitlementService;
        this.entitlementAuditLogService = entitlementAuditLogService;
        this.accessRoleEntitlementService = accessRoleEntitlementService;
    }

    @GetMapping
    public ResponseEntity<Page<EntitlementResponseDto>> getAllEntitlements(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Boolean classified,
            @RequestParam(required = false) Boolean requestable,
            @RequestParam(required = false) Boolean hasOwner,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) String assignmentSource,
            @RequestParam(required = false) Long minAssignedUsers,
            @RequestParam(required = false) Long maxAssignedUsers,
            Pageable pageable) {

        return ResponseEntity.ok(entitlementService.findAll(
                tenantId,
                resourceId,
                search,
                riskLevel,
                lifecycle,
                kind,
                classified,
                requestable,
                hasOwner,
                ownerId,
                assignmentSource,
                minAssignedUsers,
                maxAssignedUsers,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntitlementResponseDto> getEntitlementById(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return entitlementService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates governance metadata for an entitlement (lifecycle, riskLevel, requestable, etc.).
     * Sync-imported fields (sourceUid, attributes) are never overwritten by this endpoint.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EntitlementResponseDto> updateGovernance(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody EntitlementUpdateDto updateDto) {
        EntitlementResponseDto updated = entitlementService.updateGovernance(id, tenantId, updateDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves entitlements for a specific application (resource) with basic pagination.
     */
    @GetMapping("/by-resource/{resourceId}")
    public ResponseEntity<Page<EntitlementResponseDto>> getEntitlementsByResource(
            @PathVariable UUID resourceId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementService.findByResourceId(resourceId, tenantId, pageable));
    }

    /**
     * Searches entitlements for a resource with optional text search and filters.
     * Supports filtering by riskLevel, lifecycle, and kind (type).
     */
    @GetMapping("/by-resource/{resourceId}/search")
    public ResponseEntity<Page<EntitlementResponseDto>> searchEntitlementsByResource(
            @PathVariable UUID resourceId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String kind,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementService.searchByResource(
                resourceId, tenantId, search, riskLevel, lifecycle, kind, pageable));
    }

    /**
     * Returns available filter options (risk level, lifecycle, type) for the entitlement catalog
     * of a specific application. Values are derived from actual data.
     */
    @GetMapping("/by-resource/{resourceId}/filters")
    public ResponseEntity<List<FilterOptionDto>> getFilterOptions(
            @PathVariable UUID resourceId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(entitlementService.getFilterOptions(resourceId, tenantId));
    }

    /**
     * Retrieves the members (accounts/identities) assigned to a specific entitlement.
     */
    @GetMapping(EntitlementMemberConstants.PATH_ENTITLEMENT_MEMBERS)
    public ResponseEntity<Page<EntitlementMemberDto>> getEntitlementMembers(
            @PathVariable UUID entitlementId,
            @RequestHeader(EntitlementMemberConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementService.getEntitlementMembers(entitlementId, tenantId, search, pageable));
    }

    /**
     * Manually assigns this entitlement to one or more identities.
     *
     * <p>Manual assignments are stored as canonical {@code DIRECT} assignments.
     * If an identity has an eligible account shadow on the entitlement's target
     * application, the assignment is created as {@code ASSIGNED}; otherwise an
     * identity-scoped {@code PENDING} assignment is created and remains until the
     * identity gets an account.</p>
     */
    @PostMapping(EntitlementMemberConstants.PATH_ENTITLEMENT_MEMBERS)
    public ResponseEntity<ManualEntitlementAssignmentResponse> assignEntitlementMembers(
            @PathVariable UUID entitlementId,
            @RequestHeader(EntitlementMemberConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USERNAME, required = false) String userName,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USER, required = false) String gatewayUser,
            @Valid @RequestBody ManualEntitlementAssignmentRequest request) {
        return ResponseEntity.ok(entitlementService.assignEntitlementMembers(
                entitlementId, tenantId, request, userId, firstNonBlank(userName, gatewayUser, userId)));
    }

    /**
     * Synchronizes standalone direct entitlement members to a complete desired state.
     *
     * <p>Identities in the request are assigned when missing. Current standalone
     * {@code DIRECT} members omitted from the request are revoked. Rule-created
     * {@code DYNAMIC} assignments and role-derived direct assignments remain
     * controlled by their owning rule/role flows.</p>
     */
    @PutMapping(EntitlementMemberConstants.PATH_ENTITLEMENT_MEMBERS_MANUAL)
    public ResponseEntity<SyncManualEntitlementMembersResponse> syncManualEntitlementMembers(
            @PathVariable UUID entitlementId,
            @RequestHeader(EntitlementMemberConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USERNAME, required = false) String userName,
            @RequestHeader(value = EntitlementMemberConstants.HEADER_USER, required = false) String gatewayUser,
            @Valid @RequestBody SyncManualEntitlementMembersRequest request) {
        SyncManualEntitlementMembersResponse response = entitlementService.syncManualEntitlementMembers(
                entitlementId, tenantId, request, userId, firstNonBlank(userName, gatewayUser, userId));
        if (response.hasFailures()) {
            return ResponseEntity.status(207).body(response);
        }
        return ResponseEntity.ok(response);
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
     * Retrieves access roles that contain a specific entitlement.
     */
    @GetMapping("/{entitlementId}/roles")
    public ResponseEntity<Page<EntitlementRoleResponse>> getEntitlementRoles(
            @PathVariable UUID entitlementId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(accessRoleEntitlementService.getRolesForEntitlement(tenantId, entitlementId, pageable));
    }

    /**
     * Links this entitlement to multiple roles. Existing links are skipped safely.
     */
    @PostMapping("/{entitlementId}/roles")
    public ResponseEntity<EntitlementRoleAssignmentResponse> addRolesToEntitlement(
            @PathVariable UUID entitlementId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ManageEntitlementRolesRequest request) {
        return ResponseEntity.ok(accessRoleEntitlementService.addRolesToEntitlement(
                tenantId, entitlementId, request.getRoleIds()));
    }

    /**
     * Unlinks this entitlement from multiple roles. Missing links are skipped safely.
     */
    @DeleteMapping("/{entitlementId}/roles")
    public ResponseEntity<EntitlementRoleAssignmentResponse> removeRolesFromEntitlement(
            @PathVariable UUID entitlementId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ManageEntitlementRolesRequest request) {
        return ResponseEntity.ok(accessRoleEntitlementService.removeRolesFromEntitlement(
                tenantId, entitlementId, request.getRoleIds()));
    }

    /**
     * Replaces this entitlement's role links with exactly the supplied role IDs.
     */
    @PutMapping("/{entitlementId}/roles")
    public ResponseEntity<EntitlementRoleAssignmentResponse> syncRolesForEntitlement(
            @PathVariable UUID entitlementId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ManageEntitlementRolesRequest request) {
        return ResponseEntity.ok(accessRoleEntitlementService.syncRolesForEntitlement(
                tenantId, entitlementId, request.getRoleIds()));
    }

    /**
     * Retrieves entitlements assigned to a specific shadow (account).
     */
    @GetMapping("/by-shadow/{shadowId}")
    public ResponseEntity<Page<ShadowEntitlementDto>> getShadowEntitlements(
            @PathVariable UUID shadowId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementService.getShadowEntitlements(shadowId, tenantId, pageable));
    }
    
    // ===================================================================
    //  Audit History Endpoints
    // ===================================================================


    /**
     * Retrieves audit history filtered by action type across all entitlements in the tenant.
     *
     * @param tenantId the tenant identifier
     * @param action   the action type (GOVERNANCE_UPDATED, RULE_CREATED, RULE_UPDATED, RULE_DELETED, RULE_EVALUATED)
     * @param pageable pagination (page, size, sort)
     * @return paginated audit log entries
     */
    @GetMapping("/audit-history/by-action")
    public ResponseEntity<Page<EntitlementAuditLog>> getAuditHistoryByAction(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String action,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementAuditLogService.findByAction(tenantId, action, pageable));
    }

    /**
     * Retrieves audit history filtered by actor (who performed the changes).
     *
     * @param tenantId      the tenant identifier
     * @param actorUsername  the actor's username
     * @param pageable      pagination (page, size, sort)
     * @return paginated audit log entries
     */
    @GetMapping("/audit-history/by-actor")
    public ResponseEntity<Page<EntitlementAuditLog>> getAuditHistoryByActor(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String actorUsername,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementAuditLogService.findByActor(tenantId, actorUsername, pageable));
    }

    /**
     * Retrieves audit history for a specific rule across all entitlements.
     *
     * @param ruleId   the rule UUID
     * @param tenantId the tenant identifier
     * @param pageable pagination (page, size, sort)
     * @return paginated audit log entries
     */
    @GetMapping("/audit-history/by-rule/{ruleId}")
    public ResponseEntity<Page<EntitlementAuditLog>> getAuditHistoryByRule(
            @PathVariable UUID ruleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementAuditLogService.findByRuleId(tenantId, ruleId, pageable));
    }

    /**
     * Retrieves audit history within a date range across all entitlements.
     *
     * @param tenantId the tenant identifier
     * @param from     start time (epoch millis, inclusive)
     * @param to       end time (epoch millis, inclusive)
     * @param pageable pagination (page, size, sort)
     * @return paginated audit log entries
     */
    @GetMapping("/audit-history/by-date")
    public ResponseEntity<Page<EntitlementAuditLog>> getAuditHistoryByDateRange(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam Long from,
            @RequestParam Long to,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementAuditLogService.findByDateRange(tenantId, from, to, pageable));
    }

     /**
     * Retrieves the audit history for a specific entitlement in a UI-friendly format.
     * Supports optional filtering by action type.
     *
     * @param entitlementId the entitlement UUID
     * @param tenantId      the tenant identifier
     * @param action        optional action filter (GOVERNANCE_UPDATED, RULE_CREATED, RULE_UPDATED, RULE_DELETED, RULE_EVALUATED)
     * @param pageable      pagination (page, size, sort)
     * @return paginated audit history DTOs with user-friendly activity labels
     */
    @GetMapping("/{entitlementId}/audit-history")
    public ResponseEntity<Page<EntitlementAuditHistoryDto>> getEntitlementAuditHistory(
            @PathVariable UUID entitlementId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        return ResponseEntity.ok(entitlementAuditLogService.findAuditHistory(tenantId, entitlementId, action, pageable));
    }

}