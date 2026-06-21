package com.aidentx.iam.access_roles_management.service;

import java.util.List;
import java.util.UUID;

import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleSaveSimulationRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDetailResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDeleteImpactResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDirectorySummaryResponseDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleGroupedMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberEntitlementResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMembersSummaryResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleSaveSimulationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing access role lifecycle operations.
 */
public interface AccessRoleService {

    /**
     * Creates a new access role along with its entitlement and composition links.
     *
     * @param tenantId the tenant identifier
     * @param request  the creation request DTO
     * @return the created role response
     */
    AccessRoleResponse createRole(String tenantId, CreateAccessRoleRequest request);

    /**
     * Retrieves a role's full detail including entitlements and child/parent roles.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return the detailed role response
     */
    AccessRoleDetailResponse getRoleDetail(String tenantId, UUID roleId);

    /**
     * Lists all roles for a tenant with optional filtering and pagination.
     *
     * @param tenantId      the tenant identifier
     * @param types         optional role type filters
     * @param statuses      optional role status filters
     * @param businessUnits optional business-unit filters
     * @param riskLevels    optional risk-level filters
     * @param search        optional search text matched against role name, display name, and description
     * @param pageable      pagination parameters
     * @return a page of role responses
     */
    Page<AccessRoleResponse> listRoles(
            String tenantId,
            List<String> types,
            List<String> statuses,
            List<String> businessUnits,
            List<String> riskLevels,
            String search,
            Pageable pageable
    );

    /**
     * Returns a lightweight directory summary grouped by business unit and role type.
     *
     * <p>This method is intended for the directory navigator and returns only:
     * business units, type buckets, and counts. It does not return full role rows.</p>
     *
     * @param tenantId   the tenant identifier
     * @param statuses   optional role status filters
     * @param riskLevels optional risk-level filters
     * @param search     optional search text matched against role name, display name, and description
     * @return grouped directory summary response
     */
    AccessRoleDirectorySummaryResponseDto getRoleDirectorySummary(
            String tenantId,
            List<String> statuses,
            List<String> riskLevels,
            String search
    );

    /**
     * Returns active members of a role enriched with effective entitlement and path details.
     *
     * @param tenantId              the tenant identifier
     * @param roleId                the role ID
     * @param search                optional text search across member, entitlement, application, and path names
     * @param identityTypes         optional identity-type filters
     * @param identityRiskLevels    optional identity-risk filters
     * @param accessPathTypes       optional access-path filters
     * @param assignmentSourceTypes optional role-assignment source filters
     * @param applicationIds        optional application/resource filters
     * @param entitlementIds        optional entitlement filters
     * @param pageable              pagination parameters applied to the enriched member rows
     * @return paginated enriched role-member rows
     */
    Page<AccessRoleMemberResponse> getRoleMembers(
            String tenantId,
            UUID roleId,
            String search,
            List<String> identityTypes,
            List<String> identityRiskLevels,
            List<String> accessPathTypes,
            List<String> assignmentSourceTypes,
            List<UUID> applicationIds,
            List<UUID> entitlementIds,
            Pageable pageable);

    /**
     * Returns lightweight role member counts and assignment-mode summary.
     *
     * @param tenantId           the tenant identifier
     * @param roleId             the role ID
     * @param activityWindowDays number of days used to classify active vs inactive members
     * @return member count summary for the role
     */
    AccessRoleMembersSummaryResponse getRoleMembersSummary(String tenantId, UUID roleId, Integer activityWindowDays);

    /**
     * Returns active members of a role as lightweight member summaries.
     *
     * @param tenantId              the tenant identifier
     * @param roleId                the role ID
     * @param search                optional text search across member/employee name only
     * @param pageable              pagination parameters applied to grouped members
     * @return paginated grouped role members
     */
    Page<AccessRoleGroupedMemberResponse> getRoleMembersGrouped(
            String tenantId,
            UUID roleId,
            String search,
            Pageable pageable);

    /**
     * Returns effective entitlements for one active member assignment of a role.
     *
     * @param tenantId        the tenant identifier
     * @param roleId          the role ID
     * @param assignmentId    the active role-member assignment ID
     * @param search          optional search across entitlement, application, and child-role names
     * @param accessPathTypes optional access-path filters
     * @param applicationIds  optional application/resource filters
     * @param entitlementIds  optional entitlement filters
     * @param pageable        pagination parameters applied to entitlement rows
     * @return paginated member entitlement drilldown rows
     */
    Page<AccessRoleMemberEntitlementResponse> getRoleMemberEntitlements(
            String tenantId,
            UUID roleId,
            UUID assignmentId,
            String search,
            List<String> accessPathTypes,
            List<UUID> applicationIds,
            List<UUID> entitlementIds,
            Pageable pageable);

    /**
     * Updates governance metadata on an existing role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @param request  the update request DTO
     * @return the updated role response
     */
    AccessRoleResponse updateRole(String tenantId, UUID roleId, UpdateAccessRoleRequest request);

    /**
     * Returns counts-only impact preview for deleting one access role.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     * @return delete impact counts
     */
    AccessRoleDeleteImpactResponse getDeleteImpact(String tenantId, UUID roleId);

    /**
     * Simulates the identity and entitlement impact of saving proposed role edits.
     *
     * @param tenantId          the tenant identifier
     * @param roleId            the role being edited
     * @param request           proposed role desired state
     * @param includeUnchanged  whether unchanged entitlement rows should be included
     * @return read-only impact simulation response
     */
    AccessRoleSaveSimulationResponse simulateRoleSaveImpact(
            String tenantId,
            UUID roleId,
            AccessRoleSaveSimulationRequest request,
            boolean includeUnchanged);

    /**
     * Deletes a role and all its associated entitlement and composition links.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the role ID
     */
    void deleteRole(String tenantId, UUID roleId);
}