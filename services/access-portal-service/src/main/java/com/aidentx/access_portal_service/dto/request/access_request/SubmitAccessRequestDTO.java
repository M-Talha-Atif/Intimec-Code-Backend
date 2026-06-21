package com.aidentx.access_portal_service.dto.request.access_request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmitAccessRequestDTO {

    @NotBlank(message = "Overall justification is required")
    private String overallJustification;

    @NotNull(message = "Items list is required")
    @Valid
    private List<RequestItemDTO> items;

    @Getter
    @Setter
    public static class RequestItemDTO {

        @NotNull(message = "Item ID is required")
        private UUID itemId;

        @NotNull(message = "Cart Item ID is required")
        private UUID cartItemId; // for uniqueness

        @NotNull(message = "Validity days is required")
        @Min(value = 1, message = "Validity days must be at least 1")
        @Max(value = 365, message = "Validity days cannot exceed 365")
        private Integer validityDays;

        @NotBlank(message = "Urgency is required")
        private String urgency; // LOW, MEDIUM, HIGH

//        @NotBlank(message = "Justification is required")
        private String justification;
    }
}