package com.aidentx.access_portal_service.dto.request.tile;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for allowed roles in TileRequestDTO
 */
@Getter
@Setter
public class TileAllowedRoleDTO {
    private UUID roleCode;
}
