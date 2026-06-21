package com.aidentx.iam.organization_management.model.enums;

/**
 * The kind of organizational hierarchy a unit and its assignments belong to.
 *
 * <p>FUNCTIONAL is the primary HR-of-record hierarchy and is the default
 * the frontend renders. Other hierarchies (geographic, legal, cost) are
 * supported by the schema for future use.
 */
public enum HierarchyType {
    FUNCTIONAL,
    GEOGRAPHIC,
    LEGAL,
    COST
}
