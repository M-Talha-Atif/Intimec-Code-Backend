package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ext_attr_boolean_values")
public class ExtAttrBooleanValue extends AbstractExtendedAttributeValue {
    @Column(name = "value")
    private Boolean value;

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }
}