package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sort criteria for shadow queries.
 */
public class SortCriteria {

    @JsonProperty("field")
    private String field;

    @JsonProperty("direction")
    private String direction = "asc";

    // Constructors
    public SortCriteria() {
    }

    public SortCriteria(String field, String direction) {
        this.field = field;
        this.direction = direction;
    }

    // Getters and Setters
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}