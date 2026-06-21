package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitDto;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.model.entity.OrgUnitDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps {@link OrgUnit} entities to {@link OrgUnitDto}.
 *
 * <p>Aggregate counts are supplied via an {@link OrgAggregateService.OrgAggregates}
 * value object, keeping the mapper stateless and the call sites clean.
 * Children and delegates are passed as already-mapped lists.
 */
@Component
public class OrgUnitMapper {

    public OrgUnitDto toDto(
            OrgUnit u,
            OrgAggregateService.OrgAggregates agg,
            List<OrgDelegateDto> delegates,
            List<OrgUnitDto> children) {

        if (u == null) return null;

        return new OrgUnitDto(
                u.getId(),
                u.getTenantId(),
                u.getName(),
                u.getType(),
                u.getHierarchyType(),
                u.getCode(),
                u.getDescription(),
                u.getParentId(),
                u.getPath(),
                u.getDepth(),
                u.getStatus(),

                u.getHeadDisplayName(),
                u.getHeadIdentityId(),

                agg.userCount(),
                agg.roleCount(),          // Phase 4 — 0 until role-org scope is built
                agg.adminCount(),         // Phase 4 — 0 until admin_org_scope is built
                null,                     // applicationCount    — Phase 4
                agg.privilegedCount(),    // NON_HUMAN identities (service accounts)
                agg.contractorCount(),    // NON_EMPLOYEE identities (contractors/vendors)
                null,                     // vendorCount — Phase 4 (needs vendor_name join)

                null,   // riskLevel    — Phase 4
                null,   // hasStaleRoles — Phase 4

                u.getCostCenter(),
                u.getLegalEntity(),
                u.getRegion(),
                u.getCountry(),
                u.getTimeZone(),
                u.getCurrency(),
                u.getLocation(),
                u.getBuildingCode(),
                u.getFloor(),
                u.getDataClassification(),
                u.getComplianceStatus(),
                u.getCertificationStatus(),
                u.getLastAuditDate(),
                u.getNextReviewDate(),

                u.getBudget(),
                u.getActualSpend(),
                u.getLicenseCost(),

                null,   // riskScore              — Phase 4
                null,   // healthScore            — Phase 4
                null,   // sodViolationCount      — Phase 4
                null,   // orphanAccountCount     — Phase 4
                null,   // dormantAccountCount    — Phase 4
                null,   // overProvisionedUserCount — Phase 4

                delegates,

                u.getTags(),
                u.getMetadata(),
                u.getCreatedBy(),
                u.getCreatedAt(),
                u.getUpdatedBy(),
                u.getUpdatedAt(),

                children
        );
    }

    public OrgDelegateDto toDelegateDto(OrgUnitDelegate d) {
        if (d == null) return null;
        return new OrgDelegateDto(
                d.getId(),
                d.getIdentityId(),
                d.getIdentityDisplayName(),
                d.getDelegateRole(),
                d.getAssignedAt(),
                d.getExpiresAt()
        );
    }
}
