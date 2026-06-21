package com.aidentx.workflowsservice.service;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.async.AsyncExecutionDispatcher;
import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.SimulationComparisonDTO;
import com.aidentx.workflowsservice.dto.response.SimulationResultDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowExecutionDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowNodeExecutionDTO;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.entity.WorkflowSimulation;
import com.aidentx.workflowsservice.exception.ResourceNotFoundException;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowSimulationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing workflow simulations
 * Handles simulation execution, persistence, history, and comparison
 */
@Service
public class WorkflowSimulationService {

    private final IUnifiedLogger logger;
    private final WorkflowSimulationRepository simulationRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowExecutionService executionService;
    private final AsyncExecutionDispatcher asyncExecutionDispatcher;

    public WorkflowSimulationService(
            WorkflowSimulationRepository simulationRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowExecutionService executionService,
            AsyncExecutionDispatcher asyncExecutionDispatcher,
            UnifiedLoggerFactory loggerFactory) {
        this.simulationRepository = simulationRepository;
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.executionService = executionService;
        this.asyncExecutionDispatcher = asyncExecutionDispatcher;
        this.logger = loggerFactory.getLogger(WorkflowSimulationService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Execute a workflow in simulation mode
     */
    @Transactional
    public SimulationResultDTO simulate(UUID workflowId, ExecuteWorkflowRequest request, UUID simulatedBy) {
        logger.info()
            .message("Starting workflow simulation")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflowId.toString())
            .detail("simulatedBy", simulatedBy != null ? simulatedBy.toString() : "null")
            .log();
        
        try {
            // Force dryRun mode
            request.setDryRun(true);
            
            // Execute workflow in simulation mode (async - returns immediately)
            logger.info()
                .message("Simulation: Executing workflow")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowId.toString())
                .log();
            WorkflowExecutionDTO execution = executionService.executeWorkflow(workflowId, request, simulatedBy);
            
            // Return immediately - WebSocket will handle real-time updates
            // State replay will show any nodes that completed before client subscribes
            logger.info()
                .message("Simulation started - returning execution ID for WebSocket tracking")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectId(execution.getId().toString())
                .log();
            
            // Build minimal simulation result with execution ID
            // UI will receive real-time updates via WebSocket
            SimulationResultDTO result = SimulationResultDTO.builder()
                .executionId(execution.getId())
                .workflowId(workflowId)
                .status("RUNNING")
                .simulatedAt(OffsetDateTime.now())
                .simulatedBy(simulatedBy)
                .build();
            
            logger.info()
                .message("Workflow simulation initiated successfully")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType("WorkflowSimulation")
                .objectId(execution.getId().toString())
                .detail("executionId", execution.getId().toString())
                .log();
            
            return result;
        } catch (Exception e) {
            logger.error()
                .message("SIMULATION FAILED: " + e.getMessage())
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowId.toString())
                .detail("errorClass", e.getClass().getSimpleName())
                .detail("stackTrace", e.getStackTrace()[0].toString())
                .log();
            
            // Return a fallback result instead of throwing
            return createFallbackSimulationResult(workflowId, request.getContext(), simulatedBy, e);
        }
    }

    /**
     * Get simulation by ID
     */
    public SimulationResultDTO getSimulation(UUID simulationId) {
        WorkflowSimulation simulation = simulationRepository.findById(simulationId)
            .orElseThrow(() -> new ResourceNotFoundException("Simulation not found: " + simulationId));
        
        // Reconstruct result from stored data
        return reconstructSimulationResult(simulation);
    }

    /**
     * Get all simulations for a workflow
     */
    public List<SimulationResultDTO> getSimulationHistory(UUID workflowId) {
        List<WorkflowSimulation> simulations = simulationRepository.findSimulationHistory(workflowId);
        
        return simulations.stream()
            .map(this::reconstructSimulationResult)
            .collect(Collectors.toList());
    }

    /**
     * Get simulations by user
     */
    public List<SimulationResultDTO> getSimulationsByUser(UUID userId) {
        List<WorkflowSimulation> simulations = simulationRepository.findByCreatedBy(userId);
        
        return simulations.stream()
            .map(this::reconstructSimulationResult)
            .collect(Collectors.toList());
    }

    /**
     * Compare simulation with real execution
     */
    public SimulationComparisonDTO compareWithRealExecution(UUID simulationId, UUID realExecutionId) {
        WorkflowSimulation simulation = simulationRepository.findById(simulationId)
            .orElseThrow(() -> new ResourceNotFoundException("Simulation not found: " + simulationId));
        
        WorkflowExecution realExecution = executionRepository.findById(realExecutionId)
            .orElseThrow(() -> new ResourceNotFoundException("Execution not found: " + realExecutionId));
        
        // Verify both are for the same workflow
        UUID simulationWorkflowId = simulation.getWorkflowId();
        if (!simulationWorkflowId.equals(realExecution.getWorkflowId())) {
            throw new IllegalArgumentException("Simulation and execution are for different workflows");
        }
        
        SimulationResultDTO simulationResult = reconstructSimulationResult(simulation);
        WorkflowExecutionDTO executionDTO = WorkflowExecutionDTO.from(realExecution);
        
        // Build comparison
        SimulationComparisonDTO.ComparisonSummary comparison = compareExecutions(simulationResult, executionDTO);
        
        return SimulationComparisonDTO.builder()
            .simulationId(simulationId)
            .realExecutionId(realExecutionId)
            .simulation(simulationResult)
            .realExecution(executionDTO)
            .comparison(comparison)
            .build();
    }

    /**
     * Promote simulation to real execution
     */
    @Transactional
    public WorkflowExecutionDTO promoteToRealExecution(UUID simulationId, UUID promotedBy) {
        WorkflowSimulation simulation = simulationRepository.findById(simulationId)
            .orElseThrow(() -> new ResourceNotFoundException("Simulation not found: " + simulationId));
        
        logger.info()
            .message("Promoting simulation to real execution")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowSimulation")
            .objectId(simulationId.toString())
            .detail("promotedBy", promotedBy != null ? promotedBy.toString() : "null")
            .log();
        
        // Create execution request from simulation scenario
        ExecuteWorkflowRequest request = new ExecuteWorkflowRequest();
        request.setTriggeredBy(promotedBy != null ? promotedBy.toString() : null);
        request.setDryRun(false); // Real execution
        request.setContext(simulation.getScenario());
        request.setTriggerType("PROMOTED_FROM_SIMULATION");
        
        // Execute for real
        return executionService.executeWorkflow(simulation.getWorkflowId(), request, promotedBy);
    }

    /**
     * Delete simulation
     */
    @Transactional
    public void deleteSimulation(UUID simulationId) {
        if (!simulationRepository.existsById(simulationId)) {
            throw new ResourceNotFoundException("Simulation not found: " + simulationId);
        }
        
        simulationRepository.deleteById(simulationId);
        
        logger.info()
            .message("Simulation deleted")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowSimulation")
            .objectId(simulationId.toString())
            .log();
    }

    /**
     * Process simulation approval (approve/reject)
     * Simulates an approval decision and resumes workflow execution
     * Uses context-based decision to keep simulation data isolated from production approval records
     */
    @Transactional
    public WorkflowExecutionDTO processSimulationApproval(
            UUID executionId,
            boolean approved,
            String reason,
            UUID approverId) {
        
        // Validate: rejection requires a reason
        if (!approved && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        
        logger.info()
            .message("Processing simulation approval")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("approved", approved)
            .detail("approverId", approverId != null ? approverId.toString() : "simulated")
            .log();
        
        // Load the execution
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException("Execution not found: " + executionId));
        
        // Verify it's in simulation mode and paused
        Boolean simulationMode = (Boolean) execution.getExecutionContext().get("simulationMode");
        if (!Boolean.TRUE.equals(simulationMode)) {
            throw new IllegalStateException("Execution is not in simulation mode");
        }
        
        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
            throw new IllegalStateException("Execution is not paused for approval");
        }
        
        // Create approval decision in context (clean simulation approach - no DB pollution)
        Map<String, Object> context = execution.getExecutionContext();
        Map<String, Object> approvalDecision = new HashMap<>();
        approvalDecision.put("approved", approved);
        approvalDecision.put("approverId", approverId != null ? approverId.toString() : "00000000-0000-0000-0000-000000000001");
        approvalDecision.put("reason", reason != null ? reason : (approved ? "Approved in simulation" : "Rejected in simulation"));
        approvalDecision.put("decidedAt", OffsetDateTime.now().toString());
        
        context.put("simulationApprovalDecision", approvalDecision);
        
        // Add ALL keys that approval node would return - match regular execution context structure
        // These keys are expected by condition nodes and subsequent workflow steps
        context.put("decision", approved ? "APPROVED" : "REJECTED");  // For condition nodes checking decision status
        context.put("approved", approved);                             // Boolean flag
        context.put("approverId", approverId != null ? approverId.toString() : "00000000-0000-0000-0000-000000000001");
        context.put("approver", approverId != null ? approverId.toString() : "00000000-0000-0000-0000-000000000001");
        context.put("reason", reason != null ? reason : (approved ? "Approved in simulation" : "Rejected in simulation"));
        context.put("timestamp", System.currentTimeMillis());
        context.put("executed", true);                                 // Mark as executed
        context.put("nodeType", "approval.single");                   // Node type identifier
        
        execution.setExecutionContext(context);
        
        // CRITICAL: Update the paused node's output data to include approval decision
        // This ensures the data flows to the next node when WorkflowExecutor merges it
        String currentNodeId = execution.getCurrentNodeId();
        if (currentNodeId != null) {
            Optional<WorkflowNodeExecution> pausedNodeOpt = nodeExecutionRepository
                .findByExecutionIdAndNodeId(executionId, currentNodeId);
            
            if (pausedNodeOpt.isPresent()) {
                WorkflowNodeExecution pausedNode = pausedNodeOpt.get();
                
                // Update output data with approval decision keys
                Map<String, Object> outputData = pausedNode.getOutputData();
                if (outputData == null) {
                    outputData = new HashMap<>();
                }
                
                // Add all approval decision keys to node output
                outputData.put("decision", approved ? "APPROVED" : "REJECTED");
                outputData.put("approved", approved);
                outputData.put("approverId", approverId != null ? approverId.toString() : "00000000-0000-0000-0000-000000000001");
                outputData.put("approver", approverId != null ? approverId.toString() : "00000000-0000-0000-0000-000000000001");
                outputData.put("reason", reason != null ? reason : (approved ? "Approved in simulation" : "Rejected in simulation"));
                outputData.put("timestamp", System.currentTimeMillis());
                outputData.put("executed", true);
                outputData.put("nodeType", "approval.single");
                
                pausedNode.setOutputData(outputData);
                nodeExecutionRepository.save(pausedNode);
                
                logger.info()
                    .message("Updated paused node output data with approval decision")
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("WorkflowNodeExecution")
                    .objectId(pausedNode.getId().toString())
                    .detail("nodeId", currentNodeId)
                    .detail("decision", approved ? "APPROVED" : "REJECTED")
                    .log();
            }
        }
        
        // Resume execution (change status from PAUSED to RUNNING)
        execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
        executionRepository.save(execution);
        
        logger.info()
            .message("Simulation approval recorded, resuming execution")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("decision", approved ? "APPROVED" : "REJECTED")
            .log();
        
        // Resume the execution (the executor thread exited when it paused)
        asyncExecutionDispatcher.resume(executionId);
        
        logger.info()
            .message("Execution resumed after approval")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .log();
        
        // Return updated execution status
        return executionService.getExecution(executionId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Wait for execution to complete - polls status then loads full execution
     * Simulation-specific: wraps in error handling to gracefully handle JSONB issues
     */
    private WorkflowExecutionDTO waitForExecutionCompletion(UUID executionId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        long pollInterval = 500; // 500ms
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Use lightweight status check to avoid loading node executions during polling
                String status = executionService.getExecutionStatus(executionId);
                
                logger.debug()
                    .message("Simulation polling: current status=" + status)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectId(executionId.toString())
                    .log();
                
                if ("COMPLETED".equals(status) || "FAILED".equals(status) || 
                    "CANCELLED".equals(status) || "TIMEOUT".equals(status)) {
                    // Execution finished - now load full details with simulation-specific error handling
                    try {
                        return executionService.getExecution(executionId);
                    } catch (Exception e) {
                        logger.warn()
                            .message("Simulation: Failed to load node executions (possibly JSONB issue) - returning basic info")
                            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                            .objectId(executionId.toString())
                            .detail("error", e.getMessage())
                            .detail("errorClass", e.getClass().getSimpleName())
                            .log();
                        
                        // Return basic execution info without node details (simulation can rely on WebSocket)
                        return getBasicExecutionInfo(executionId);
                    }
                }
                
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Simulation polling interrupted", e);
            }
        }
        
        // Timeout reached - try to load current state
        logger.warn()
            .message("Simulation: Execution timeout reached")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectId(executionId.toString())
            .log();
            
        try {
            return executionService.getExecution(executionId);
        } catch (Exception e) {
            logger.warn()
                .message("Simulation: Failed to load execution after timeout")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectId(executionId.toString())
                .detail("error", e.getMessage())
                .log();
            return getBasicExecutionInfo(executionId);
        }
    }
    
    /**
     * Get basic execution info without node executions (fallback for simulation JSONB errors)
     * This ensures simulation always returns a result even if node execution data is corrupted
     */
    private WorkflowExecutionDTO getBasicExecutionInfo(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException("Execution not found: " + executionId));
        
        WorkflowExecutionDTO dto = WorkflowExecutionDTO.from(execution);
        dto.setNodeExecutions(new ArrayList<>()); // Empty list - frontend can use WebSocket data
        return dto;
    }

    /**
     * Build comprehensive simulation result from execution
     * Gracefully handles missing or corrupted node execution data
     */
    private SimulationResultDTO buildSimulationResult(
            WorkflowExecutionDTO execution, 
            Map<String, Object> inputScenario,
            UUID simulatedBy) {
        
        logger.info()
            .message("Building simulation result")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectId(execution.getId().toString())
            .detail("hasNodeExecutions", execution.getNodeExecutions() != null)
            .detail("nodeExecutionsSize", execution.getNodeExecutions() != null ? execution.getNodeExecutions().size() : 0)
            .log();
        
        // Analyze node executions - with fallback to empty analysis if data is corrupted
        SimulationResultDTO.SimulationSummary summary;
        try {
            summary = analyzeSimulation(execution);
            logger.info()
                .message("Simulation analysis completed successfully")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectId(execution.getId().toString())
                .log();
        } catch (Exception e) {
            logger.warn()
                .message("Failed to analyze simulation (possibly JSONB issue) - using empty summary")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectId(execution.getId().toString())
                .detail("error", e.getMessage())
                .detail("errorClass", e.getClass().getSimpleName())
                .log();
            summary = createEmptySummary();
        }
        
        // Extract metadata
        String simulatedByUsername = null;
        if (execution.getExecutionContext() != null) {
            Object username = execution.getExecutionContext().get("username");
            if (username != null) {
                simulatedByUsername = username.toString();
            }
        }
        
        // Ensure nodeExecutions is never null
        List<WorkflowNodeExecutionDTO> nodeExecutions = execution.getNodeExecutions();
        if (nodeExecutions == null) {
            nodeExecutions = new ArrayList<>();
            logger.warn()
                .message("Node executions list is null - using empty list")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectId(execution.getId().toString())
                .log();
        }
        
        // Build result
        return SimulationResultDTO.builder()
            .simulationId(null) // Will be set when persisted
            .workflowId(execution.getWorkflowId())
            .executionId(execution.getId())
            .workflowName((String) execution.getExecutionContext().get("workflowName"))
            .simulatedAt(execution.getStartedAt())
            .simulatedBy(simulatedBy)
            .simulatedByUsername(simulatedByUsername)
            .simulationDurationMs(execution.getDurationMs())
            .status(execution.getStatus())
            .totalSteps(execution.getTotalSteps())
            .stepsCompleted(execution.getStepsCompleted())
            .failedSteps(execution.getFailedSteps())
            .summary(summary)
            .nodeExecutions(nodeExecutions)
            .pathTrace(execution.getPathTrace() != null ? execution.getPathTrace() : new ArrayList<>())
            .inputScenario(inputScenario != null ? inputScenario : new HashMap<>())
            .outputContext(execution.getExecutionContext() != null ? execution.getExecutionContext() : new HashMap<>())
            .warnings(safeGenerateWarnings(execution))
            .recommendations(safeGenerateRecommendations(execution, summary))
            .build();
    }
    
    /**
     * Safely generate warnings with error handling
     */
    private List<String> safeGenerateWarnings(WorkflowExecutionDTO execution) {
        try {
            return generateWarnings(execution);
        } catch (Exception e) {
            logger.warn()
                .message("Failed to generate warnings")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return List.of("This is a simulation - no real changes were made");
        }
    }
    
    /**
     * Safely generate recommendations with error handling
     */
    private List<String> safeGenerateRecommendations(WorkflowExecutionDTO execution, SimulationResultDTO.SimulationSummary summary) {
        try {
            return generateRecommendations(execution, summary);
        } catch (Exception e) {
            logger.warn()
                .message("Failed to generate recommendations")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return List.of("Review simulation results before production execution");
        }
    }
    
    /**
     * Create empty summary when node data cannot be analyzed
     */
    private SimulationResultDTO.SimulationSummary createEmptySummary() {
        return SimulationResultDTO.SimulationSummary.builder()
            .totalApprovals(0)
            .autoApprovedCount(0)
            .approverList(new ArrayList<>())
            .provisionsCount(0)
            .deprovisionsCount(0)
            .notificationsCount(0)
            .httpCallsCount(0)
            .scriptsCount(0)
            .simulatedActions(new ArrayList<>())
            .conditionsEvaluated(0)
            .branchesTaken(new HashMap<>())
            .riskLevel("UNKNOWN")
            .potentialIssues(List.of("Unable to analyze node executions due to data corruption"))
            .build();
    }
    
    /**
     * Create fallback result when simulation fails catastrophically
     */
    private SimulationResultDTO createFallbackSimulationResult(
            UUID workflowId,
            Map<String, Object> inputScenario,
            UUID simulatedBy,
            Exception error) {
        logger.info()
            .message("Creating fallback simulation result due to error")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflowId.toString())
            .log();
        
        return SimulationResultDTO.builder()
            .simulationId(null)
            .workflowId(workflowId)
            .executionId(null)
            .workflowName("Unknown")
            .simulatedAt(OffsetDateTime.now())
            .simulatedBy(simulatedBy)
            .simulatedByUsername(null)
            .simulationDurationMs(0L)
            .status("FAILED")
            .totalSteps(0)
            .stepsCompleted(0)
            .failedSteps(0)
            .summary(createEmptySummary())
            .nodeExecutions(new ArrayList<>())
            .pathTrace(new ArrayList<>())
            .inputScenario(inputScenario != null ? inputScenario : new HashMap<>())
            .outputContext(new HashMap<>())
            .warnings(List.of(
                "Simulation encountered JSONB data corruption",
                "This may be due to old workflow executions with incompatible data",
                "Simulation results are incomplete but workflow execution succeeded"
            ))
            .recommendations(List.of(
                "Check workflow execution history for corrupt data",
                "Consider cleaning up old workflow executions",
                "Contact system administrator if issue persists"
            ))
            .build();
    }

    /**
     * Analyze simulation to generate summary
     */
    private SimulationResultDTO.SimulationSummary analyzeSimulation(WorkflowExecutionDTO execution) {
        List<WorkflowNodeExecutionDTO> nodeExecutions = execution.getNodeExecutions();
        if (nodeExecutions == null) {
            nodeExecutions = new ArrayList<>();
        }
        
        int totalApprovals = 0;
        int autoApprovedCount = 0;
        List<String> approverList = new ArrayList<>();
        
        int provisionsCount = 0;
        int deprovisionsCount = 0;
        int notificationsCount = 0;
        int httpCallsCount = 0;
        int scriptsCount = 0;
        
        List<SimulationResultDTO.SimulatedAction> simulatedActions = new ArrayList<>();
        
        int conditionsEvaluated = 0;
        Map<String, Object> branchesTaken = new HashMap<>();
        
        // Analyze each node
        for (WorkflowNodeExecutionDTO node : nodeExecutions) {
            String stepType = node.getStepType();
            Map<String, Object> outputData = node.getOutputData();
            
            if (stepType == null) continue;
            
            // Count node types
            if (stepType.contains("approval")) {
                totalApprovals++;
                if (outputData != null && Boolean.TRUE.equals(outputData.get("simulated"))) {
                    autoApprovedCount++;
                }
                if (outputData != null && outputData.get("approver") != null) {
                    approverList.add(outputData.get("approver").toString());
                }
            } else if (stepType.equals("action.provision")) {
                provisionsCount++;
                simulatedActions.add(buildSimulatedAction(node, "Provision"));
            } else if (stepType.equals("action.deprovision")) {
                deprovisionsCount++;
                simulatedActions.add(buildSimulatedAction(node, "Deprovision"));
            } else if (stepType.equals("action.notify")) {
                notificationsCount++;
                simulatedActions.add(buildSimulatedAction(node, "Notification"));
            } else if (stepType.equals("action.http")) {
                httpCallsCount++;
                simulatedActions.add(buildSimulatedAction(node, "HTTP Request"));
            } else if (stepType.equals("action.script")) {
                scriptsCount++;
                simulatedActions.add(buildSimulatedAction(node, "Script Execution"));
            } else if (stepType.contains("condition")) {
                conditionsEvaluated++;
                if (outputData != null && outputData.get("selectedBranch") != null) {
                    branchesTaken.put(node.getNodeId(), outputData.get("selectedBranch"));
                }
            }
        }
        
        // Assess risk
        String riskLevel = assessRiskLevel(execution, provisionsCount, deprovisionsCount);
        List<String> potentialIssues = identifyPotentialIssues(execution, nodeExecutions);
        
        return SimulationResultDTO.SimulationSummary.builder()
            .totalApprovals(totalApprovals)
            .autoApprovedCount(autoApprovedCount)
            .approverList(approverList)
            .provisionsCount(provisionsCount)
            .deprovisionsCount(deprovisionsCount)
            .notificationsCount(notificationsCount)
            .httpCallsCount(httpCallsCount)
            .scriptsCount(scriptsCount)
            .simulatedActions(simulatedActions)
            .conditionsEvaluated(conditionsEvaluated)
            .branchesTaken(branchesTaken)
            .riskLevel(riskLevel)
            .potentialIssues(potentialIssues)
            .build();
    }

    /**
     * Build simulated action description
     */
    private SimulationResultDTO.SimulatedAction buildSimulatedAction(
            WorkflowNodeExecutionDTO node, String actionType) {
        Map<String, Object> outputData = node.getOutputData();
        boolean simulated = outputData != null && Boolean.TRUE.equals(outputData.get("simulated"));
        
        Map<String, Object> wouldExecute = new HashMap<>();
        if (outputData != null) {
            wouldExecute.putAll(outputData);
        }
        
        return SimulationResultDTO.SimulatedAction.builder()
            .nodeId(node.getNodeId())
            .actionType(actionType)
            .description(buildActionDescription(node, actionType))
            .wouldExecute(wouldExecute)
            .wouldSucceed("COMPLETED".equals(node.getStatus()))
            .reason(simulated ? "Simulated - no real changes made" : "Executed normally")
            .build();
    }

    /**
     * Build human-readable action description
     */
    private String buildActionDescription(WorkflowNodeExecutionDTO node, String actionType) {
        Map<String, Object> inputData = node.getInputData();
        if (inputData == null) {
            return actionType + " action";
        }
        
        // Customize based on action type
        if ("Provision".equals(actionType)) {
            return "Would provision access/identity";
        } else if ("Deprovision".equals(actionType)) {
            return "Would revoke access/identity";
        } else if ("Notification".equals(actionType)) {
            return "Would send notification";
        } else if ("HTTP Request".equals(actionType)) {
            return "Would make HTTP request";
        }
        
        return actionType + " action";
    }

    /**
     * Assess overall risk level
     */
    private String assessRiskLevel(WorkflowExecutionDTO execution, int provisions, int deprovisions) {
        // Simple risk assessment logic
        if ("FAILED".equals(execution.getStatus())) {
            return "HIGH";
        }
        
        int totalActions = provisions + deprovisions;
        if (totalActions > 10) {
            return "HIGH";
        } else if (totalActions > 5) {
            return "MEDIUM";
        }
        
        return "LOW";
    }

    /**
     * Identify potential issues
     */
    private List<String> identifyPotentialIssues(
            WorkflowExecutionDTO execution, 
            List<WorkflowNodeExecutionDTO> nodes) {
        List<String> issues = new ArrayList<>();
        
        if ("FAILED".equals(execution.getStatus())) {
            issues.add("Simulation failed - workflow may have errors");
        }
        
        long failedNodes = nodes.stream()
            .filter(n -> "FAILED".equals(n.getStatus()))
            .count();
        if (failedNodes > 0) {
            issues.add(failedNodes + " node(s) failed during simulation");
        }
        
        return issues;
    }

    /**
     * Generate warnings
     */
    private List<String> generateWarnings(WorkflowExecutionDTO execution) {
        List<String> warnings = new ArrayList<>();
        
        warnings.add("This is a simulation - no real changes were made");
        
        if (execution.getNodeExecutions() != null) {
            boolean hasHttpOrNotify = execution.getNodeExecutions().stream()
                .anyMatch(n -> n.getStepType() != null && 
                              (n.getStepType().equals("action.http") || 
                               n.getStepType().equals("action.notify")));
            
            if (hasHttpOrNotify) {
                warnings.add("Some HTTP/notification actions may have executed - check logs");
            }
        }
        
        return warnings;
    }

    /**
     * Generate recommendations
     */
    private List<String> generateRecommendations(
            WorkflowExecutionDTO execution, 
            SimulationResultDTO.SimulationSummary summary) {
        List<String> recommendations = new ArrayList<>();
        
        if ("COMPLETED".equals(execution.getStatus())) {
            recommendations.add("Simulation completed successfully - ready for real execution");
        }
        
        if (summary.getTotalApprovals() != null && summary.getTotalApprovals() > 0) {
            recommendations.add("Ensure approvers are available before real execution");
        }
        
        if (summary.getProvisionsCount() != null && summary.getProvisionsCount() > 5) {
            recommendations.add("Large number of provisions - consider batch processing");
        }
        
        return recommendations;
    }

    /**
     * Persist simulation to database
     * Note: Does NOT persist nodeExecutions to avoid JSONB serialization issues
     */
    @Transactional
    private void persistSimulation(
            UUID workflowId, 
            UUID executionId, 
            SimulationResultDTO result,
            Map<String, Object> scenario,
            UUID createdBy) {
        
        // Build result map - exclude nodeExecutions and other complex objects that might have JSONB issues
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("status", result.getStatus());
        resultMap.put("totalSteps", result.getTotalSteps());
        resultMap.put("stepsCompleted", result.getStepsCompleted());
        // Skip summary - it might contain references to corrupt data
        // resultMap.put("summary", result.getSummary());
        resultMap.put("warnings", result.getWarnings());
        resultMap.put("recommendations", result.getRecommendations());
        resultMap.put("executionId", executionId.toString());
        
        WorkflowSimulation simulation = WorkflowSimulation.builder()
            .workflowId(workflowId)
            .scenario(scenario != null ? scenario : new HashMap<>())
            .options(new HashMap<>())
            .result(resultMap)
            .createdBy(createdBy)
            .createdAt(OffsetDateTime.now())
            .build();
        
        WorkflowSimulation saved = simulationRepository.save(simulation);
        result.setSimulationId(saved.getId());
        
        logger.info()
            .message("Simulation persisted to database")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowSimulation")
            .objectId(saved.getId().toString())
            .log();
    }

    /**
     * Reconstruct simulation result from database entity
     */
    private SimulationResultDTO reconstructSimulationResult(WorkflowSimulation simulation) {
        Map<String, Object> result = simulation.getResult();
        if (result == null) {
            result = new HashMap<>();
        }
        
        // Try to get full execution details
        UUID executionId = null;
        if (result.get("executionId") != null) {
            try {
                executionId = UUID.fromString(result.get("executionId").toString());
            } catch (Exception e) {
                logger.warn()
                    .message("Failed to parse executionId from simulation result")
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        WorkflowExecutionDTO execution = null;
        if (executionId != null) {
            try {
                execution = executionService.getExecution(executionId);
            } catch (Exception e) {
                logger.warn()
                    .message("Failed to retrieve execution for simulation")
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("executionId", executionId.toString())
                    .log();
            }
        }
        
        return SimulationResultDTO.builder()
            .simulationId(simulation.getId())
            .workflowId(simulation.getWorkflowId())
            .executionId(executionId)
            .simulatedAt(simulation.getCreatedAt())
            .simulatedBy(simulation.getCreatedBy())
            .status((String) result.get("status"))
            .totalSteps((Integer) result.get("totalSteps"))
            .stepsCompleted((Integer) result.get("stepsCompleted"))
            .summary(extractSummary(result.get("summary")))
            .nodeExecutions(execution != null ? execution.getNodeExecutions() : null)
            .pathTrace(execution != null ? execution.getPathTrace() : null)
            .inputScenario(simulation.getScenario())
            .outputContext(execution != null ? execution.getExecutionContext() : null)
            .warnings(extractList(result.get("warnings")))
            .recommendations(extractList(result.get("recommendations")))
            .build();
    }

    /**
     * Extract summary from stored result
     */
    @SuppressWarnings("unchecked")
    private SimulationResultDTO.SimulationSummary extractSummary(Object summaryObj) {
        if (!(summaryObj instanceof Map)) {
            return null;
        }
        
        Map<String, Object> map = (Map<String, Object>) summaryObj;
        return SimulationResultDTO.SimulationSummary.builder()
            .totalApprovals((Integer) map.get("totalApprovals"))
            .autoApprovedCount((Integer) map.get("autoApprovedCount"))
            .approverList((List<String>) map.get("approverList"))
            .provisionsCount((Integer) map.get("provisionsCount"))
            .deprovisionsCount((Integer) map.get("deprovisionsCount"))
            .notificationsCount((Integer) map.get("notificationsCount"))
            .httpCallsCount((Integer) map.get("httpCallsCount"))
            .scriptsCount((Integer) map.get("scriptsCount"))
            .conditionsEvaluated((Integer) map.get("conditionsEvaluated"))
            .branchesTaken((Map<String, Object>) map.get("branchesTaken"))
            .riskLevel((String) map.get("riskLevel"))
            .potentialIssues((List<String>) map.get("potentialIssues"))
            .build();
    }

    /**
     * Extract list from object
     */
    @SuppressWarnings("unchecked")
    private List<String> extractList(Object obj) {
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
    }

    /**
     * Compare simulation with real execution
     */
    private SimulationComparisonDTO.ComparisonSummary compareExecutions(
            SimulationResultDTO simulation, 
            WorkflowExecutionDTO realExecution) {
        
        // Compare paths
        boolean pathsMatch = comparePaths(simulation.getPathTrace(), realExecution.getPathTrace());
        
        // Compare nodes
        List<SimulationComparisonDTO.NodeDifference> differences = compareNodes(
            simulation.getNodeExecutions(), 
            realExecution.getNodeExecutions()
        );
        
        int totalNodes = simulation.getNodeExecutions() != null ? 
            simulation.getNodeExecutions().size() : 0;
        int differingNodes = differences.size();
        int matchingNodes = totalNodes - differingNodes;
        
        return SimulationComparisonDTO.ComparisonSummary.builder()
            .pathsMatch(pathsMatch)
            .totalNodes(totalNodes)
            .matchingNodes(matchingNodes)
            .differingNodes(differingNodes)
            .differences(differences)
            .simulationMetrics(buildMetrics(simulation))
            .realMetrics(buildMetrics(realExecution))
            .build();
    }

    private boolean comparePaths(List<Map<String, Object>> path1, List<Map<String, Object>> path2) {
        if (path1 == null && path2 == null) return true;
        if (path1 == null || path2 == null) return false;
        if (path1.size() != path2.size()) return false;
        
        // Compare node IDs in path
        for (int i = 0; i < path1.size(); i++) {
            String nodeId1 = (String) path1.get(i).get("nodeId");
            String nodeId2 = (String) path2.get(i).get("nodeId");
            if (!Objects.equals(nodeId1, nodeId2)) {
                return false;
            }
        }
        
        return true;
    }

    private List<SimulationComparisonDTO.NodeDifference> compareNodes(
            List<WorkflowNodeExecutionDTO> simNodes, 
            List<WorkflowNodeExecutionDTO> realNodes) {
        List<SimulationComparisonDTO.NodeDifference> differences = new ArrayList<>();
        
        if (simNodes == null || realNodes == null) {
            return differences;
        }
        
        // Create maps for easy lookup
        Map<String, WorkflowNodeExecutionDTO> simMap = simNodes.stream()
            .collect(Collectors.toMap(WorkflowNodeExecutionDTO::getNodeId, n -> n, (a, b) -> a));
        Map<String, WorkflowNodeExecutionDTO> realMap = realNodes.stream()
            .collect(Collectors.toMap(WorkflowNodeExecutionDTO::getNodeId, n -> n, (a, b) -> a));
        
        // Compare matching nodes
        for (String nodeId : simMap.keySet()) {
            if (realMap.containsKey(nodeId)) {
                WorkflowNodeExecutionDTO simNode = simMap.get(nodeId);
                WorkflowNodeExecutionDTO realNode = realMap.get(nodeId);
                
                if (!Objects.equals(simNode.getStatus(), realNode.getStatus())) {
                    differences.add(SimulationComparisonDTO.NodeDifference.builder()
                        .nodeId(nodeId)
                        .difference("Status mismatch")
                        .simulatedValue(simNode.getStatus())
                        .realValue(realNode.getStatus())
                        .build());
                }
            }
        }
        
        return differences;
    }

    private Map<String, Object> buildMetrics(SimulationResultDTO simulation) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("durationMs", simulation.getSimulationDurationMs());
        metrics.put("totalSteps", simulation.getTotalSteps());
        metrics.put("status", simulation.getStatus());
        return metrics;
    }

    private Map<String, Object> buildMetrics(WorkflowExecutionDTO execution) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("durationMs", execution.getDurationMs());
        metrics.put("totalSteps", execution.getTotalSteps());
        metrics.put("status", execution.getStatus());
        return metrics;
    }
}
