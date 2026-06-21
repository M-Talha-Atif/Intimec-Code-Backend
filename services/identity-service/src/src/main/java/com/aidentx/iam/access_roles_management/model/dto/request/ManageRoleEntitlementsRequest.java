package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding or removing entitlements from a role.
 */
public class ManageRoleEntitlementsRequest {

    @NotNull(message = "Entitlement IDs list is required")
    private List<@NotNull(message = "Entitlement ID is required") UUID> entitlementIds;

    public List<UUID> getEntitlementIds() { return entitlementIds; }
    public void setEntitlementIds(List<UUID> entitlementIds) { this.entitlementIds = entitlementIds; }
}

