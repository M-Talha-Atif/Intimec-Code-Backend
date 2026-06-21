package com.aidentx.iam.organization_management.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Role granted to a delegate of an organizational unit.
 *
 * <p>Wire values match {@code OrgDelegate.role} in the frontend type.
 */
public enum DelegateRole {

    ADMIN("admin"),
    REVIEWER("reviewer"),
    APPROVER("approver");

    private final String wireValue;

    DelegateRole(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static DelegateRole fromWire(String value) {
        if (value == null) {
            return null;
        }
        for (DelegateRole r : values()) {
            if (r.wireValue.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown DelegateRole: " + value);
    }
}
