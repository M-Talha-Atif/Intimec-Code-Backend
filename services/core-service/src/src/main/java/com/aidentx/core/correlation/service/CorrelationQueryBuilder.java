package com.aidentx.core.correlation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CorrelationQueryBuilder {

    private final ObjectMapper objectMapper;

    public record SqlQueryFragment(String sql, List<Value> parameters) {
    }

    public CorrelationQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SqlQueryFragment buildSqlQueryFragment(JsonNode rootGroupNode, JsonNode accountAttributes) {
        return buildGroupFragment(rootGroupNode, accountAttributes);
    }

    private SqlQueryFragment buildGroupFragment(JsonNode groupNode, JsonNode accountAttributes) {
        List<String> sqlParts = new ArrayList<>();
        List<Value> allParams = new ArrayList<>();

        JsonNode rulesNode = groupNode.path("rules");
        if (rulesNode.isMissingNode() || !rulesNode.isArray() || rulesNode.isEmpty()) {
            return new SqlQueryFragment("", List.of());
        }

        for (JsonNode ruleOrGroupNode : rulesNode) {
            SqlQueryFragment fragment;

            if (ruleOrGroupNode.has("combinator")) {
                fragment = buildGroupFragment(ruleOrGroupNode, accountAttributes);
            } else {
                fragment = buildRuleFragment(ruleOrGroupNode, accountAttributes);
            }

            if (fragment != null && !fragment.sql().isBlank()) {
                sqlParts.add(fragment.sql());
                allParams.addAll(fragment.parameters());
            }
        }

        if (sqlParts.isEmpty()) {
            return new SqlQueryFragment("", List.of());
        }

        String combinator = " " + (groupNode.path("combinator").asText("and").equalsIgnoreCase("or") ? "OR" : "AND")
                + " ";
        String finalSql = "(" + String.join(combinator, sqlParts) + ")";

        if (groupNode.path("not").asBoolean(false)) {
            finalSql = "NOT " + finalSql;
        }

        return new SqlQueryFragment(finalSql, allParams);
    }

    private SqlQueryFragment buildRuleFragment(JsonNode ruleNode, JsonNode accountAttributes) {
        String targetValueType = ruleNode.path("valueSource").asText("value");
        String valueKey = ruleNode.path("value").asText();

        JsonNode sourceValueNode;
        if ("field".equalsIgnoreCase(targetValueType)) {
            sourceValueNode = accountAttributes.get(valueKey);
        } else {
            sourceValueNode = ruleNode.get("value");
        }

        if (sourceValueNode == null || sourceValueNode.isNull() || sourceValueNode.asText().isBlank()) {
            return null;
        }
        String sourceValue = sourceValueNode.asText();

        String targetIdentityAttribute = ruleNode.path("field").asText(); // The target attribute on the Identity
        String operator = getSqlOperator(ruleNode.path("operator").asText());
        String finalValue = prepareValueForOperator(sourceValue, ruleNode.path("operator").asText());

        String sql = String.format("(\"%s\" %s ?)", targetIdentityAttribute, operator);

        List<Value> params = List.of(Value.newBuilder().setStringValue(finalValue).build());

        return new SqlQueryFragment(sql, params);
    }

    private String prepareValueForOperator(String value, String operator) {
        return switch (operator.toLowerCase()) {
            case "contains", "doesnotcontain" -> "%" + value + "%";
            case "beginswith", "doesnotbeginwith" -> value + "%";
            case "endswith", "doesnotendwith" -> "%" + value;
            default -> value;
        };
    }

    private String getSqlOperator(String operator) {
        return switch (operator.toLowerCase()) {
            case "=", "in" -> "=";
            case "!=", "notin" -> "!=";
            case ">" -> ">";
            case "<" -> "<";
            case "<=" -> "<=";
            case ">=" -> ">=";
            case "contains", "beginswith", "endswith" -> "ILIKE";
            case "doesnotcontain", "doesnotbeginwith", "doesnotendwith" -> "NOT ILIKE";
            default -> throw new IllegalArgumentException("Unsupported correlation operator: " + operator);
        };
    }
}