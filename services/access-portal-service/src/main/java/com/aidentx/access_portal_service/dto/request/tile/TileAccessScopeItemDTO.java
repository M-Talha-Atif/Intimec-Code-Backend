package com.aidentx.access_portal_service.dto.request.tile;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for access scope items (application, business role, internal role)
 */
@Getter
@Setter
public class TileAccessScopeItemDTO {
private String itemType; // APPLICATION, BUSINESS_ROLE, INTERNAL_ROLE, ENTITLEMENT

    private UUID itemCode; // application id, entitlement id, internal role id

}
