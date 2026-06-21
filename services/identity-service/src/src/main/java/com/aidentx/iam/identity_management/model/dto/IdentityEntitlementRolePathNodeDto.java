package com.aidentx.iam.identity_management.model.dto;

import java.util.UUID;

/**
 * One role node in a role-derived entitlement route.
 *
 * @param roleId role identifier
 * @param roleName role display/name for UI rendering
 * @param depth zero for the assigned parent role, increasing toward child/source role
 */
public record IdentityEntitlementRolePathNodeDto(
        UUID roleId,
        String roleName,
        Integer depth
) {
}

