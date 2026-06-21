package com.aidentx.iam.access_roles_management.model.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * Desired-state request for simulating the impact of saving access-role edits.
 */
public class AccessRoleSaveSimulationRequest {

    private String roleName;
    private List<UUID> identityIds;
    private List<UUID> entitlementIds;
    private List<UUID> childRoleIds;
    private Boolean includeUnchanged;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<UUID> getIdentityIds() {
        return identityIds;
    }

    public void setIdentityIds(List<UUID> identityIds) {
        this.identityIds = identityIds;
    }

    public List<UUID> getEntitlementIds() {
        return entitlementIds;
    }

    public void setEntitlementIds(List<UUID> entitlementIds) {
        this.entitlementIds = entitlementIds;
    }

    public List<UUID> getChildRoleIds() {
        return childRoleIds;
    }

    public void setChildRoleIds(List<UUID> childRoleIds) {
        this.childRoleIds = childRoleIds;
    }

    public Boolean getIncludeUnchanged() {
        return includeUnchanged;
    }

    public void setIncludeUnchanged(Boolean includeUnchanged) {
        this.includeUnchanged = includeUnchanged;
    }
}

