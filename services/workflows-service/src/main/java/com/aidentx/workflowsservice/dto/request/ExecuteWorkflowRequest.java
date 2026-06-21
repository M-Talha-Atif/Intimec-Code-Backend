package com.aidentx.workflowsservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for executing a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowRequest {

    @NotBlank(message = "Triggered by is required")
    private String triggeredBy; // User ID who triggered (required)

    @Builder.Default
    private Map<String, Object> context = new HashMap<>(); // Optional - defaults to empty map

    private String triggerType; // MANUAL, EVENT_BASED, TIME_BASED

    private String idempotencyKey; // For preventing duplicate executions

    @Builder.Default
    private Boolean dryRun = false; // Simulation mode - defaults to false

    private Integer timeoutMinutes; // Execution timeout
}
