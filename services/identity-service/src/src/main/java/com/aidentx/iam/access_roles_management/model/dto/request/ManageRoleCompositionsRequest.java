package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding or removing child roles from a parent role.
 */
public class ManageRoleCompositionsRequest {

    @NotNull(message = "Child role IDs list is required")
    private List<@NotNull(message = "Child role ID is required") UUID> childRoleIds;

    public List<UUID> getChildRoleIds() { return childRoleIds; }
    public void setChildRoleIds(List<UUID> childRoleIds) { this.childRoleIds = childRoleIds; }
}

