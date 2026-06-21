package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.UUID;

/**
 * User-friendly entitlement drilldown row for a specific role member assignment.
 *
 * <p>This DTO is intentionally focused on the entitlement list shown after a
 * user expands a member in the role-members screen. It hides low-level closure
 * fields such as raw depth/path arrays and instead returns clearer display
 * fields for inheritance and path context.</p>
 *
 * <p>The {@code assignmentStatus} field indicates whether this entitlement is
 * actually provisioned to the identity's accounts:</p>
 * <ul>
 *   <li>{@code ASSIGNED} — entitlement row exists in entitlement_assignments table</li>
 *   <li>{@code PENDING_NO_ACCOUNT} — identity has no account on the target application</li>
 *   <li>{@code NOT_ASSIGNED} — account exists but entitlement not yet provisioned</li>
 * </ul>
 */
public record AccessRoleMemberEntitlementResponse(
        UUID entitlementId,
        String entitlementName,
        UUID applicationId,
        String applicationName,
        String accessPathType,
        String accessPathLabel,
        UUID childRoleId,
        String childRoleName,
        String inheritanceDepthLabel,
        String assignmentPath,
        String assignmentStatus,
        String assignmentStatusReason
) {
}
