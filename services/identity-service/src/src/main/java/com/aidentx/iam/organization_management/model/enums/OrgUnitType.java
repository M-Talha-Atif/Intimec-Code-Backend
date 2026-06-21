package com.aidentx.iam.organization_management.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type discriminator for an Organizational Unit.
 *
 * <p>Wire values are aligned with the frontend type
 * {@code OrgNodeData.type} in
 * {@code intimec-development-frontend/src/types/organization.ts}.
 * The Java identifiers use UPPER_SNAKE while JSON serialization uses the
 * exact string the frontend expects.
 */
public enum OrgUnitType {

    DIVISION("Division"),
    DEPARTMENT("Department"),
    TEAM("Team"),
    BUSINESS_UNIT("Business Unit");

    private final String wireValue;

    OrgUnitType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static OrgUnitType fromWire(String value) {
        if (value == null) {
            return null;
        }
        for (OrgUnitType t : values()) {
            if (t.wireValue.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown OrgUnitType: " + value);
    }
}
