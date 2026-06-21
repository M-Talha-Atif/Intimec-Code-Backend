package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Workflow Template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowTemplateDTO {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private String framework;
    private String complexity;

    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;

    private String version;
    private List<String> tags;

    private Integer usageCount;
    private BigDecimal rating;
    private Integer downloads;

    private Integer steps;
    private String estimatedTime;

    private List<String> complianceFrameworks;
    private List<String> prerequisites;
    private Map<String, Object> variables;

    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Boolean isSystemTemplate;

    /**
     * Factory method to convert Entity to DTO
     */
    public static WorkflowTemplateDTO from(WorkflowTemplate template) {
        if (template == null) return null;

        return WorkflowTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .framework(template.getFramework())
                .complexity(template.getComplexity() != null ? template.getComplexity().name() : null)
                .nodes(template.getNodes())
                .edges(template.getEdges())
                .version(template.getVersion())
                .tags(template.getTags())
                .usageCount(template.getUsageCount())
                .rating(template.getRating())
                .downloads(template.getDownloads())
                .steps(template.getSteps())
                .estimatedTime(template.getEstimatedTime())
                .complianceFrameworks(template.getComplianceFrameworks())
                .prerequisites(template.getPrerequisites())
                .variables(template.getVariables())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .isSystemTemplate(template.getIsSystemTemplate())
                .build();
    }
}
