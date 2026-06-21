package com.aidentx.iam.entitlement_management.service;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Registry that discovers queryable fields from Identity entity classes
 * at application startup using reflection.
 *
 * <p>Replaces hardcoded field allowlists. When a new field is added to
 * any Identity subclass, it is automatically available for rule conditions
 * without code changes.
 *
 * <p>Supports case-insensitive field resolution — clients can send
 * "jobtitle", "jobTitle", "JOBTITLE" and all resolve to the correct
 * JPA field name "jobTitle".
 *
 * <p>Thread-safe: built once at startup, read-only thereafter.
 */
@Component
public class IdentityFieldRegistry {

    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "id", "tenantId", "dgraphNodeId", "createdAt", "updatedAt",
            "passwordHash", "manager", "sponsor",
            "serialVersionUID"
    );

    /**
     * Maps identity class → set of allowed field names (exact camelCase).
     */
    private final Map<Class<? extends Identity>, Set<String>> fieldCache = new ConcurrentHashMap<>();

    /**
     * Maps identity class → (lowercased field name → exact camelCase field name).
     * Used for case-insensitive resolution.
     */
    private final Map<Class<? extends Identity>, Map<String, String>> caseInsensitiveCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerClass(Identity.class);
        registerClass(Employee.class);
        registerClass(NonEmployee.class);
        registerClass(NonHumanIdentity.class);
    }

    private void registerClass(Class<? extends Identity> clazz) {
        Set<String> fields = new LinkedHashSet<>();
        Map<String, String> caseMap = new HashMap<>();
        Class<?> current = clazz;

        while (current != null && Identity.class.isAssignableFrom(current)) {
            for (Field field : current.getDeclaredFields()) {
                if (isQueryableField(field)) {
                    fields.add(field.getName());
                    caseMap.put(field.getName().toLowerCase(), field.getName());
                }
            }
            current = current.getSuperclass();
        }

        fieldCache.put(clazz, Collections.unmodifiableSet(fields));
        caseInsensitiveCache.put(clazz, Collections.unmodifiableMap(caseMap));
    }

    private boolean isQueryableField(Field field) {
        if (EXCLUDED_FIELDS.contains(field.getName())) {
            return false;
        }
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        Class<?> type = field.getType();
        return type == String.class || type.isEnum();
    }

    /**
     * Returns the set of allowed field names for a given Identity subclass.
     *
     * @param identityClass the Identity subclass to check
     * @return unmodifiable set of queryable field names in exact camelCase
     */
    public Set<String> getAllowedFields(Class<? extends Identity> identityClass) {
        Set<String> fields = fieldCache.get(identityClass);
        if (fields == null) {
            throw new IllegalStateException(
                    "Identity class not registered in field registry: " + identityClass.getSimpleName());
        }
        return fields;
    }

    /**
     * Resolves a user-provided field name to the exact camelCase JPA field name.
     *
     * <p>Case-insensitive: "jobtitle", "jobTitle", "JOBTITLE" all resolve to "jobTitle".
     *
     * @param field         the user-provided field name (any casing)
     * @param identityClass the Identity subclass being queried
     * @return the exact camelCase field name as declared in the entity
     * @throws IllegalArgumentException if the field cannot be resolved
     */
    public String resolveField(String field, Class<? extends Identity> identityClass) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_FIELD_UNRESOLVED + ": (blank)");
        }

        Map<String, String> caseMap = caseInsensitiveCache.get(identityClass);
        if (caseMap == null) {
            throw new IllegalStateException(
                    "Identity class not registered in field registry: " + identityClass.getSimpleName());
        }

        String resolved = caseMap.get(field.toLowerCase());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "Field '" + field + "' is not allowed for identity type " + identityClass.getSimpleName()
                            + ". Allowed fields: " + getAllowedFields(identityClass));
        }
        return resolved;
    }

    /**
     * Returns the list of all registered field names for a given identity class.
     *
     * <p>Used by the evaluation engine to load identity snapshots with all
     * relevant fields for evidence building.
     *
     * @param identityClass the JPA entity class
     * @return list of field names registered for this class
     */
    public List<String> getFieldsForClass(Class<? extends Identity> identityClass) {
        return List.copyOf(getAllowedFields(identityClass));
    }

    /**
     * Validates that a field name is allowed for the given Identity subclass.
     * Case-insensitive.
     *
     * @param field         the field name from the rule condition
     * @param identityClass the Identity subclass being queried
     * @throws IllegalArgumentException if the field is not allowed
     */
    public void validateField(String field, Class<? extends Identity> identityClass) {
        resolveField(field, identityClass);
    }
}