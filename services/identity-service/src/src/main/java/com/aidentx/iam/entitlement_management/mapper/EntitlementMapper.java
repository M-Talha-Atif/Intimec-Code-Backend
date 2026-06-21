package com.aidentx.iam.entitlement_management.mapper;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementSyncDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementUpdateDto;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EntitlementMapper {

    // Maps the DTO from a sync event to a new Entitlement entity.
    // Governance fields get defaults from entity field initializers.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "dgraphUid", ignore = true)
    @Mapping(target = "lifecycle", ignore = true)
    @Mapping(target = "riskLevel", ignore = true)
    @Mapping(target = "riskReason", ignore = true)
    @Mapping(target = "requestable", ignore = true)
    @Mapping(target = "classification", ignore = true)
    @Mapping(target = "identityType", ignore = true)
    @Mapping(target = "privilegeLevel", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "requireApproval", ignore = true)
    @Mapping(target = "allowedAutomatedGrant", ignore = true)
    @Mapping(target = "maxDurationMs", ignore = true)
    @Mapping(target = "autoExpireDurationMs", ignore = true)
    Entitlement syncDtoToEntity(EntitlementSyncDto dto);

    // Updates an existing entity from a sync DTO.
    // Ignores natural key fields and ALL governance fields —
    // sync never overwrites admin-managed governance metadata.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "sourceUid", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "dgraphUid", ignore = true)
    @Mapping(target = "lastSyncRunId", ignore = true)
    @Mapping(target = "lifecycle", ignore = true)
    @Mapping(target = "riskLevel", ignore = true)
    @Mapping(target = "riskReason", ignore = true)
    @Mapping(target = "requestable", ignore = true)
    @Mapping(target = "classification", ignore = true)
    @Mapping(target = "identityType", ignore = true)
    @Mapping(target = "privilegeLevel", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "requireApproval", ignore = true)
    @Mapping(target = "allowedAutomatedGrant", ignore = true)
    @Mapping(target = "maxDurationMs", ignore = true)
    @Mapping(target = "autoExpireDurationMs", ignore = true)
    void updateEntityFromSyncDto(EntitlementSyncDto dto, @MappingTarget Entitlement entity);

    // Updates governance metadata from an admin update DTO.
    // Only governance fields are mapped — technical/sync fields are never touched.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "sourceUid", ignore = true)
    @Mapping(target = "objectClass", ignore = true)
//    @Mapping(target = "kind", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "lastSyncRunId", ignore = true)
    @Mapping(target = "dgraphUid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromGovernanceDto(EntitlementUpdateDto dto, @MappingTarget Entitlement entity);
}