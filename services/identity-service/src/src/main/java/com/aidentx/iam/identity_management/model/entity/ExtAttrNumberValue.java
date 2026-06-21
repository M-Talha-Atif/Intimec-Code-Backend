package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ext_attr_number_values")
public class ExtAttrNumberValue extends AbstractExtendedAttributeValue {
    @Column(name = "value")
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}