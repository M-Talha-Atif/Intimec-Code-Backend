package com.aidentx.iam.access_roles_management.model.enums;

import java.util.Locale;

/** View mode for the unified access-role relationship API. */
public enum RoleRelationshipMode {
    DIRECT,
    HIERARCHY,
    CONFLICT;

    public static RoleRelationshipMode from(String value) {
        if (value == null || value.isBlank()) {
            return DIRECT;
        }
        return RoleRelationshipMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

