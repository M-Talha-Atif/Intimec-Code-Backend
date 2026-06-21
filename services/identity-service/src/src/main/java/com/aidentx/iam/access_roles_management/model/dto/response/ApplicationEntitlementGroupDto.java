package com.aidentx.iam.access_roles_management.model.dto.response;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Represents a single application's entitlement group within a role.
 * Contains the application metadata and a paginated list of its entitlements
 * that are linked to the role.
 *
 * @param resourceId       the application/resource UUID
 * @param resourceName     the resolved application display name
 * @param entitlementCount total number of entitlements linked to this role for this app
 * @param highRiskCount    number of high-risk entitlements in this app for this role
 * @param entitlements     paginated entitlement details for this application
 */
public record ApplicationEntitlementGroupDto(
    UUID resourceId,
    String resourceName,
    long entitlementCount,
    long highRiskCount,
    Page<EntitlementResponseDto> entitlements
) {}

