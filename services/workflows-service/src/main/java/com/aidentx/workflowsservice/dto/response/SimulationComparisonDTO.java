package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for comparing simulation with real execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulationComparisonDTO {

    private UUID simulationId;
    private UUID realExecutionId;
    
    private SimulationResultDTO simulation;
    private WorkflowExecutionDTO realExecution;
    
    private ComparisonSummary comparison;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummary {
        private Boolean pathsMatch;
        private Integer totalNodes;
        private Integer matchingNodes;
        private Integer differingNodes;
        
        private List<NodeDifference> differences;
        
        private Map<String, Object> simulationMetrics;
        private Map<String, Object> realMetrics;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDifference {
        private String nodeId;
        private String difference;
        private Object simulatedValue;
        private Object realValue;
    }
}
