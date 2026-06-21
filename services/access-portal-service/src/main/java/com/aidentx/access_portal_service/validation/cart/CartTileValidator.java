package com.aidentx.access_portal_service.validation.cart;

import com.aidentx.access_portal_service.entity.tile.Tile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Validator for cart operations related to tiles
 */
@Component
public class CartTileValidator {

    /**
     * Validates if a tile is eligible for cart operations
     */
    public void validateTileForCart(Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null");
        }

        validateTileActive(tile);
        // Additional tile validations can be added here
    }

    /**
     * Validates if tile is active
     */
    public void validateTileActive(Tile tile) {
        if (!tile.isTileActive()) {
            throw new IllegalArgumentException("Tile is not active");
        }
    }

    /**
     * Validates if an item belongs to tile's access scope
     */
    public boolean isItemAllowedInTile(Tile tile, UUID itemId, String itemType) {
        if (tile == null || itemId == null || itemType == null) {
            return false;
        }

        return tile.getAccessScopeItems().stream()
                .anyMatch(item -> item.getItemCode().equals(itemId)
                        && item.getItemType().equals(itemType));
    }

    /**
     * Validates if an item belongs to tile's access scope, throws exception if not
     */
    public void validateItemAllowedInTile(Tile tile, UUID itemId, String itemType) {
        if (!isItemAllowedInTile(tile, itemId, itemType)) {
            throw new IllegalArgumentException(
                    String.format("Item %s of type %s is not allowed in tile %s",
                            itemId, itemType, tile.getId())
            );
        }
    }

    /**
     * Validates if request type is allowed by tile configuration
     */
    public void validateRequestType(Tile tile, String requestType, List<UUID> beneficiaryUserIds, UUID requestingUserId) {
        if (tile == null || requestType == null) {
            throw new IllegalArgumentException("Tile and request type cannot be null");
        }

        switch (requestType.toUpperCase()) {
            case "SELF":
                validateIsSelfRequest(beneficiaryUserIds, requestingUserId);
                break;
            case "OTHERS":
                validateRequestForOthers(tile);
                validateBeneficiariesRequired(beneficiaryUserIds);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }
    }

    /**
     * Validates that beneficiaryUserIds is null or empty for SELF requests
     */
    private void validateIsSelfRequest(List<UUID> beneficiaryUserIds, UUID requestingUserId) {
        if (beneficiaryUserIds != null && !beneficiaryUserIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "SELF request type cannot have beneficiary users. Remove beneficiaries or change request type to OTHERS."
            );
        }
    }

    /**
     * Validates that beneficiaryUserIds is provided for OTHERS requests
     */
    private void validateBeneficiariesRequired(List<UUID> beneficiaryUserIds) {
        if (beneficiaryUserIds == null || beneficiaryUserIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Beneficiary users are required for OTHERS request type"
            );
        }
    }

    /**
     * Validates that tile allows requesting for others
     */
    private void validateRequestForOthers(Tile tile) {
        if (!tile.isAllowRequestForOthers()) {
            throw new IllegalArgumentException(
                    "This tile does not allow requesting access for others"
            );
        }
    }

    /**
     * Validates if tile requires manager approval
     */
    public void validateManagerApprovalRequired(Tile tile) {
        if (tile.isRequireManagerApproval()) {
            throw new IllegalStateException(
                    "This tile requires manager approval. Cannot proceed with cart operation."
            );
        }
    }


    /**
     * Validates if tile allows object filtering
     */
    public void validateObjectFiltering(Tile tile, boolean hasFilters) {
        if (hasFilters && !tile.isObjectFilteringEnabled()) {
            throw new IllegalArgumentException(
                    "Object filtering is not enabled for this tile"
            );
        }
    }
}
