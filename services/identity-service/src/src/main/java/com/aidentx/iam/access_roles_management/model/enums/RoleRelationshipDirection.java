package com.aidentx.iam.access_roles_management.model.enums;

import java.util.Locale;

/** Direction filter for the unified access-role relationship API. */
public enum RoleRelationshipDirection {
    BOTH,
    CHILDREN,
    PARENTS;

    public static RoleRelationshipDirection from(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        return RoleRelationshipDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean includesChildren() {
        return this == BOTH || this == CHILDREN;
    }

    public boolean includesParents() {
        return this == BOTH || this == PARENTS;
    }
}
