package com.aidentx.access_portal_service.validation.cart;

import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validator for cart item operations
 */
@Component
public class CartItemValidator {

    /**
     * Validates cart item for operations
     */
    public void validateCartItem(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }

        validateCartItemStatus(cartItem);
        // Additional cart item validations can be added here
    }

    /**
     * Validates cart item is in active status
     */
    public void validateCartItemStatus(CartItem cartItem) {
        if (cartItem.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Cart item %s is not active. Current status: %s",
                            cartItem.getId(), cartItem.getStatus())
            );
        }
    }

    /**
     * Validates if user owns the cart item
     */
    public void validateCartItemOwnership(CartItem cartItem, UUID userId) {
        if (cartItem == null || userId == null) {
            throw new IllegalArgumentException("Cart item and user ID cannot be null");
        }

        if (!userId.equals(cartItem.getUserId())) {
            throw new SecurityException(
                    String.format("User %s does not own cart item %s", userId, cartItem.getId())
            );
        }
    }

    /**
     * Validates if cart item belongs to tenant
     */
    public void validateCartItemTenant(CartItem cartItem, String tenantId) {
        if (cartItem == null || tenantId == null) {
            throw new IllegalArgumentException("Cart item and tenant ID cannot be null");
        }

        if (!tenantId.equals(cartItem.getTenantId())) {
            throw new SecurityException(
                    String.format("Cart item %s does not belong to tenant %s",
                            cartItem.getId(), tenantId)
            );
        }
    }

    /**
     * Validates if duplicate cart item exists
     */
    public boolean isDuplicateCartItem(CartItem existingCartItem, CartItem newCartItem) {
        if (existingCartItem == null || newCartItem == null) {
            return false;
        }

        return existingCartItem.getUserId().equals(newCartItem.getUserId())
                && existingCartItem.getTile().getId().equals(newCartItem.getTile().getId())
                && existingCartItem.getItemId().equals(newCartItem.getItemId())
                && existingCartItem.getStatus() == CartStatus.ACTIVE;
    }

    /**
     * Validates if cart item can be modified
     */
    public void validateCartItemModifiable(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }

        // Add business rules for when cart items can be modified
        // For example, if cart item is already submitted for approval
        if (cartItem.getStatus() == CartStatus.PENDING) {
            throw new IllegalStateException(
                    "Cart item is pending approval and cannot be modified"
            );
        }

        if (cartItem.getStatus() == CartStatus.DELETED) {
            throw new IllegalStateException(
                    "Cart item is deleted and cannot be modified"
            );
        }
    }

    /**
     * Validates beneficiary information consistency
     */
    public void validateBeneficiaryInfo(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }

        String beneficiaryUserIds = cartItem.getBeneficiaryUserIds();
        String beneficiaryNames = cartItem.getBeneficiaryNames();

        // Both should be null/empty or both should have values
        boolean hasUserIds = beneficiaryUserIds != null && !beneficiaryUserIds.isEmpty();
        boolean hasNames = beneficiaryNames != null && !beneficiaryNames.isEmpty();

        if (hasUserIds != hasNames) {
            throw new IllegalArgumentException(
                    "Beneficiary user IDs and names must both be provided or both be null"
            );
        }

        // If both are provided, validate they have the same count
        if (hasUserIds && hasNames) {
            int userIdCount = beneficiaryUserIds.split(",").length;
            int nameCount = beneficiaryNames.split(",").length;

            if (userIdCount != nameCount) {
                throw new IllegalArgumentException(
                        String.format(
                                "Beneficiary user IDs count (%d) must match beneficiary names count (%d)",
                                userIdCount, nameCount
                        )
                );
            }
        }

        // Validate request type consistency
        if ("SELF".equals(cartItem.getRequestType()) && hasUserIds) {
            throw new IllegalArgumentException(
                    "SELF request type cannot have beneficiary users"
            );
        }

        if ("OTHERS".equals(cartItem.getRequestType()) && !hasUserIds) {
            throw new IllegalArgumentException(
                    "OTHERS request type must have at least one beneficiary user"
            );
        }
    }
}