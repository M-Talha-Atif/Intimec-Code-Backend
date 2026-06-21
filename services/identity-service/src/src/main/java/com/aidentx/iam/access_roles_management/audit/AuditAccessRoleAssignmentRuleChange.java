package com.aidentx.iam.access_roles_management.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an auditable access-role assignment rule change.
 *
 * <p>The current rule audit aspect primarily uses stable execution-based
 * pointcuts so service classes remain free of audit annotations. This
 * annotation is provided to keep the rule audit package extensible and to
 * support future explicit interception without moving audit logic out of the
 * audit folder.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAccessRoleAssignmentRuleChange {

    /**
     * Declares which rule CRUD action the annotated method performs.
     *
     * @return audit action enum value
     */
    AccessRoleAssignmentRuleAuditAction action();

    /**
     * Index of the tenant ID parameter in the intercepted method signature.
     *
     * @return tenant ID parameter index
     */
    int tenantIdParamIndex() default 0;

    /**
     * Index of the rule ID parameter in the intercepted method signature.
     * Use {@code -1} when the rule ID is only available in the return value.
     *
     * @return rule ID parameter index, or {@code -1}
     */
    int ruleIdParamIndex() default 1;
}
