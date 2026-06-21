package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Workflow Simulation Results
 * Provides detailed analysis of what would happen in a simulation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulationResultDTO {

    private UUID simulationId;
    private UUID workflowId;
    private UUID executionId;
    private String workflowName;
    
    // Simulation metadata
    private OffsetDateTime simulatedAt;
    private UUID simulatedBy;
    private String simulatedByUsername;
    private Long simulationDurationMs;
    
    // Execution summary
    private String status; // COMPLETED, FAILED, etc.
    private Integer totalSteps;
    private Integer stepsCompleted;
    private Integer failedSteps;
    
    // Simulation-specific analysis
    private SimulationSummary summary;
    
    // Detailed results
    private List<WorkflowNodeExecutionDTO> nodeExecutions;
    private List<Map<String, Object>> pathTrace;
    
    // Context and scenario
    private Map<String, Object> inputScenario;
    private Map<String, Object> outputContext;
    
    // Warnings and recommendations
    private List<String> warnings;
    private List<String> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationSummary {
        // Approval summary
        private Integer totalApprovals;
        private Integer autoApprovedCount;
        private List<String> approverList;
        
        // Action summary
        private Integer provisionsCount;
        private Integer deprovisionsCount;
        private Integer notificationsCount;
        private Integer httpCallsCount;
        private Integer scriptsCount;
        
        // Simulated actions
        private List<SimulatedAction> simulatedActions;
        
        // Conditions and branches
        private Integer conditionsEvaluated;
        private Map<String, Object> branchesTaken;
        
        // Risk assessment
        private String riskLevel; // LOW, MEDIUM, HIGH
        private List<String> potentialIssues;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulatedAction {
        private String nodeId;
        private String actionType;
        private String description;
        private Map<String, Object> wouldExecute;
        private Boolean wouldSucceed;
        private String reason;
    }
}
