package com.aidentx.access_portal_service.mapper;

import com.aidentx.access_portal_service.dto.response.cart.CartItemResponseDTO;
import com.aidentx.access_portal_service.dto.response.cart.CartPageResponseDTO;
import com.aidentx.access_portal_service.entity.cart.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

import static com.aidentx.access_portal_service.util.StringParsingUtils.parseBeneficiaryNames;
import static com.aidentx.access_portal_service.util.StringParsingUtils.parseBeneficiaryUserIds;


@Component
public class CartMapper {

    public CartItemResponseDTO toCartItemResponse(CartItem cartItem) {
        return CartItemResponseDTO.builder()
                .id(cartItem.getId())
                .userId(cartItem.getUserId())
                .tileId(cartItem.getTile().getId())
                .itemType(cartItem.getItemType())
                .itemId(cartItem.getItemId())
                .itemName(cartItem.getItemName())
                .itemDescription(cartItem.getItemDescription())
                .requestType(cartItem.getRequestType())
                .beneficiaryUserIds(parseBeneficiaryUserIds(cartItem.getBeneficiaryUserIds()))
                .beneficiaryNames(parseBeneficiaryNames(cartItem.getBeneficiaryNames()))
                .status(cartItem.getStatus())
                .tenantId(cartItem.getTenantId())
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }

    public List<CartItemResponseDTO> toCartItemResponseList(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
    }

    public CartPageResponseDTO toCartPageResponse(Page<CartItem> page) {
        return CartPageResponseDTO.builder()
                .items(toCartItemResponseList(page.getContent()))
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}




