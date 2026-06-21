package com.aidentx.iam.organization_management.model.enums;

/** Event types written to {@code org_unit_history}. */
public enum OrgEventType {
    CREATED,
    UPDATED,
    RENAMED,
    MOVED,
    MERGED,
    SPLIT,
    DEACTIVATED,
    REACTIVATED,
    ASSIGNMENT_ADDED,
    ASSIGNMENT_REMOVED,
    PRIMARY_CHANGED,
    DELEGATE_ADDED,
    DELEGATE_REVOKED,
    IMPORTED
}
