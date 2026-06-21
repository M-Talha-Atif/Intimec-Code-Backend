package com.aidentx.iam.identity_management.model.enums;

/**
 * Enum representing the different types of identities in the system.
 */
public enum IdentityType {
    EMPLOYEE("EMPLOYEE"),
    NON_EMPLOYEE("NON_EMPLOYEE"),
    NON_HUMAN("NON_HUMAN");

    private final String value;

    IdentityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns the enum constant for the given string value.
     * @param value the string value
     * @return the corresponding enum constant
     * @throws IllegalArgumentException if no enum constant matches the value
     */
    public static IdentityType fromValue(String value) {
        for (IdentityType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown identity type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
