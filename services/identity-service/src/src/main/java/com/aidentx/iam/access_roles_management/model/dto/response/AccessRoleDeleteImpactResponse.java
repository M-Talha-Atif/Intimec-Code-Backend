package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Counts-only preview of what deleting an access role will impact.
 */
public record AccessRoleDeleteImpactResponse(
        int rolesDeletedCount,
        long activeAssignmentsRevoked,
        long distinctUsersAffected,
        long directEntitlementLinksRemoved,
        long effectiveEntitlementRowsRemoved,
        long distinctEffectiveEntitlementsAffected,
        long childRoleLinksRemoved,
        long parentRoleLinksRemoved,
        long parentRolesToRefresh,
        long assignmentRulesImpacted,
        long enabledAssignmentRulesImpacted,
        long executionRunsImpacted,
        long executionItemsImpacted,
        long auditRecordsImpacted,
        long totalLinksRemoved
) {
}

