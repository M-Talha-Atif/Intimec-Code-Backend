package com.aidentx.iam.entitlement_management.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic entitlement governance audit logging.
 *
 * Contract:
 *   - Parameter at index {@link #idParamIndex()} must be the entitlement UUID.
 *   - Parameter at index {@link #tenantIdParamIndex()} must be the tenant ID string.
 *   - Parameter at index {@link #updateDtoParamIndex()} must be the EntitlementUpdateDto.
 *   - The method must return an {@link com.aidentx.iam.entitlement_management.dto.EntitlementResponseDto}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditEntitlementGovernance {

    /** Index of the entitlement UUID parameter. Default: 0 */
    int idParamIndex() default 0;

    /** Index of the tenantId parameter. Default: 1 */
    int tenantIdParamIndex() default 1;

    /** Index of the EntitlementUpdateDto parameter. Default: 2 */
    int updateDtoParamIndex() default 2;
}