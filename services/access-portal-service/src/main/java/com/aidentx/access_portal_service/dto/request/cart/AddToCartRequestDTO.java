package com.aidentx.access_portal_service.dto.request.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequestDTO {

    @NotNull(message = "Tile ID is required")
    private UUID tileId;

    @NotBlank(message = "Item type is required")
    private String itemType;

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String itemDescription;


    @NotBlank(message = "Request type is required")
    @Builder.Default
    private String requestType = "SELF";

    private List<UUID> beneficiaryUserIds; // List of beneficiary user IDs
    private List<String> beneficiaryNames; // Parallel list of names
}