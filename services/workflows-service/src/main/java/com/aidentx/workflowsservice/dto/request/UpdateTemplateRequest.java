package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating a workflow template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String framework;

    private WorkflowTemplate.Complexity complexity;

    @NotEmpty(message = "Nodes are required")
    private List<Map<String, Object>> nodes;

    @NotEmpty(message = "Edges are required")
    private List<Map<String, Object>> edges;

    private Map<String, Object> variables;

    private List<String> tags;

    private Integer steps;

    private String estimatedTime;

    private List<String> complianceFrameworks;

    private List<String> prerequisites;
}
