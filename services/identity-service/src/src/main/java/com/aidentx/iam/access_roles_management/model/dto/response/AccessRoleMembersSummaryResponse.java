package com.aidentx.iam.access_roles_management.model.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Lightweight counts, activity-window, and assignment-mode summary for a role's active members. */
public record AccessRoleMembersSummaryResponse(
        UUID roleId,
        String tenantId,
        int activityWindowDays,
        Instant activityCutoffAt,
        long totalMembers,
        long activeMembers,
        long inactiveMembers,
        String assignmentMode
) {
}

