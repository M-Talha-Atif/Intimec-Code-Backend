package com.aidentx.access_portal_service.model;


import java.util.List;
import java.util.UUID;

/**
 * Data class for role assignment details in cart.
 * Matches the structure expected by /assign-from-cart API.
 */
public class RoleAssignmentDetail {
    public final List<UUID> userIds;
    public final List<String> userNames;
    public final String roleName;
    public final String justification;
    public final Integer validityDays;

    public RoleAssignmentDetail(List<UUID> userIds, List<String> userNames, String roleName, String justification, Integer validityDays) {
        this.userIds = userIds;
        this.userNames = userNames != null ? userNames : List.of();
        this.roleName = roleName;
        this.justification = justification;
        this.validityDays = validityDays;
    }
}
