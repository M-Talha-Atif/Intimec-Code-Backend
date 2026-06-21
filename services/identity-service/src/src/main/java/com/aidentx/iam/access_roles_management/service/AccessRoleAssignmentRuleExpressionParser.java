package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses simple UI rule expressions into the canonical grouped access-role rule DTO format.
 *
 * <p>Supported examples:</p>
 * <ul>
 *   <li>{@code user.department == "Finance" && user.jobTitle.contains("Manager")}</li>
 *   <li>{@code user.department in ["Finance", "Accounting"] || user.email.endsWith("@corp.com")}</li>
 *   <li>{@code !(user.department == "HR")}</li>
 * </ul>
 */
@Component
public class AccessRoleAssignmentRuleExpressionParser {

    public List<AccessRoleAssignmentRuleConditionGroupDto> parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_EXPRESSION_REQUIRED);
        }
        Parser parser = new Parser(tokenize(expression));
        Node node = parser.parseExpression();
        parser.expectEnd();
        return toTopLevelGroups(node);
    }

    private List<AccessRoleAssignmentRuleConditionGroupDto> toTopLevelGroups(Node node) {
        Node normalized = node;
        if (normalized instanceof BinaryNode binary && "OR".equals(binary.operator())) {
            List<Node> branches = new ArrayList<>();
            flatten(binary, "OR", branches);
            List<AccessRoleAssignmentRuleConditionGroupDto> groups = new ArrayList<>();
            for (Node branch : branches) {
                AccessRoleAssignmentRuleConditionGroupDto group = toGroup(branch);
                group.setInterGroupOperator("OR");
                groups.add(group);
            }
            return groups;
        }
        return List.of(toGroup(normalized));
    }

    private AccessRoleAssignmentRuleConditionGroupDto toGroup(Node node) {
        if (node instanceof ConditionNode condition) {
            return new AccessRoleAssignmentRuleConditionGroupDto(
                    "AND",
                    "AND",
                    false,
                    List.of(condition.condition()),
                    null);
        }
        if (node instanceof NotNode notNode) {
            AccessRoleAssignmentRuleConditionGroupDto group = toGroup(notNode.child());
            group.setNegated(!Boolean.TRUE.equals(group.getNegated()));
            return group;
        }
        if (node instanceof BinaryNode binary) {
            List<Node> children = new ArrayList<>();
            flatten(binary, binary.operator(), children);
            AccessRoleAssignmentRuleConditionGroupDto group = new AccessRoleAssignmentRuleConditionGroupDto();
            group.setGroupOperator(binary.operator());
            group.setInterGroupOperator("AND");
            group.setNegated(false);
            List<AccessRoleAssignmentRuleConditionDto> conditions = new ArrayList<>();
            List<AccessRoleAssignmentRuleConditionGroupDto> subGroups = new ArrayList<>();
            for (Node child : children) {
                if (child instanceof ConditionNode condition) {
                    conditions.add(condition.condition());
                } else {
                    subGroups.add(toGroup(child));
                }
            }
            group.setConditions(conditions.isEmpty() ? null : conditions);
            group.setConditionGroups(subGroups.isEmpty() ? null : subGroups);
            return group;
        }
        throw new IllegalArgumentException("Unsupported expression node");
    }

    private void flatten(Node node, String operator, List<Node> output) {
        if (node instanceof BinaryNode binary && operator.equals(binary.operator())) {
            flatten(binary.left(), operator, output);
            flatten(binary.right(), operator, output);
            return;
        }
        output.add(node);
    }

    private List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < expression.length()) {
            char ch = expression.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (ch == '&' && peek(expression, index + 1) == '&') {
                tokens.add(new Token(TokenType.AND, "&&"));
                index += 2;
                continue;
            }
            if (ch == '|' && peek(expression, index + 1) == '|') {
                tokens.add(new Token(TokenType.OR, "||"));
                index += 2;
                continue;
            }
            if (ch == '=' && peek(expression, index + 1) == '=') {
                tokens.add(new Token(TokenType.EQ, "=="));
                index += 2;
                continue;
            }
            if (ch == '!' && peek(expression, index + 1) == '=') {
                tokens.add(new Token(TokenType.NE, "!="));
                index += 2;
                continue;
            }
            if (ch == '!') {
                tokens.add(new Token(TokenType.NOT, "!"));
                index++;
                continue;
            }
            if (ch == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                index++;
                continue;
            }
            if (ch == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                index++;
                continue;
            }
            if (ch == '[') {
                tokens.add(new Token(TokenType.LBRACKET, "["));
                index++;
                continue;
            }
            if (ch == ']') {
                tokens.add(new Token(TokenType.RBRACKET, "]"));
                index++;
                continue;
            }
            if (ch == ',') {
                tokens.add(new Token(TokenType.COMMA, ","));
                index++;
                continue;
            }
            if (ch == '.') {
                tokens.add(new Token(TokenType.DOT, "."));
                index++;
                continue;
            }
            if (ch == '"' || ch == '\'') {
                int start = ++index;
                StringBuilder value = new StringBuilder();
                boolean closed = false;
                while (index < expression.length()) {
                    char current = expression.charAt(index);
                    if (current == '\\' && index + 1 < expression.length()) {
                        value.append(expression.charAt(index + 1));
                        index += 2;
                        continue;
                    }
                    if (current == ch) {
                        closed = true;
                        index++;
                        break;
                    }
                    value.append(current);
                    index++;
                }
                if (!closed) {
                    throw new IllegalArgumentException("Unterminated string literal at position " + start);
                }
                tokens.add(new Token(TokenType.STRING, value.toString()));
                continue;
            }
            if (Character.isLetter(ch) || ch == '_') {
                int start = index;
                while (index < expression.length()) {
                    char current = expression.charAt(index);
                    if (!Character.isLetterOrDigit(current) && current != '_') {
                        break;
                    }
                    index++;
                }
                String text = expression.substring(start, index);
                String upper = text.toUpperCase(Locale.ROOT);
                if ("AND".equals(upper)) {
                    tokens.add(new Token(TokenType.AND, text));
                } else if ("OR".equals(upper)) {
                    tokens.add(new Token(TokenType.OR, text));
                } else if ("NOT".equals(upper)) {
                    tokens.add(new Token(TokenType.NOT, text));
                } else if ("IN".equals(upper)) {
                    tokens.add(new Token(TokenType.IN, text));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, text));
                }
                continue;
            }
            throw new IllegalArgumentException("Unexpected character '" + ch + "' at position " + index);
        }
        tokens.add(new Token(TokenType.END, ""));
        return tokens;
    }

    private char peek(String value, int index) {
        return index < value.length() ? value.charAt(index) : '\0';
    }

    private final class Parser {
        private final List<Token> tokens;
        private int position;

        private Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        private Node parseExpression() {
            return parseOr();
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (match(TokenType.OR)) {
                left = new BinaryNode("OR", left, parseAnd());
            }
            return left;
        }

        private Node parseAnd() {
            Node left = parseUnary();
            while (match(TokenType.AND)) {
                left = new BinaryNode("AND", left, parseUnary());
            }
            return left;
        }

        private Node parseUnary() {
            if (match(TokenType.NOT)) {
                return new NotNode(parseUnary());
            }
            if (match(TokenType.LPAREN)) {
                Node node = parseExpression();
                consume(TokenType.RPAREN, "Expected ')' after grouped expression");
                return node;
            }
            return parseCondition();
        }

        private Node parseCondition() {
            String field = parseFieldReference();
            if (match(TokenType.EQ)) {
                return condition(field, RuleOperator.EQUALS.name(), parseValue(), null);
            }
            if (match(TokenType.NE)) {
                return condition(field, RuleOperator.NOT_EQUALS.name(), parseValue(), null);
            }
            if (match(TokenType.IN)) {
                return condition(field, RuleOperator.IN.name(), null, parseList());
            }
            if (match(TokenType.NOT)) {
                consume(TokenType.IN, "Expected IN after NOT");
                return condition(field, RuleOperator.NOT_IN.name(), null, parseList());
            }
            if (match(TokenType.DOT)) {
                String method = consume(TokenType.IDENTIFIER, "Expected method name after '.'").text();
                consume(TokenType.LPAREN, "Expected '(' after method name");
                String value = parseValue();
                consume(TokenType.RPAREN, "Expected ')' after method argument");
                return condition(field, methodToOperator(method), value, null);
            }
            throw new IllegalArgumentException("Expected comparison operator after field '" + field + "'");
        }

        private String parseFieldReference() {
            String first = consume(TokenType.IDENTIFIER, "Expected field reference").text();
            String field;
            if ("user".equalsIgnoreCase(first) || "identity".equalsIgnoreCase(first)) {
                consume(TokenType.DOT, "Expected '.' after " + first);
                field = consume(TokenType.IDENTIFIER, "Expected field name after " + first + ".").text();
            } else {
                field = first;
            }
            return normalizeFieldAlias(field);
        }

        private String parseValue() {
            Token token = current();
            if (match(TokenType.STRING) || match(TokenType.IDENTIFIER)) {
                return token.text();
            }
            throw new IllegalArgumentException("Expected string or identifier value");
        }

        private List<String> parseList() {
            consume(TokenType.LBRACKET, "Expected '[' before value list");
            List<String> values = new ArrayList<>();
            if (!check(TokenType.RBRACKET)) {
                do {
                    values.add(parseValue());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RBRACKET, "Expected ']' after value list");
            return values;
        }

        private Node condition(String field, String operator, String value, List<String> values) {
            return new ConditionNode(new AccessRoleAssignmentRuleConditionDto(field, operator, value, values));
        }

        private String methodToOperator(String method) {
            return switch (method.trim().toLowerCase(Locale.ROOT)) {
                case "contains" -> RuleOperator.CONTAINS.name();
                case "startswith", "starts_with" -> RuleOperator.STARTS_WITH.name();
                case "endswith", "ends_with" -> RuleOperator.ENDS_WITH.name();
                default -> throw new IllegalArgumentException("Unsupported expression method: " + method);
            };
        }

        private String normalizeFieldAlias(String field) {
            if ("title".equalsIgnoreCase(field)) {
                return "jobTitle";
            }
            return field;
        }

        private boolean match(TokenType type) {
            if (!check(type)) {
                return false;
            }
            position++;
            return true;
        }

        private Token consume(TokenType type, String message) {
            if (check(type)) {
                return tokens.get(position++);
            }
            throw new IllegalArgumentException(message + " near '" + current().text() + "'");
        }

        private boolean check(TokenType type) {
            return current().type() == type;
        }

        private Token current() {
            return tokens.get(position);
        }

        private void expectEnd() {
            if (!check(TokenType.END)) {
                throw new IllegalArgumentException("Unexpected token: " + current().text());
            }
        }
    }

    private sealed interface Node permits BinaryNode, ConditionNode, NotNode {
    }

    private record BinaryNode(String operator, Node left, Node right) implements Node {
    }

    private record ConditionNode(AccessRoleAssignmentRuleConditionDto condition) implements Node {
    }

    private record NotNode(Node child) implements Node {
    }

    private enum TokenType {
        IDENTIFIER,
        STRING,
        EQ,
        NE,
        IN,
        AND,
        OR,
        NOT,
        DOT,
        COMMA,
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        END
    }

    private record Token(TokenType type, String text) {
    }
}

