package com.aidentx.access_portal_service.dto.response.cart;

import com.aidentx.access_portal_service.entity.cart.CartStatus;
import com.aidentx.access_portal_service.service.enrichment.NameResolvable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO implements NameResolvable {

    private UUID id;
    private UUID userId;
    private UUID tileId;
    private String itemType;
    private UUID itemId;
    private String itemName;
    private String itemDescription;
    private String itemCode;
    private String requestType;
    private List<UUID> beneficiaryUserIds;
    private List<String> beneficiaryNames;
    private CartStatus status;
    private String tenantId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private boolean merged;
}