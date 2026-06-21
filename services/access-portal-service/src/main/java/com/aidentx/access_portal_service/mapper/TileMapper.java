package com.aidentx.access_portal_service.mapper;

import com.aidentx.access_portal_service.dto.request.tile.*;
import com.aidentx.access_portal_service.dto.response.tile.*;
import com.aidentx.access_portal_service.entity.tile.*;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestItemRepository;
import com.aidentx.access_portal_service.repository.access_request.projection.TileApprovalMetrics;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TileMapper {

    // --------------------
    // Tile mapper
    // --------------------
    private final AccessRequestItemRepository accessRequestItemRepository;
    private final IUnifiedLogger logger;

    public TileMapper(AccessRequestItemRepository accessRequestItemRepository, UnifiedLoggerFactory loggerFactory) {
        this.accessRequestItemRepository = accessRequestItemRepository;
        this.logger = loggerFactory.getLogger(
                getClass(),
                "tile-mapper"
        );
    }

    /**
     * Calculate approval rate for a tile based on all its items
     */
    private Double calculateTileApprovalRate(Tile tile) {
        try {
            TileApprovalMetrics metrics =
                    accessRequestItemRepository.getApprovalMetricsForTile(
                            tile.getId(),
                            tile.getTenantId()
                    );

            if (metrics == null) {
                return 0.0;
            }

            long approvedCount = metrics.getApprovedCount() != null ? metrics.getApprovedCount() : 0L;
            long rejectedCount = metrics.getRejectedCount() != null ? metrics.getRejectedCount() : 0L;
            long totalDecisions = approvedCount + rejectedCount;

            if (totalDecisions == 0) {
                return 0.0;
            }

            double approvalRate = (double) approvedCount / totalDecisions * 100.0;
            return Math.round(approvalRate * 100.0) / 100.0;

        } catch (Exception e) {
            logger.warn()
                    .message("Failed to calculate tile approval rate")
                    .detail("tileId", tile.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            return 0.0;
        }
    }

    public Tile toEntity(TileRequestDTO dto) {
        Tile tile = new Tile();
        tile.setTitle(dto.getTitle());
        tile.setCategory(dto.getCategory());
        tile.setDescription(dto.getDescription());
        tile.setIcon(dto.getIcon());
        tile.setTileActive(dto.isTileActive());
        // manager approval
        tile.setRequireManagerApproval(dto.isRequireManagerApproval());
        tile.setWorkflowId(dto.getWorkflowId());
        // meta data
        tile.setMaximumRiskLevel(dto.getMaximumRiskLevel());
        tile.setAllowRequestForOthers(dto.isAllowRequestForOthers());
        tile.setObjectFilteringEnabled(dto.isObjectFilteringEnabled());
        tile.setAllowApplications(dto.isAllowApplications());
        tile.setAllowBusinessRoles(dto.isAllowBusinessRoles());
        tile.setAllowInternalRoles(dto.isAllowInternalRoles());
        tile.setAllowEntitlements(dto.isAllowEntitlements());
        return tile;
    }

    public TileResponseDTO toResponseForSingleTile(Tile tile) {
        TileResponseDTO dto = new TileResponseDTO();

        dto.setId(tile.getId());
        dto.setTitle(tile.getTitle());
        dto.setDescription(tile.getDescription());
        dto.setIcon(tile.getIcon());
        dto.setCategory(tile.getCategory());
        dto.setTileActive(tile.isTileActive());

        if (tile.getAccessScopeItems() != null) {
            dto.setItemCount(tile.getAccessScopeItems().size());
        } else {
            dto.setItemCount(0);
        }


        dto.setRequireManagerApproval(tile.isRequireManagerApproval());
        dto.setWorkflowId(tile.getWorkflowId());
        dto.setMaximumRiskLevel(tile.getMaximumRiskLevel());
        dto.setAllowRequestForOthers(tile.isAllowRequestForOthers());
        dto.setObjectFilteringEnabled(tile.isObjectFilteringEnabled());
        dto.setAllowApplications(tile.isAllowApplications());
        dto.setAllowBusinessRoles(tile.isAllowBusinessRoles());
        dto.setAllowInternalRoles(tile.isAllowInternalRoles());
        dto.setAllowEntitlements(tile.isAllowEntitlements());
        dto.setTenantId(tile.getTenantId());
        dto.setCreatedAt(tile.getCreatedAt());
        dto.setUpdatedAt(tile.getUpdatedAt());

        dto.setApprovalRate(calculateTileApprovalRate(tile));


        // Allowed roles
        dto.setAllowedRoleCodes(
                tile.getAllowedRoles()
                        .stream()
                        .map(TileAllowedRole::getRoleCode)
                        .collect(Collectors.toSet())
        );

        // Access scope items
        dto.setAccessScopeItems(
                tile.getAccessScopeItems()
                        .stream()
                        .map(item -> {
                            TileAccessScopeItemDTO d = new TileAccessScopeItemDTO();
                            d.setItemType(item.getItemType());
                            d.setItemCode(item.getItemCode());
                            return d;
                        })
                        .collect(Collectors.toSet())
        );

        // Visual filters + rules
        dto.setVisualFilters(
                tile.getVisualFilters()
                        .stream()
                        .map(filter -> {
                            TileVisualFilterDTO f = new TileVisualFilterDTO();
                            f.setFilterName(filter.getFilterName());

                            f.setRules(
                                    filter.getRules()
                                            .stream()
                                            .map(rule -> {
                                                TileVisualFilterRuleDTO r = new TileVisualFilterRuleDTO();
                                                r.setFieldName(rule.getFieldName());
                                                r.setLogicalOperator(rule.getLogicalOperator());
                                                r.setOperator(rule.getOperator());
                                                r.setValue(rule.getValue());
                                                return r;
                                            })
                                            .toList()
                            );
                            return f;
                        })
                        .toList()
        );

        return dto;
    }


    public TileResponseDTO toResponseForListOfTiles(Tile tile) {
        TileResponseDTO dto = new TileResponseDTO();

        dto.setId(tile.getId());
        dto.setTitle(tile.getTitle());
        dto.setDescription(tile.getDescription());
        dto.setIcon(tile.getIcon());
        dto.setCategory(tile.getCategory());
        dto.setTileActive(tile.isTileActive());
        dto.setAllowApplications(tile.isAllowApplications());
        dto.setAllowBusinessRoles(tile.isAllowBusinessRoles());
        dto.setAllowInternalRoles(tile.isAllowInternalRoles());
        dto.setAllowEntitlements(tile.isAllowEntitlements());
        dto.setTenantId(tile.getTenantId());
        dto.setCreatedAt(tile.getCreatedAt());
        dto.setUpdatedAt(tile.getUpdatedAt());
        dto.setMaximumRiskLevel(tile.getMaximumRiskLevel());
        dto.setAllowRequestForOthers(tile.isAllowRequestForOthers());
        dto.setObjectFilteringEnabled(tile.isObjectFilteringEnabled());

        dto.setApprovalRate(calculateTileApprovalRate(tile));

        if (tile.getAccessScopeItems() != null) {
            dto.setItemCount(tile.getAccessScopeItems().size());
        } else {
            dto.setItemCount(0);
        }

        return dto;
    }


}

