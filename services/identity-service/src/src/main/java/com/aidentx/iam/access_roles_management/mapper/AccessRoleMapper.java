package com.aidentx.iam.access_roles_management.mapper;

import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link AccessRole} entity and its DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AccessRoleMapper {

    /**
     * Maps a create request DTO to a new {@link AccessRole} entity.
     * Auto-generated fields (id, timestamps) and relationship fields are ignored.
     *
     * @param request the create request DTO
     * @return a new AccessRole entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AccessRole createRequestToEntity(CreateAccessRoleRequest request);

    /**
     * Updates an existing {@link AccessRole} entity from an update request DTO.
     * Only non-null fields in the DTO are applied to the entity.
     *
     * @param request the update request DTO
     * @param entity  the existing entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateAccessRoleRequest request, @MappingTarget AccessRole entity);

    /**
     * Maps an {@link AccessRole} entity to a response DTO.
     *
     * @param entity the access role entity
     * @return the response DTO
     */
    @Mapping(target = "organizationName", ignore = true)
    AccessRoleResponse entityToResponse(AccessRole entity);
}

