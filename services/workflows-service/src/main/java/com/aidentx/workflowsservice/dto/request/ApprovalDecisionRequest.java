package com.aidentx.workflowsservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for approval decision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecisionRequest {

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVED, REJECTED

    @Size(max = 2000, message = "Comments must not exceed 2000 characters")
    private String comments;

    private String delegatedTo; // UUID of user to delegate to

    private Map<String, Object> evidenceBundle; // Supporting documents/data
}
