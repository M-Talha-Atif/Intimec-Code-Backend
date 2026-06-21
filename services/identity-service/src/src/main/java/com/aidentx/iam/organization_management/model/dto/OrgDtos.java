package com.aidentx.iam.organization_management.model.dto;

import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.model.enums.DelegateRole;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgEventType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for the Organization Management domain. Field names and JSON
 * shapes are aligned with the frontend type {@code OrgNodeData} in
 * {@code intimec-development-frontend/src/types/organization.ts}.
 */
public final class OrgDtos {

    private OrgDtos() {}

    /** Request payload for creating a new {@code OrgUnit}. */
    public record OrgUnitCreateDto(
        @NotBlank String name,
        @NotNull OrgUnitType type,
        UUID parentId,
        HierarchyType hierarchyType,
        String code,
        String description,
        UUID headIdentityId,

        String costCenter,
        String legalEntity,
        String region,
        String country,
        String timeZone,
        String currency,
        String location,
        String buildingCode,
        String floor,
        String dataClassification,
        String complianceStatus,
        String certificationStatus,
        LocalDate lastAuditDate,
        LocalDate nextReviewDate,

        BigDecimal budget,
        BigDecimal actualSpend,
        BigDecimal licenseCost,

        List<String> tags,
        Map<String, Object> metadata,

        String externalId,
        String sourceSystem
    ) {}

    /** Partial update — every field is optional, {@code null} = unchanged. */
    public record OrgUnitUpdateDto(
        String name,
        OrgUnitType type,
        String code,
        String description,
        UUID headIdentityId,
        OrgUnitStatus status,

        String costCenter,
        String legalEntity,
        String region,
        String country,
        String timeZone,
        String currency,
        String location,
        String buildingCode,
        String floor,
        String dataClassification,
        String complianceStatus,
        String certificationStatus,
        LocalDate lastAuditDate,
        LocalDate nextReviewDate,

        BigDecimal budget,
        BigDecimal actualSpend,
        BigDecimal licenseCost,

        List<String> tags,
        Map<String, Object> metadata
    ) {}

    /**
     * Detail / tree node response. Superset of frontend {@code OrgNodeData}.
     * {@code children} is populated only by tree endpoints; flat endpoints
     * leave it null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrgUnitDto(
        UUID id,
        String tenantId,
        String name,
        OrgUnitType type,
        HierarchyType hierarchyType,
        String code,
        String description,
        UUID parentId,
        String path,
        Integer depth,
        OrgUnitStatus status,

        String head,
        UUID headIdentityId,

        // Computed aggregates
        long userCount,
        long roleCount,
        long adminCount,
        Long applicationCount,
        Long privilegedUserCount,
        Long contractorCount,
        Long vendorCount,

        String riskLevel,
        Boolean hasStaleRoles,

        // Governance
        String costCenter,
        String legalEntity,
        String region,
        String country,
        String timeZone,
        String currency,
        String location,
        String buildingCode,
        String floor,
        String dataClassification,
        String complianceStatus,
        String certificationStatus,
        LocalDate lastAuditDate,
        LocalDate nextReviewDate,

        // Financial
        BigDecimal budget,
        BigDecimal actualSpend,
        BigDecimal licenseCost,

        // Risk derivatives (Phase 4 — null for now)
        Integer riskScore,
        Integer healthScore,
        Long sodViolationCount,
        Long orphanAccountCount,
        Long dormantAccountCount,
        Long overProvisionedUserCount,

        // Delegates (only on detail endpoints; null on tree)
        List<OrgDelegateDto> delegates,

        // Metadata
        List<String> tags,
        Map<String, Object> metadata,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,

        // Tree only
        List<OrgUnitDto> children
    ) {}

    /** Aggregate counts for a unit (recursive over its subtree). */
    public record OrgAggregatesDto(
        UUID orgUnitId,
        long userCount,
        long roleCount,
        long adminCount,
        long applicationCount,
        long privilegedUserCount,
        long contractorCount,
        long vendorCount
    ) {}

    public record OrgDelegateDto(
        UUID id,
        UUID identityId,
        String name,
        DelegateRole role,
        Instant assignedAt,
        Instant expiresAt
    ) {}

    public record OrgDelegateCreateDto(
        @NotNull UUID identityId,
        String name,
        @NotNull DelegateRole role,
        Instant expiresAt
    ) {}

    public record OrgAssignmentDto(
        UUID id,
        UUID identityId,
        String identityDisplayName,
        UUID orgUnitId,
        String orgUnitName,
        HierarchyType hierarchyType,
        AssignmentType assignmentType,
        boolean primary,
        Instant validFrom,
        Instant validTo
    ) {}

    public record OrgAssignmentCreateDto(
        @NotNull UUID identityId,
        HierarchyType hierarchyType,
        AssignmentType assignmentType,
        Boolean primary,
        Instant validFrom,
        Instant validTo
    ) {}

    public record OrgMoveDto(
        UUID newParentId
    ) {}

    public record OrgHistoryDto(
        Long id,
        UUID orgUnitId,
        OrgEventType eventType,
        String action,
        String details,
        UUID actorIdentityId,
        String actorUsername,
        Instant occurredAt,
        JsonNode before,
        JsonNode after
    ) {}

    // ====================================================================
    // Bulk import
    // ====================================================================

    /**
     * One row of an org-structure CSV import. {@code externalId} is the
     * client-side stable key used to link rows to their parent in the same
     * file; it is also persisted to {@link OrgUnitDto#tenantId()} via
     * {@code externalId}/{@code sourceSystem="IMPORT"} for traceability.
     */
    public record BulkImportItem(
        @NotBlank String externalId,
        String parentExternalId,
        @NotBlank String name,
        @NotNull OrgUnitType type,
        HierarchyType hierarchyType,
        OrgUnitStatus status,
        String code,
        String description,
        /** Employee-ID of the unit head/manager; resolved to identity UUID at import time. */
        String headExternalId,
        String costCenter,
        String location,
        String region,
        String country
    ) {}

    /** Bulk import request body. */
    public record BulkImportRequest(
        UUID rootParentId,           // optional: re-root the imported tree under an existing unit
        HierarchyType defaultHierarchyType,
        @NotNull List<BulkImportItem> items
    ) {}

    /** Per-row outcome of a bulk import. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BulkImportRowResult(
        String externalId,
        String action,               // "created" | "validated" | "failed"
        UUID id,
        String error
    ) {}

    /** Aggregate response from a bulk import (or a dry-run validation). */
    public record BulkImportResponse(
        boolean dryRun,
        int totalRows,
        int created,
        int validated,
        int failed,
        List<BulkImportRowResult> results
    ) {}
}
