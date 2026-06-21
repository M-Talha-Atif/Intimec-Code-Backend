package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleQueryService;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.entitlement_management.util.LikePatternEscaper;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Criteria-based, read-only query service for access-role rule matching and evidence.
 */
@Service
public class AccessRoleAssignmentRuleQueryServiceImpl implements AccessRoleAssignmentRuleQueryService {

    private static final int QUERY_CHUNK_SIZE = 500;

    private final EntityManager entityManager;
    private final IdentityFieldRegistry fieldRegistry;
    private final AccessRoleAssignmentRuleNormalizationService normalizationService;

    public AccessRoleAssignmentRuleQueryServiceImpl(EntityManager entityManager,
                                                    IdentityFieldRegistry fieldRegistry,
                                                    AccessRoleAssignmentRuleNormalizationService normalizationService) {
        this.entityManager = entityManager;
        this.fieldRegistry = fieldRegistry;
        this.normalizationService = normalizationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findAllMatchingIdentityIds(String tenantId,
                                                List<String> identityTypes,
                                                List<Map<String, Object>> conditionGroups) {
        validateTenantId(tenantId);
        Set<UUID> matched = new LinkedHashSet<>();
        for (Class<? extends Identity> identityClass : normalizationService.resolveIdentityClasses(identityTypes)) {
            matched.addAll(findMatchingIdentityIds(tenantId, identityClass, conditionGroups));
        }
        return matched;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, IdentitySnapshot> loadIdentitySnapshots(String tenantId,
                                                             Collection<UUID> identityIds,
                                                             List<String> identityTypes) {
        validateTenantId(tenantId);
        if (identityIds == null || identityIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, IdentitySnapshot> snapshots = new LinkedHashMap<>();
        List<UUID> ids = List.copyOf(new LinkedHashSet<>(identityIds));
        for (Class<? extends Identity> identityClass : normalizationService.resolveIdentityClasses(identityTypes)) {
            snapshots.putAll(loadIdentitySnapshotsForClass(tenantId, ids, identityClass));
        }
        return snapshots;
    }

    @Override
    public Map<String, Object> buildEvidence(IdentitySnapshot snapshot,
                                             List<Map<String, Object>> conditionGroups,
                                             boolean matched,
                                             String errorMessage) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("overallMatch", matched);
        evidence.put("identityType", snapshot != null ? snapshot.identityType() : null);
        evidence.put("fields", snapshot != null ? snapshot.fieldValues() : Map.of());
        evidence.put("conditionGroups", snapshot != null
                ? buildGroupEvidence(conditionGroups, snapshot.fieldValues(), resolveIdentityClass(snapshot.identityType()))
                : List.of());
        if (errorMessage != null) {
            evidence.put("error", errorMessage);
        }
        return evidence;
    }

    private List<UUID> findMatchingIdentityIds(String tenantId,
                                               Class<? extends Identity> identityClass,
                                               List<Map<String, Object>> conditionGroups) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UUID> query = cb.createQuery(UUID.class);
        Root<? extends Identity> root = query.from(identityClass);
        query.select(root.get("id"));
        query.where(buildWhereClause(cb, root, tenantId, identityClass, conditionGroups));

        List<UUID> result = new ArrayList<>();
        int offset = 0;
        List<UUID> page;
        do {
            page = entityManager.createQuery(query)
                    .setFirstResult(offset)
                    .setMaxResults(QUERY_CHUNK_SIZE)
                    .getResultList();
            result.addAll(page);
            offset += QUERY_CHUNK_SIZE;
        } while (page.size() == QUERY_CHUNK_SIZE);
        return result;
    }

    private Predicate buildWhereClause(CriteriaBuilder cb,
                                       Root<? extends Identity> root,
                                       String tenantId,
                                       Class<? extends Identity> identityClass,
                                       List<Map<String, Object>> conditionGroups) {
        return cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("status"), IdentityStatus.ACTIVE),
                buildGroupedPredicate(cb, root, identityClass, conditionGroups));
    }

    private Predicate buildGroupedPredicate(CriteriaBuilder cb,
                                            Root<? extends Identity> root,
                                            Class<? extends Identity> identityClass,
                                            List<Map<String, Object>> conditionGroups) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            return cb.conjunction();
        }
        Predicate accumulator = buildSingleGroupPredicate(cb, root, identityClass, conditionGroups.get(0));
        for (int i = 1; i < conditionGroups.size(); i++) {
            Predicate current = buildSingleGroupPredicate(cb, root, identityClass, conditionGroups.get(i));
            String interGroupOperator = normalizeLogicalOperator(conditionGroups.get(i - 1).get("interGroupOperator"), AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR);
            accumulator = "OR".equals(interGroupOperator) ? cb.or(accumulator, current) : cb.and(accumulator, current);
        }
        return accumulator;
    }

    private Predicate buildSingleGroupPredicate(CriteriaBuilder cb,
                                                Root<? extends Identity> root,
                                                Class<? extends Identity> identityClass,
                                                Map<String, Object> group) {
        List<Predicate> predicates = new ArrayList<>();
        for (Map<String, Object> condition : extractMapList(group.get("conditions"))) {
            predicates.add(buildSingleConditionPredicate(cb, root, identityClass, condition));
        }
        List<Map<String, Object>> subGroups = extractMapList(group.get("conditionGroups"));
        if (!subGroups.isEmpty()) {
            predicates.add(buildSingleGroupPredicate(cb, root, identityClass, subGroups.get(0)));
            for (int i = 1; i < subGroups.size(); i++) {
                Predicate previous = predicates.remove(predicates.size() - 1);
                Predicate current = buildSingleGroupPredicate(cb, root, identityClass, subGroups.get(i));
                String interGroupOperator = normalizeLogicalOperator(subGroups.get(i - 1).get("interGroupOperator"), AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR);
                predicates.add("OR".equals(interGroupOperator) ? cb.or(previous, current) : cb.and(previous, current));
            }
        }
        if (predicates.isEmpty()) {
            return Boolean.TRUE.equals(group.get("negated")) ? cb.disjunction() : cb.conjunction();
        }
        String groupOperator = normalizeLogicalOperator(group.get("groupOperator"), AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR);
        Predicate combined = "OR".equals(groupOperator)
                ? cb.or(predicates.toArray(new Predicate[0]))
                : cb.and(predicates.toArray(new Predicate[0]));
        return Boolean.TRUE.equals(group.get("negated")) ? cb.not(combined) : combined;
    }

    private Predicate buildSingleConditionPredicate(CriteriaBuilder cb,
                                                    Root<? extends Identity> root,
                                                    Class<? extends Identity> identityClass,
                                                    Map<String, Object> condition) {
        String rawField = stringValue(condition.get("field"));
        RuleOperator operator = RuleOperator.fromString(stringValue(condition.get("operator")));
        String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
        return buildFieldPredicate(cb, root, resolvedField, operator, condition.get("value"), condition.get("values"));
    }

    private Predicate buildFieldPredicate(CriteriaBuilder cb,
                                          Root<?> root,
                                          String field,
                                          RuleOperator operator,
                                          Object value,
                                          Object values) {
        Expression<String> path = root.get(field).as(String.class);
        String strValue = value != null ? value.toString() : "";
        return switch (operator) {
            case EQUALS -> cb.equal(cb.upper(path), strValue.toUpperCase(Locale.ROOT));
            case NOT_EQUALS -> cb.notEqual(cb.upper(path), strValue.toUpperCase(Locale.ROOT));
            case CONTAINS -> cb.like(cb.upper(path), "%" + LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT) + "%", '\\');
            case STARTS_WITH -> cb.like(cb.upper(path), LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT) + "%", '\\');
            case ENDS_WITH -> cb.like(cb.upper(path), "%" + LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT), '\\');
            case IN -> cb.upper(path).in(resolveListValues(value, values).stream().map(item -> item.toUpperCase(Locale.ROOT)).toList());
            case NOT_IN -> cb.not(cb.upper(path).in(resolveListValues(value, values).stream().map(item -> item.toUpperCase(Locale.ROOT)).toList()));
        };
    }

    private Map<UUID, IdentitySnapshot> loadIdentitySnapshotsForClass(String tenantId,
                                                                      List<UUID> identityIds,
                                                                      Class<? extends Identity> identityClass) {
        if (identityIds.isEmpty()) {
            return Map.of();
        }
        List<String> fields = fieldRegistry.getFieldsForClass(identityClass);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<? extends Identity> root = query.from(identityClass);

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root.get("id").alias("id"));
        for (String field : fields) {
            try {
                selections.add(root.get(field).alias(field));
            } catch (IllegalArgumentException ignored) {
            }
        }
        query.multiselect(selections);
        query.where(cb.equal(root.get("tenantId"), tenantId), root.get("id").in(identityIds));

        Map<UUID, IdentitySnapshot> snapshots = new LinkedHashMap<>();
        for (Tuple tuple : entityManager.createQuery(query).getResultList()) {
            UUID id = tuple.get("id", UUID.class);
            Map<String, Object> fieldValues = new LinkedHashMap<>();
            for (String field : fields) {
                try {
                    Object value = tuple.get(field);
                    if (value != null) {
                        fieldValues.put(field, value.toString());
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            snapshots.put(id, new IdentitySnapshot(id, resolveDisplayName(fieldValues), resolveIdentityType(identityClass), fieldValues));
        }
        return snapshots;
    }

    private List<Map<String, Object>> buildGroupEvidence(List<Map<String, Object>> conditionGroups,
                                                         Map<String, Object> fieldValues,
                                                         Class<? extends Identity> identityClass) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (int i = 0; i < conditionGroups.size(); i++) {
            evidence.add(buildSingleGroupEvidence(conditionGroups.get(i), i, fieldValues, identityClass));
        }
        return evidence;
    }

    private Map<String, Object> buildSingleGroupEvidence(Map<String, Object> group,
                                                         int index,
                                                         Map<String, Object> fieldValues,
                                                         Class<? extends Identity> identityClass) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("groupIndex", index);
        evidence.put("groupOperator", normalizeLogicalOperator(group.get("groupOperator"), AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR));
        evidence.put("interGroupOperator", normalizeLogicalOperator(group.get("interGroupOperator"), AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR));
        evidence.put("negated", Boolean.TRUE.equals(group.get("negated")));
        List<Map<String, Object>> conditionEvidence = new ArrayList<>();
        for (Map<String, Object> condition : extractMapList(group.get("conditions"))) {
            conditionEvidence.add(buildConditionEvidence(condition, fieldValues, identityClass));
        }
        evidence.put("conditions", conditionEvidence);
        List<Map<String, Object>> subGroupEvidence = new ArrayList<>();
        int childIndex = 0;
        for (Map<String, Object> subGroup : extractMapList(group.get("conditionGroups"))) {
            subGroupEvidence.add(buildSingleGroupEvidence(subGroup, childIndex++, fieldValues, identityClass));
        }
        if (!subGroupEvidence.isEmpty()) {
            evidence.put("conditionGroups", subGroupEvidence);
        }
        return evidence;
    }

    private Map<String, Object> buildConditionEvidence(Map<String, Object> condition,
                                                       Map<String, Object> fieldValues,
                                                       Class<? extends Identity> identityClass) {
        String resolvedField = fieldRegistry.resolveField(stringValue(condition.get("field")), identityClass);
        String operator = stringValue(condition.get("operator"));
        Object actualValue = fieldValues.get(resolvedField);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("field", resolvedField);
        evidence.put("operator", operator);
        evidence.put("expectedValue", condition.get("values") != null ? condition.get("values") : condition.get("value"));
        evidence.put("actualValue", actualValue);
        evidence.put("matched", evaluateConditionInMemory(actualValue, operator, condition.get("value"), condition.get("values")));
        return evidence;
    }

    private boolean evaluateConditionInMemory(Object actualValue, String operator, Object expectedValue, Object expectedValues) {
        if (actualValue == null) {
            return "NOT_EQUALS".equalsIgnoreCase(operator) || "NOT_IN".equalsIgnoreCase(operator);
        }
        String actual = actualValue.toString().toUpperCase(Locale.ROOT);
        String expected = expectedValue != null ? expectedValue.toString().toUpperCase(Locale.ROOT) : "";
        return switch (operator.toUpperCase(Locale.ROOT)) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "CONTAINS" -> actual.contains(expected);
            case "STARTS_WITH" -> actual.startsWith(expected);
            case "ENDS_WITH" -> actual.endsWith(expected);
            case "IN" -> resolveListValues(expectedValue, expectedValues).stream().map(item -> item.toUpperCase(Locale.ROOT)).anyMatch(actual::equals);
            case "NOT_IN" -> resolveListValues(expectedValue, expectedValues).stream().map(item -> item.toUpperCase(Locale.ROOT)).noneMatch(actual::equals);
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMapList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        return maps;
    }

    private List<String> resolveListValues(Object value, Object values) {
        if (values instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        if (value != null) {
            return Arrays.stream(value.toString().split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
        }
        return List.of();
    }

    private String resolveDisplayName(Map<String, Object> fieldValues) {
        Object displayName = fieldValues.get("displayName");
        if (displayName != null && !displayName.toString().isBlank()) {
            return displayName.toString();
        }
        Object firstName = fieldValues.get("firstName");
        Object lastName = fieldValues.get("lastName");
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        Object username = fieldValues.get("username");
        if (username != null) {
            return username.toString();
        }
        Object serviceName = fieldValues.get("serviceName");
        return serviceName != null ? serviceName.toString() : "Unknown";
    }

    private String resolveIdentityType(Class<? extends Identity> identityClass) {
        if (Employee.class.isAssignableFrom(identityClass)) return "EMPLOYEE";
        if (NonEmployee.class.isAssignableFrom(identityClass)) return "NON_EMPLOYEE";
        if (NonHumanIdentity.class.isAssignableFrom(identityClass)) return "NON_HUMAN";
        return "IDENTITY";
    }

    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        return switch (identityType) {
            case "EMPLOYEE" -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN" -> NonHumanIdentity.class;
            default -> Identity.class;
        };
    }

    private String normalizeLogicalOperator(Object value, String defaultValue) {
        String operator = value == null || value.toString().isBlank()
                ? defaultValue
                : value.toString().trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(operator) && !"OR".equals(operator)) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_GROUP_OPERATOR + ": " + value);
        }
        return operator;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }
}

