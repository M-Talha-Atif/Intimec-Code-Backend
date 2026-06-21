package com.aidentx.access_portal_service.dto.response.tile;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TileAccessScopeItemResponseDTO {

    private UUID id;
    private String itemType;
    private UUID itemCode;
    //  Item details from role service
    private String itemName;
    private String itemDescription;
    private String itemIcon;
    private String riskLevel;
    private String itemOwner;
    private String department;

    //  NEW: Approval metrics for this item
    private Double approvalRate;  // Percentage: (approved / (approved + rejected)) * 100
    private Integer totalRequests; // Total requests for this item (all statuses)

    // Tile-level configuration (same for all items from this tile)
    private Boolean allowRequestForOthers;
}
