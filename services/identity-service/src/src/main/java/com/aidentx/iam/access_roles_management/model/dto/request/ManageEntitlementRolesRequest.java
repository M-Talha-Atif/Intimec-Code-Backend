package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding, removing, or syncing roles for an entitlement.
 */
public class ManageEntitlementRolesRequest {

    @NotNull(message = "Role IDs list is required")
    private List<@NotNull(message = "Role ID is required") UUID> roleIds;

    public List<UUID> getRoleIds() { return roleIds; }
    public void setRoleIds(List<UUID> roleIds) { this.roleIds = roleIds; }
}
