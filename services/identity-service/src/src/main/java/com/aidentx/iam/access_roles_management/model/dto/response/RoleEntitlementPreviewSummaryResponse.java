package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Summary of effective entitlements a role would grant during preview.
 */
public class RoleEntitlementPreviewSummaryResponse {

    private UUID roleId;
    private String roleName;
    private int effectiveEntitlementCount;
    private int directEntitlementCount;
    private int inheritedEntitlementCount;
    private List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlements;

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public int getEffectiveEntitlementCount() { return effectiveEntitlementCount; }
    public void setEffectiveEntitlementCount(int effectiveEntitlementCount) { this.effectiveEntitlementCount = effectiveEntitlementCount; }

    public int getDirectEntitlementCount() { return directEntitlementCount; }
    public void setDirectEntitlementCount(int directEntitlementCount) { this.directEntitlementCount = directEntitlementCount; }

    public int getInheritedEntitlementCount() { return inheritedEntitlementCount; }
    public void setInheritedEntitlementCount(int inheritedEntitlementCount) { this.inheritedEntitlementCount = inheritedEntitlementCount; }

    public List<AccessRoleAssignmentRulePreviewEntitlementResponse> getEntitlements() { return entitlements; }
    public void setEntitlements(List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlements) { this.entitlements = entitlements; }
}

