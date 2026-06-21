package com.aidentx.access_portal_service.dto.response.tile;

import com.aidentx.access_portal_service.dto.request.tile.TileAccessScopeItemDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileVisualFilterDTO;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for returning Tile data with all related entities
 */
@Getter
@Setter
public class TileResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private String icon;
    private boolean tileActive;
    private boolean requireManagerApproval;
    private UUID workflowId;

    private String maximumRiskLevel;
    private boolean allowRequestForOthers;
    private boolean objectFilteringEnabled;

    private boolean allowApplications;
    private boolean allowBusinessRoles;
    private boolean allowInternalRoles;
    private boolean allowEntitlements;


    private String tenantId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Integer itemCount;

    private Double approvalRate; // per items

    private Set<UUID> allowedRoleCodes;
    private Set<TileAccessScopeItemDTO> accessScopeItems;
    private List<TileVisualFilterDTO> visualFilters;
}
