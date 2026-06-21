package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Extended response DTO for the role detail view.
 * Includes the role's attached entitlements and child roles.
 */
public class AccessRoleDetailResponse extends AccessRoleResponse {

    /** IDs of entitlements attached to this role. */
    private List<UUID> entitlementIds;

    /** Child roles nested under this role. */
    private List<AccessRoleResponse> childRoles;

    /** Parent roles that contain this role. */
    private List<AccessRoleResponse> parentRoles;

    // --- Getters and Setters ---

    public List<UUID> getEntitlementIds() { return entitlementIds; }
    public void setEntitlementIds(List<UUID> entitlementIds) { this.entitlementIds = entitlementIds; }

    public List<AccessRoleResponse> getChildRoles() { return childRoles; }
    public void setChildRoles(List<AccessRoleResponse> childRoles) { this.childRoles = childRoles; }

    public List<AccessRoleResponse> getParentRoles() { return parentRoles; }
    public void setParentRoles(List<AccessRoleResponse> parentRoles) { this.parentRoles = parentRoles; }
}

