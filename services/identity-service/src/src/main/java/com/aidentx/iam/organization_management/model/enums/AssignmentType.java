package com.aidentx.iam.organization_management.model.enums;

/** Why an identity is associated with an organizational unit. */
public enum AssignmentType {
    /** The HR source system (Workday, SAP, etc.) is the authoritative record for this assignment. */
    HR_OF_RECORD,
    /** Secondary/matrix reporting line; does not imply primary department membership. */
    DOTTED_LINE,
    /** Temporary assignment for a specific project; expected to end when the project closes. */
    PROJECT,
    /** Short-term temporary assignment (e.g. cover, secondment). */
    TEMPORARY,
    /** Assignment created by an IGA administrator via a Mover lifecycle action, not from an HR feed. */
    GOVERNANCE_MOVER
}
