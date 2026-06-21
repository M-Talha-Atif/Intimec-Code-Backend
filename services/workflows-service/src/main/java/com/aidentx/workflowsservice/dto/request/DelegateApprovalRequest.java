package com.aidentx.workflowsservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for delegating an approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateApprovalRequest {

    @NotNull(message = "Delegate to user ID is required")
    private UUID delegateToUserId;

    @NotNull(message = "Current approver ID is required")
    private UUID currentApproverId;

    private String reason;
}
