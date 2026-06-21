package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Search filter for shadow queries.
 * Supports simple field comparisons and complex logical operations.
 */
public class SearchFilter {

    @JsonProperty("field")
    private String field;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("and")
    @Valid
    private List<SearchFilter> and;

    @JsonProperty("or")
    @Valid
    private List<SearchFilter> or;

    // Constructors
    public SearchFilter() {
    }

    public SearchFilter(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    // Getters and Setters
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<SearchFilter> getAnd() {
        return and;
    }

    public void setAnd(List<SearchFilter> and) {
        this.and = and;
    }

    public List<SearchFilter> getOr() {
        return or;
    }

    public void setOr(List<SearchFilter> or) {
        this.or = or;
    }
}