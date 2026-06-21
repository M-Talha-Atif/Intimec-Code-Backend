package com.aidentx.access_portal_service.audit;


import com.aidentx.access_portal_service.entity.tile.Tile;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simplified audit representation of a Tile to prevent circular references.
 * Only includes essential fields needed for audit logging.
 */
@Data
@Builder
public class TileAuditDTO {
    private UUID id;
    private String title;
    private String description;
    private String icon;
    private String category;
    private boolean tileActive;
    private boolean requireManagerApproval;
    private String maximumRiskLevel;
    private boolean allowRequestForOthers;
    private boolean allowJustificationForOthers;
    private boolean objectFilteringEnabled;
    private boolean allowApplications;
    private boolean allowBusinessRoles;
    private boolean allowInternalRoles;

    // Simplified child collections - only IDs to prevent recursion
    private Set<UUID> allowedRoleCodes;
    private int accessScopeItemCount;
    private int visualFilterCount;

    /**
     * Converts a Tile entity to an audit-safe DTO.
     */
    public static TileAuditDTO from(Tile tile) {
        return TileAuditDTO.builder()
                .id(tile.getId())
                .title(tile.getTitle())
                .description(tile.getDescription())
                .icon(tile.getIcon())
                .tileActive(tile.isTileActive())
                .category(tile.getCategory())
                .requireManagerApproval(tile.isRequireManagerApproval())
                .maximumRiskLevel(tile.getMaximumRiskLevel())
                .allowRequestForOthers(tile.isAllowRequestForOthers())
                .objectFilteringEnabled(tile.isObjectFilteringEnabled())
                .allowApplications(tile.isAllowApplications())
                .allowBusinessRoles(tile.isAllowBusinessRoles())
                .allowInternalRoles(tile.isAllowInternalRoles())
                .allowedRoleCodes(
                        tile.getAllowedRoles().stream()
                                .map(role -> role.getRoleCode())
                                .collect(Collectors.toSet())
                )
                .accessScopeItemCount(tile.getAccessScopeItems().size())
                .visualFilterCount(tile.getVisualFilters().size())
                .build();
    }
}