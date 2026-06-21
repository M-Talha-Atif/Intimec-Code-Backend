package com.aidentx.iam.access_roles_management.service;

import java.util.List;
import java.util.UUID;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleAssignmentResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.GroupedRoleEntitlementsDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing entitlement links on access roles.
 */
public interface AccessRoleEntitlementService {

    /**
     * Retrieves roles linked to an entitlement.
     */
    Page<EntitlementRoleResponse> getRolesForEntitlement(String tenantId, UUID entitlementId, Pageable pageable);

    /**
     * Adds one entitlement to multiple roles, skipping already-linked roles.
     */
    EntitlementRoleAssignmentResponse addRolesToEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds);

    /**
     * Removes one entitlement from multiple roles, skipping roles that are not linked.
     */
    EntitlementRoleAssignmentResponse removeRolesFromEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds);

    /**
     * Synchronizes one entitlement to exactly the supplied roles.
     */
    EntitlementRoleAssignmentResponse syncRolesForEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds);

    /**
     * Adds entitlements to a role.
     *
     * @param tenantId       the tenant identifier
     * @param roleId         the role ID
     * @param entitlementIds the entitlement IDs to add
     */
    void addEntitlements(String tenantId, UUID roleId, List<UUID> entitlementIds);

    /**
     * Removes entitlements from a role.
     *
     * @param tenantId       the tenant identifier
     * @param roleId         the role ID
     * @param entitlementIds the entitlement IDs to remove
     */
    void removeEntitlements(String tenantId, UUID roleId, List<UUID> entitlementIds);

    /**
     * Retrieves all entitlement IDs linked to a role.
     *
     * @param roleId the role ID
     * @return list of entitlement IDs
     */
    List<UUID> getEntitlementIds(UUID roleId);

    /**
     * Retrieves detailed information about entitlements linked to a role.
     *
     * @param roleId   the role ID
     * @param pageable pagination parameters
     * @return list of entitlement response DTOs
     */
    Page<EntitlementDtos.EntitlementResponseDto> getEntitlementDetails(UUID roleId, String tenantId, Pageable pageable);

    /**
     * Retrieves role entitlements grouped by application with two-level pagination.
     * <p>
     * Level 1 (application): paginates the distinct applications that contain
     * entitlements linked to this role.
     * Level 2 (entitlement): paginates entitlements within each application on the current app page.
     * </p>
     *
     * @param roleId      the role whose entitlements to retrieve
     * @param tenantId    tenant identifier for application name resolution
     * @param appPageable pagination for the application level
     * @param entPageable pagination for the entitlement level within each application
     * @param search      optional search across application name and entitlement fields
     * @return grouped response with nested pagination
     */
    GroupedRoleEntitlementsDto getGroupedEntitlementDetails(
            UUID roleId,
            String tenantId,
            Pageable appPageable,
            Pageable entPageable,
            String search
    );

    /**
     * Compares two access roles by overlapping effective entitlements.
     *
     * @param tenantId     tenant identifier
     * @param roleAId      first role ID
     * @param roleBId      second role ID
     * @param sourceFilter optional source filter: ALL, DIRECT, or INHERITED
     * @param pageable     pagination for overlapping entitlement rows
     * @return paginated comparison response
     */
    AccessRoleEntitlementComparisonResponse compareRoleEntitlements(
            String tenantId,
            UUID roleAId,
            UUID roleBId,
            String sourceFilter,
            Pageable pageable
    );

    /**
     * Returns unique entitlement distribution metrics for a role, split into direct
     * entitlements and inherited child-role entitlements.
     *
     * @param tenantId tenant identifier
     * @param roleId   access role identifier
     * @return role entitlement distribution summary
     */
    AccessRoleEntitlementDistributionResponse getEntitlementDistribution(String tenantId, UUID roleId);
}

