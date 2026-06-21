package com.aidentx.iam.identity_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Represents a single entitlement assignment as seen from an identity's perspective.
 * Combines data from three domains: assignment metadata, shadow (account) context,
 * and entitlement catalog details.
 * <p>
 * This DTO is used by the identity controller to present a unified view of
 * "all entitlements this identity holds across all accounts on all resources."
 *
 * @param assignmentId     unique ID of the entitlement assignment record
 * @param shadowId         UUID of the account (shadow) that holds this entitlement
 * @param accountName      human-readable account identifier from the target system
 *                         (e.g., "CN=jdoe,OU=Users,DC=corp" for AD, "SAP:jdoe" for SAP)
 * @param resourceId       UUID of the resource (application) this account belongs to
 * @param applicationName  display name of the resource/application (e.g., "Active Directory", "SAP ERP")
 * @param entitlementId    UUID of the entitlement in the catalog
 * @param entitlementName  display name of the entitlement (e.g., "Finance Team", "Domain Admins")
 * @param description      description of the entitlement from the catalog
 * @param kind             type of entitlement (e.g., "entitlement", "group", "role")
 * @param objectClass      ConnID object class the entitlement was imported from
 * @param riskLevel        governance risk classification (e.g., "LOW", "MEDIUM", "CRITICAL")
 * @param classification   governance labels (e.g., ["SOX", "PII"])
 * @param source           how this assignment was created: DIRECT, DYNAMIC, ROLE_DERIVED, or legacy IMPORT/MANUAL
 * @param accessPathType   DIRECT_ENTITLEMENT, ROLE_DIRECT, ROLE_INHERITED_CHILD, or ROLE_DERIVED_UNKNOWN
 * @param accessRoleId     access role that caused this role-derived entitlement assignment
 * @param accessRoleName   display/name of the assigned access role
 * @param sourceRoleId     role where the entitlement is directly attached; may be a child role
 * @param sourceRoleName   display/name of the source role where the entitlement is attached
 * @param roleEntitlementDirect whether the entitlement is directly on the assigned access role
 * @param roleEntitlementDepth 0 for direct, 1+ for inherited child-role entitlements
 * @param rolePath         assigned parent role to source child role route
 * @param assignmentStatus provisioning status for the assignment
 * @param assignmentStatusReason optional human-readable status reason
 * @param assignedAt       epoch millis when the assignment was first created
 * @param assignedByUserId user ID of the actor who directly assigned this entitlement, when known
 * @param assignedByUserName username/name of the actor who directly assigned this entitlement, when known
 * @param updatedAt        epoch millis when the assignment was last updated
 */
public record IdentityEntitlementViewDto(
        UUID assignmentId,
        UUID shadowId,
        String accountName,
        UUID resourceId,
        String applicationName,
        UUID entitlementId,
        String entitlementName,
        String description,
        String kind,
        String objectClass,
        String riskLevel,
        List<String> classification,
        String source,
        String accessPathType,
        UUID accessRoleId,
        String accessRoleName,
        UUID sourceRoleId,
        String sourceRoleName,
        Boolean roleEntitlementDirect,
        Integer roleEntitlementDepth,
        List<IdentityEntitlementRolePathNodeDto> rolePath,
        String assignmentStatus,
        String assignmentStatusReason,
        Long assignedAt,
        String assignedByUserId,
        String assignedByUserName,
        Long updatedAt
) {
}