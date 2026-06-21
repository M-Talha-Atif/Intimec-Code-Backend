package com.aidentx.iam.identity_management.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE,
    ARRAY,
    OBJECT;

    @JsonCreator
    public static DataType fromString(String value) {
        if (value == null) return null;
        for (DataType type : DataType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DataType: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}