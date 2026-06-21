package com.aidentx.access_portal_service.dto.response.tile;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class TileDeletionImpactDTO {
    private UUID tileId;
    private String tileTitle;
    private boolean canDelete;
    private String deleteBlockReason;

    // Impact summary
    private ImpactSummary impactSummary;

    // Detailed items
    private List<AffectedCartItem> affectedCartItems;
    private List<AffectedRequest> affectedRequests;

    @Getter
    @Setter
    @Builder
    public static class ImpactSummary {
        private int totalCartItems;
        private int activeCartItems;
        private int totalRequests;
        private int blockingRequests; // Requests in SUBMITTED, PENDING_APPROVAL status
        private int processedRequests; // Requests in PROVISIONED, PARTIALLY_PROVISIONED, REJECTED
    }

    @Getter
    @Setter
    @Builder
    public static class AffectedCartItem {
        private UUID cartItemId;
        private String itemName;
        private String itemType;
        private String status;
        private String userName;
        private UUID userId;
        private String createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class AffectedRequest {
        private UUID requestId;
        private String status;
        private String requestedByUserName;
        private UUID requestedByUserId;
        private String requestedAt;
        private int itemCount;
        private List<RequestItemSummary> items;
    }

    @Getter
    @Setter
    @Builder
    public static class RequestItemSummary {
        private UUID itemId;
        private String itemName;
        private String itemType;
        private String requestTarget;
        private int beneficiaryCount;
    }
}