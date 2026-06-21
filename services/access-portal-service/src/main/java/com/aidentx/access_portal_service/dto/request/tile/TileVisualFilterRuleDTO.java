package com.aidentx.access_portal_service.dto.request.tile;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for rules inside a visual filter
 */
@Getter
@Setter
public class TileVisualFilterRuleDTO {
    private String logicalOperator; // AND, OR, NOT
    private String fieldName;       // department, risk_level, etc.
    private String operator;        // EQ, NE, GT, LT, IN, NOT_IN
    private String value;           // string or JSON list
}
