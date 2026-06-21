package com.aidentx.core.notifications_management.enums;

/**
 * Enumeration representing different types of operations that can trigger notifications.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public enum OperationType {
    
    /**
     * Operation triggered when a user requests a password reset.
     */
    PASSWORD_RESET,

    /**
     * Operation triggered when an access request has been approved.
     */
    ACCESS_REQUEST_APPROVED,

    /**
     * Operation triggered to remind a user to reset their password.
     */
    PASSWORD_REMINDER_RESET,

    /**
     * Operation triggered when an internal role is assigned to a user.
     */
    ASSIGN_INTERNAL_ROLE,

    /**
     * Operation triggered when an internal role is revoked from a user.
     */
    REVOKE_INTERNAL_ROLE,

    /**
     * Operation triggered when a workflow approval is requested.
     */
    WORKFLOW_APPROVAL_REQUESTED,

    /**
     * Operation triggered when a workflow is completed.
     */
    WORKFLOW_COMPLETED,

    /**
     * Operation triggered when a workflow approval is approved.
     */
    WORKFLOW_APPROVAL_APPROVED,

    /**
     * Operation triggered when a workflow approval is rejected.
     */
    WORKFLOW_APPROVAL_REJECTED,

    // ==================== Phase 3: Delegation Operations ====================

    /**
     * Operation triggered when a delegation rule is created.
     * Phase 3: Delegation Enhancement
     */
    DELEGATION_CREATED,

    /**
     * Operation triggered when an approval is automatically delegated
     * based on an active delegation rule.
     * Phase 3: Delegation Enhancement
     */
    APPROVAL_AUTO_DELEGATED,

    // ==================== Phase 4: Escalation Operations ====================

    /**
     * Operation triggered when an approval is escalated to a new approver
     * due to timeout.
     * Phase 4: Escalation Enhancement
     */
    APPROVAL_ESCALATED,

    /**
     * Operation triggered to notify the original approver that their
     * approval request has been escalated.
     * Phase 4: Escalation Enhancement
     */
    APPROVAL_ESCALATED_FROM_YOU
}
