package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.entity.WorkflowApproval;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a manual approval request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalRequest {

    @NotNull(message = "Execution ID is required")
    private UUID executionId;

    private UUID nodeExecutionId;

    @NotNull(message = "Approver ID is required")
    private UUID approverId;

    @NotNull(message = "Approver type is required")
    private WorkflowApproval.ApproverType approverType;

    private Map<String, Object> approvalContext;

    private Map<String, Object> evidenceBundle;

    private Integer timeoutMinutes; // Optional timeout in minutes
}
