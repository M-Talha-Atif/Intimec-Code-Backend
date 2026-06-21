package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ext_attr_string_values")
public class ExtAttrStringValue extends AbstractExtendedAttributeValue {
    @Column(name = "value")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}