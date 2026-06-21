package com.aidentx.workflowsservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for instantiating a template into a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstantiateTemplateRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotBlank(message = "Workflow name is required")
    private String workflowName;

    private Map<String, Object> variableValues; // Values to replace template variables
}
