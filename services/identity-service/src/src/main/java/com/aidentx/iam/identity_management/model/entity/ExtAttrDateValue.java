package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ext_attr_date_values")
public class ExtAttrDateValue extends AbstractExtendedAttributeValue {
    @Column(name = "value")
    private Instant value;

    public Instant getValue() {
        return value;
    }

    public void setValue(Instant value) {
        this.value = value;
    }
}