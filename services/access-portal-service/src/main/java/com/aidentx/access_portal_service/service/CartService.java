package com.aidentx.access_portal_service.service;

import com.aidentx.access_portal_service.dto.request.cart.AddToCartRequestDTO;
import com.aidentx.access_portal_service.dto.response.cart.CartItemResponseDTO;
import com.aidentx.access_portal_service.dto.response.cart.CartPageResponseDTO;

import java.util.UUID;

public interface CartService {

    CartItemResponseDTO addToCart(String tenantId, UUID userId, AddToCartRequestDTO requestDTO);

    CartPageResponseDTO getCart(String tenantId, UUID userId, int page, int size, String sortBy, String sortDirection);

    void removeFromCart(String tenantId, UUID userId, UUID cartItemId);

    long getCartItemCount(String tenantId, UUID userId);
}