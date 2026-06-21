package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord.Outcome;

import java.util.UUID;

/**
 * Rich result returned by {@link IdentityOrgSyncService} for a single
 * org-assignment attempt. Carries enough context for the reconciliation
 * audit table (per-identity outcome) and the summary chips in the
 * task-run details UI.
 */
public record OrgAssignmentResult(
        Outcome outcome,
        UUID resolvedOrgUnitId,
        UUID previousOrgUnitId,
        String detail) {

    public boolean isSuccess() {
        return outcome == Outcome.ASSIGNED
                || outcome == Outcome.REASSIGNED
                || outcome == Outcome.UNCHANGED;
    }

    public static OrgAssignmentResult skippedBlank() {
        return new OrgAssignmentResult(Outcome.SKIPPED_BLANK, null, null, null);
    }
}
