package com.aidentx.access_portal_service.controller;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.cart.AddToCartRequestDTO;
import com.aidentx.access_portal_service.dto.response.ApiResponse;
import com.aidentx.access_portal_service.dto.response.cart.*;
import com.aidentx.access_portal_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_TENANT_ID;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_USER_ID;
import static com.aidentx.access_portal_service.util.RequestContextUtil.resolveUserId;

@RestController
@RequestMapping(AccessPortalConstants.API_BASE_CART)
@RequiredArgsConstructor
@Tag(name = AccessPortalConstants.TAG_CART_API, description = AccessPortalConstants.TAG_CART_API_DESC)
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(summary = AccessPortalConstants.OP_ADD_TO_CART, description = AccessPortalConstants.OP_ADD_TO_CART_DESC)
    public ResponseEntity<ApiResponse<CartItemResponseDTO>> addToCart(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @Valid @RequestBody AddToCartRequestDTO requestDTO
    ) {

        UUID createdBy = resolveUserId(userIdHeader);

        CartItemResponseDTO cartItem = cartService.addToCart(tenantIdHeader, createdBy, requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(AccessPortalConstants.SUCCESS_ITEM_ADDED_TO_CART, cartItem));
    }

    @GetMapping
    @Operation(summary = AccessPortalConstants.OP_GET_CART, description = AccessPortalConstants.OP_GET_CART_DESC)
    public ResponseEntity<ApiResponse<CartPageResponseDTO>> getCart(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        UUID fetchedBy = resolveUserId(userIdHeader);

        CartPageResponseDTO cart = cartService.getCart(tenantIdHeader, fetchedBy, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping(AccessPortalConstants.API_CART_ITEM_BY_ID)
    @Operation(summary = AccessPortalConstants.OP_REMOVE_FROM_CART, description = AccessPortalConstants.OP_REMOVE_FROM_CART_DESC)
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @PathVariable UUID cartItemId
    ) {

        UUID deletedBy = resolveUserId(userIdHeader);

        cartService.removeFromCart( tenantIdHeader , deletedBy, cartItemId);

        return ResponseEntity.ok(ApiResponse.success(AccessPortalConstants.SUCCESS_ITEM_REMOVED_FROM_CART, null));
    }
}