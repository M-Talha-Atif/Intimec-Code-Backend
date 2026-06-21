package com.aidentx.access_portal_service.dto.request.access_request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for withdrawing an access request.
 * Only submitted or pending requests can be withdrawn.
 */
@Getter
@Setter
public class WithdrawRequestDTO {

    /**
     * Reason for withdrawing the request.
     * Required to maintain audit trail.
     */
    @NotBlank(message = "Withdrawal reason is required")
    private String withdrawalReason;
}