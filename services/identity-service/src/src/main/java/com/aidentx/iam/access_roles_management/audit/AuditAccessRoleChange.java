package com.aidentx.iam.access_roles_management.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for access role audit interception.
 *
 * <p>The {@link AccessRoleAuditAspect} intercepts methods annotated with this
 * annotation, captures the before/after state of the affected role, and
 * publishes an audit event to Kafka.</p>
 *
 * <p>Parameter index fields tell the aspect which method arguments contain
 * the tenant ID and role ID. For {@code ROLE_CREATED}, the role ID does not
 * exist before the method executes, so {@code roleIdParamIndex} should be
 * set to {@code -1}; the aspect will extract the ID from the return value.</p>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * @AuditAccessRoleChange(action = AccessRoleAuditAction.ROLE_CREATED, roleIdParamIndex = -1)
 * public AccessRoleResponse createRole(String tenantId, CreateAccessRoleRequest request) { ... }
 *
 * @AuditAccessRoleChange(action = AccessRoleAuditAction.ROLE_UPDATED)
 * public AccessRoleResponse updateRole(String tenantId, UUID roleId, UpdateAccessRoleRequest request) { ... }
 *
 * @AuditAccessRoleChange(action = AccessRoleAuditAction.CHILDREN_ADDED)
 * public AddChildRolesResultDto addChildRoles(String tenantId, UUID parentRoleId, List<UUID> childRoleIds) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAccessRoleChange {

    /**
     * The audit action this method performs.
     *
     * @return the action enum value
     */
    AccessRoleAuditAction action();

    /**
     * Index of the {@code tenantId} parameter in the method signature.
     * Default is 0 (first argument).
     *
     * @return the parameter index
     */
    int tenantIdParamIndex() default 0;

    /**
     * Index of the {@code roleId} parameter in the method signature.
     * Set to {@code -1} for create operations where the role ID is not
     * available until the method returns.
     * Default is 1 (second argument).
     *
     * @return the parameter index, or -1 if extracted from return value
     */
    int roleIdParamIndex() default 1;
}