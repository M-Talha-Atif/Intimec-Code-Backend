package com.aidentx.iam.role_management.mapper;

import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleCreateDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleResponseDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleUpdateDto;
import com.aidentx.iam.role_management.model.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "includedEntitlements", ignore = true)
    @Mapping(target = "dgraphUid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Role createDtoToEntity(RoleCreateDto dto);

    RoleResponseDto entityToResponseDto(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "dgraphUid", ignore = true)
    @Mapping(target = "includedEntitlements", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(RoleUpdateDto dto, @MappingTarget Role entity);
}