package com.aidentx.access_portal_service.dto.request.tile;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating/updating a Tile with related entities.
 */
@Getter
@Setter
public class TileRequestDTO {

    private String title;
    private String description;
    private String icon;
    private String category;
    private boolean tileActive;
    private boolean requireManagerApproval;
    private UUID workflowId; // Required when requireManagerApproval is true
    private String maximumRiskLevel;
    private boolean allowRequestForOthers;
    private boolean objectFilteringEnabled;

    private boolean allowApplications;
    private boolean allowBusinessRoles;
    private boolean allowInternalRoles;
    private boolean allowEntitlements;

    private List<TileAllowedRoleDTO> allowedRoles; // tile will show to these roles only
    private List<TileAccessScopeItemDTO> accessScopeItems;
    private List<TileVisualFilterDTO> visualFilters;
    private List<TileVisualFilterRuleDTO> visualFilterRules;
}
