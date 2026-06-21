package com.aidentx.iam.entitlement_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for the Entitlement Management domain.
 * Organized by concern: sync (import pipeline), response (API output),
 * update (governance edits), and member (assignment view).
 */
public final class EntitlementDtos {

    private EntitlementDtos() {} // Prevent instantiation

    /** DTO for creating or updating an entitlement from a synchronization event. */
    public record EntitlementSyncDto(
        @NotBlank String tenantId,
        @NotNull UUID resourceId,
        @NotBlank String sourceUid,
        @NotBlank String displayName,
        String description,
        String objectClass,
        String kind,
        Map<String, Object> attributes,
        String lastSyncRunId
    ) {}

    /** DTO for returning entitlement details via the API. */
    public record EntitlementResponseDto(
        UUID id,
        String tenantId,
        UUID resourceId,
        String sourceUid,
        String resourceName,
        String displayName,
        String description,
        String objectClass,
        String kind,
        Map<String, Object> attributes,
        Map<String, Object> customAttributes,
        // Governance metadata
        String lifecycle,
        String riskLevel,
        String riskReason,
        boolean requestable,
        List<String> classification,
        String identityType,
        String privilegeLevel,
        UUID ownerId,
        boolean requireApproval,
        boolean allowedAutomatedGrant,
        Long maxDurationMs,
        Long autoExpireDurationMs,
        // Computed
        long assignedUsersCount,
        // Timestamps
        Long createdAt,
        Long updatedAt
    ) {}

    /**
     * DTO for updating entitlement governance metadata.
     * Only governance fields — technical fields (sourceUid, objectClass, attributes)
     * are managed exclusively by the sync pipeline and cannot be edited manually.
     */
    public record EntitlementUpdateDto(
        String displayName,
        String description,
        String lifecycle,
        String riskLevel,
        String kind,
        String riskReason,
        Boolean requestable,
        List<String> classification,
        String identityType,
        String privilegeLevel,
        UUID ownerId,
        Boolean requireApproval,
        Boolean allowedAutomatedGrant,
        Long maxDurationMs,
        Long autoExpireDurationMs
    ) {}

    /**
     * DTO for representing a member (account) assigned to an entitlement.
     * Joins EntitlementAssignment → Shadow → Identity to produce a
     * human-readable view of "who holds this entitlement".
     *
     * <p>When the assignment originates from an access role, the role fields
     * provide the assignment path context. The {@code assignmentStatus} field
     * indicates provisioning state:</p>
     * <ul>
     *   <li>{@code ASSIGNED} — entitlement is provisioned to the account</li>
     *   <li>{@code PENDING} — identity has no account on target application yet</li>
     *   <li>{@code NOT_ASSIGNED} — account exists but entitlement not yet provisioned</li>
     * </ul>
     */
    public record EntitlementMemberDto(
        UUID assignmentId,
        UUID shadowId,
        String accountName,
        UUID identityId,
        String identityName,
        String identityRiskLevel,
        String source,
        Long assignedAt,
        Long updatedAt,
        // Role context (populated when assignment originates from an access role)
        UUID accessRoleId,
        String accessRoleName,
        String assignmentPath,
        // Provisioning status
        String assignmentStatus,
        String assignmentStatusReason
    ) {}

    /**
     * Request body for manually assigning an entitlement to one or more identities.
     *
     * <p>The service creates canonical {@code DIRECT} entitlement assignments. If an
     * identity has an account shadow on the entitlement's target application, the
     * assignment is shadow-backed and {@code ASSIGNED}. If no such account exists,
     * an identity-scoped pending assignment is created instead.</p>
     */
    public record ManualEntitlementAssignmentRequest(
        @NotEmpty List<@NotNull UUID> identityIds
    ) {}

    /**
     * Response returned after manually assigning an entitlement to identities.
     * Counts summarize created, updated/promoted, pending, and already-existing
     * assignments; {@code assignments} contains one result row per affected
     * assignment or identity-level pending row.
     */
    public record ManualEntitlementAssignmentResponse(
        UUID entitlementId,
        int requestedIdentities,
        int assignmentsCreated,
        int assignmentsUpdated,
        int pendingAssignments,
        int alreadyAssigned,
        List<ManualEntitlementAssignmentResult> assignments
    ) {}

    /** Result row for one manually assigned identity/shadow target. */
    public record ManualEntitlementAssignmentResult(
        UUID identityId,
        UUID shadowId,
        UUID assignmentId,
        String assignmentStatus,
        String assignmentStatusReason,
        String action
    ) {}

    /**
     * Desired-state request body for manual entitlement members.
     *
     * <p>The supplied identities become the complete standalone {@code DIRECT}
     * member set for the entitlement. Missing identities are assigned; current
     * standalone direct identities omitted from the list are revoked. Empty lists
     * are valid and revoke all standalone direct members for the entitlement.</p>
     */
    public record SyncManualEntitlementMembersRequest(
        @NotNull List<@NotNull UUID> identityIds
    ) {}

    /** Response returned after syncing manual entitlement members to desired state. */
    public record SyncManualEntitlementMembersResponse(
        UUID entitlementId,
        int desiredCount,
        int addedCount,
        int revokedCount,
        int skippedCount,
        int failedCount,
        int totalDirectCount,
        List<SyncEntitlementMemberResult> added,
        List<SyncEntitlementMemberResult> revoked,
        List<SyncEntitlementMemberResult> skipped,
        List<SyncEntitlementMemberFailure> failed
    ) {
        public boolean hasFailures() {
            return failed != null && !failed.isEmpty();
        }
    }

    /** One identity-level sync result with affected assignment rows. */
    public record SyncEntitlementMemberResult(
        UUID identityId,
        List<ManualEntitlementAssignmentResult> assignments
    ) {}

    /** One identity that could not be synced. */
    public record SyncEntitlementMemberFailure(
        UUID identityId,
        String reason
    ) {}

    /**
     * DTO for returning entitlement assignment details for a given shadow (account).
     */
    public record ShadowEntitlementDto(
        UUID assignmentId,
        UUID entitlementId,
        String entitlementName,
        String entitlementType,
        String objectClass,
        String riskLevel,
        String source,
        Long assignedAt
    ) {}

    /**
     * DTO for filter options returned by the filters endpoint.
     */
    public record FilterOptionDto(
        String key,
        String label,
        List<FilterValueDto> options
    ) {}

    public record FilterValueDto(
        String value,
        String label,
        long count
    ) {}
}