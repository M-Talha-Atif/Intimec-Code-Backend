package com.aidentx.iam.identity_management.model.dto;

import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

/**
 * Unified response DTO for identity operations across the IAM system.
 *
 * <p>This record provides a consistent, type-safe response format for all identity-related
 * API operations. It combines standard identity metadata with flexible attribute storage
 * to accommodate different identity types (Employee, Non-Employee, Non-Human) while
 * maintaining a uniform API contract.</p>
 *
 * <h2>Structure:</h2>
 * <ul>
 *   <li><strong>Standard Fields:</strong> id, tenantId, identityType, status, timestamps</li>
 *   <li><strong>Core Attributes:</strong> Type-specific attributes (username, email, employeeId, etc.)</li>
 *   <li><strong>Extended Attributes:</strong> Custom attributes defined per tenant/identity type</li>
 * </ul>
 *
 * <h2>Identity Types:</h2>
 * <ul>
 *   <li><strong>EMPLOYEE:</strong> Core attributes include username, email, employeeId, jobTitle, etc.</li>
 *   <li><strong>NON_EMPLOYEE:</strong> Core attributes include username, email, contractId, vendorName, etc.</li>
 *   <li><strong>NON_HUMAN:</strong> Core attributes include serviceName, ownerTeam, description, etc.</li>
 * </ul>
 *
 * @param id Unique identifier for the identity (UUID)
 * @param dgraphNodeId Optional Dgraph node identifier for graph database integration
 * @param tenantId Tenant identifier for multi-tenancy isolation
 * @param identityType Type of identity (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN)
 * @param status Current status of the identity (ACTIVE, INACTIVE, SUSPENDED, etc.)
 * @param riskLevel Identity risk level (low, medium, high), when available
 * @param totalAccess Total entitlement assignment row count for the identity, including pending and duplicates
 * @param coreAttributes Map of type-specific core attributes (required fields for the identity type)
 * @param extendedAttributes Map of custom extended attributes (optional, tenant-defined)
 * @param createdAt Timestamp when the identity was first created
 * @param updatedAt Timestamp when the identity was last modified
 *
 * @since 1.0
 * @see IdentityStatus
 */
public record IdentityResponseDto(
        UUID id, String dgraphNodeId,
        String tenantId,
        String identityType,
        IdentityStatus status,
        String riskLevel,
        Long totalAccess,
        Map<String, Object> coreAttributes,
        Map<String, Object> extendedAttributes,
        java.time.Instant createdAt,
        java.time.Instant updatedAt) {
}