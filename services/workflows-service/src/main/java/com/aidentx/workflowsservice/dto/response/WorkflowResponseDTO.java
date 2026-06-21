package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.Workflow;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Workflow entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private String version;
    private String status;
    private String category;
    private String triggerType;
    
    private Map<String, Object> eventConfig;
    private String cronSchedule;

    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;

    private List<String> applicationScope;
    private List<String> tags;
    private List<String> complianceFrameworks;

    private UUID sourceTemplateId;
    private Boolean isConnectedToTemplate;
    private String upstreamVersion;
    private Boolean driftDetected;

    private Map<String, Boolean> permissions;

    private Integer riskScore;
    private Boolean hasTimeLogic;
    private Boolean hasSodViolation;
    private String roleSensitivity;
    private String policyCompliance;

    private UUID createdBy;
    private UUID lastModifiedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Execution statistics
    private Long executionCount;
    private Double averageCompletionTime; // in hours
    private OffsetDateTime lastExecuted;

    /**
     * Factory method to convert Entity to DTO
     */
    public static WorkflowResponseDTO from(Workflow workflow) {
        if (workflow == null) return null;

        return WorkflowResponseDTO.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .version(workflow.getVersion())
                .status(workflow.getStatus() != null ? workflow.getStatus().name() : null)
                .category(workflow.getCategory() != null ? workflow.getCategory().name() : null)
                .triggerType(workflow.getTriggerType() != null ? workflow.getTriggerType().name() : null)
                .eventConfig(workflow.getEventConfig())
                .cronSchedule(workflow.getCronSchedule())
                .nodes(convertNodesToDTO(workflow.getNodes()))
                .edges(convertEdgesToDTO(workflow.getEdges()))
                .applicationScope(workflow.getApplicationScope())
                .tags(workflow.getTags())
                .complianceFrameworks(workflow.getComplianceFrameworks())
                .sourceTemplateId(workflow.getSourceTemplateId())
                .isConnectedToTemplate(workflow.getIsConnectedToTemplate())
                .upstreamVersion(workflow.getUpstreamVersion())
                .driftDetected(workflow.getDriftDetected())
                .permissions(workflow.getPermissions())
                .riskScore(workflow.getRiskScore())
                .hasTimeLogic(workflow.getHasTimeLogic())
                .hasSodViolation(workflow.getHasSodViolation())
                .roleSensitivity(workflow.getRoleSensitivity() != null ? workflow.getRoleSensitivity().name() : null)
                .policyCompliance(workflow.getPolicyCompliance() != null ? workflow.getPolicyCompliance().name() : null)
                .createdBy(workflow.getCreatedBy())
                .lastModifiedBy(workflow.getLastModifiedBy())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                // Note: execution statistics are set separately in the service layer
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<NodeDTO> convertNodesToDTO(List<Map<String, Object>> nodes) {
        if (nodes == null) return null;
        return nodes.stream()
                .map(node -> NodeDTO.builder()
                        .id((String) node.get("id"))
                        .type((String) node.get("type"))
                        .data((Map<String, Object>) node.get("data"))
                        .position(node.get("position") != null ? 
                                convertPosition((Map<String, Object>) node.get("position")) : null)
                        .build())
                .collect(Collectors.toList());
    }

    private static NodeDTO.PositionDTO convertPosition(Map<String, Object> position) {
        if (position == null) return null;
        return NodeDTO.PositionDTO.builder()
                .x(position.get("x") != null ? ((Number) position.get("x")).intValue() : null)
                .y(position.get("y") != null ? ((Number) position.get("y")).intValue() : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<EdgeDTO> convertEdgesToDTO(List<Map<String, Object>> edges) {
        if (edges == null) return null;
        return edges.stream()
                .map(edge -> EdgeDTO.builder()
                        .id((String) edge.get("id"))
                        .source((String) edge.get("source"))
                        .target((String) edge.get("target"))
                        .sourceHandle((String) edge.get("sourceHandle"))
                        .targetHandle((String) edge.get("targetHandle"))
                        .label((String) edge.get("label"))
                        .type((String) edge.get("type"))
                        .condition((Map<String, Object>) edge.get("condition"))
                        .data((Map<String, Object>) edge.get("data"))
                        .build())
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeDTO {
        private String id;
        private String type;
        private Map<String, Object> data;
        private PositionDTO position;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PositionDTO {
            private Integer x;
            private Integer y;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeDTO {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String label;
        private String type;
        private Map<String, Object> condition;
        private Map<String, Object> data;
    }
}
