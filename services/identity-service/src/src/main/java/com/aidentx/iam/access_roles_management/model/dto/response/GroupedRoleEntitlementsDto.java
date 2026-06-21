package com.aidentx.iam.access_roles_management.model.dto.response;

import org.springframework.data.domain.Page;

/**
 * Top-level response DTO for role entitlements grouped by application.
 * Provides two-level pagination:
 *   - Application level: which applications are on the current page
 *   - Entitlement level: which entitlements within each application are shown
 *
 * @param totalApplications total distinct applications that have entitlements linked to this role
 * @param totalEntitlements total entitlements linked to this role across all applications
 * @param applications      paginated list of application groups, each containing paginated entitlements
 */
public record GroupedRoleEntitlementsDto(
    int totalApplications,
    long totalEntitlements,
    Page<ApplicationEntitlementGroupDto> applications
) {}

