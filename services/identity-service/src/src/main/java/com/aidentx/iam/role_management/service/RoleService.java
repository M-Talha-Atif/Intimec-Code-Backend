package com.aidentx.iam.role_management.service;

import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleCreateDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleResponseDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RoleService {
    RoleResponseDto createRole(RoleCreateDto createDto);

    Optional<RoleResponseDto> findById(UUID roleId);

    RoleResponseDto updateRole(UUID roleId, RoleUpdateDto updateDto);

    /**
     * Assigns a Business Role to an Identity. This action publishes an event
     * to create the HAS_ROLE link in the identity graph.
     *
     * @param roleId     The UUID of the role to assign.
     * @param identityId The UUID of the identity receiving the role.
     * @param tenantId   The tenant ID for validation.
     */
    void assignRoleToIdentity(UUID roleId, UUID identityId, String tenantId);

    /**
     * Revokes a Business Role from an Identity. This action publishes an event
     * to delete the HAS_ROLE link in the identity graph.
     *
     * @param roleId     The UUID of the role to revoke.
     * @param identityId The UUID of the identity losing the role.
     * @param tenantId   The tenant ID for validation.
     */
    void revokeRoleFromIdentity(UUID roleId, UUID identityId, String tenantId);

    /**
     * Retrieves a paginated list of all business roles.
     * 
     * @param pageable Pagination and sorting information.
     * @return A page of RoleResponseDto objects.
     */
    Page<RoleResponseDto> findAll(Pageable pageable);

    /**
     * Eagerly fetches a role and its included entitlements.
     * Used by the gRPC service to avoid lazy loading issues.
     */
    Optional<RoleResponseDto> findRoleWithEntitlements(UUID roleId);
}