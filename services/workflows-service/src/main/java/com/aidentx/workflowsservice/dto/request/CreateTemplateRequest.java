package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a workflow template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String framework; // NIST, ISO27001, SOC2, etc.

    @NotNull(message = "Complexity is required")
    private WorkflowTemplate.Complexity complexity;

    @NotEmpty(message = "Nodes are required")
    private List<Map<String, Object>> nodes;

    @NotEmpty(message = "Edges are required")
    private List<Map<String, Object>> edges;

    private Map<String, Object> variables; // Template variables for instantiation

    private List<String> tags;

    private Integer steps;

    private String estimatedTime; // e.g., "2-4 hours"

    private List<String> complianceFrameworks;

    private List<String> prerequisites;

    private Boolean isSystemTemplate = false;
}
