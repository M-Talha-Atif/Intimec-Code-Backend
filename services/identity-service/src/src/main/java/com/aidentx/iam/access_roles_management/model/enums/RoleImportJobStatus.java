package com.aidentx.iam.access_roles_management.model.enums;

/** Lifecycle status for an asynchronous access-role import job. */
public enum RoleImportJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED
}

