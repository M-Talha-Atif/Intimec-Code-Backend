package com.aidentx.workflowsservice.controller;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.ApiResponse;
import com.aidentx.workflowsservice.dto.response.SimulationComparisonDTO;
import com.aidentx.workflowsservice.dto.response.SimulationResultDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowExecutionDTO;
import com.aidentx.workflowsservice.service.WorkflowSimulationService;
import com.aidentx.workflowsservice.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST Controller for Workflow Simulation Operations
 * Provides dedicated endpoints for testing workflows in simulation mode
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Workflow Simulation", description = "Test workflows without making real changes")
public class WorkflowSimulationController {

    private final IUnifiedLogger logger;
    private final WorkflowSimulationService simulationService;
    private final JwtUtils jwtUtils;

    public WorkflowSimulationController(
            WorkflowSimulationService simulationService,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.simulationService = simulationService;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger(WorkflowSimulationController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    /**
     * Run a workflow simulation
     * 
     * POST /api/v1/workflows/{id}/simulate
     */
    @PostMapping("/workflows/{id}/simulate")
    @Operation(
        summary = "Simulate workflow execution",
        description = "Execute a workflow in simulation mode to test logic without making real changes. " +
                     "Provisions, deprovisions, HTTP calls, notifications, and scripts will be simulated."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Simulation completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workflow not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid simulation request")
    })
    public ResponseEntity<ApiResponse<SimulationResultDTO>> simulateWorkflow(
        @Parameter(description = "Workflow ID")
        @PathVariable UUID id,
        @Valid @RequestBody ExecuteWorkflowRequest request,
        @RequestHeader(value = HEADER_USER_ID, required = false) UUID simulatedBy,
        HttpServletRequest httpRequest
    ) {
        // Extract user info from JWT
        String tenantId = null;
        String userId = null;
        String username = null;
        try {
            tenantId = jwtUtils.extractTenantId(httpRequest);
            userId = jwtUtils.extractUserId(httpRequest);
            username = jwtUtils.extractUsername(httpRequest);
            
            logger.info()
                .message("Starting workflow simulation")
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(id.toString())
                .detail("tenantId", tenantId)
                .detail("username", username)
                .detail("userId", userId)
                .log();
        } catch (Exception e) {
            logger.warn()
                .message("Failed to extract JWT claims for simulation")
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Add tenant and user context
        if (request.getContext() == null) {
            request.setContext(new java.util.HashMap<>());
        }
        if (tenantId != null) {
            request.getContext().put(CONTEXT_KEY_TENANT_ID, tenantId);
        }
        if (userId != null) {
            request.getContext().put(CONTEXT_KEY_USER_ID, userId);
        }
        if (username != null) {
            request.getContext().put("username", username);
            request.getContext().put("simulatedByUsername", username);
        }
        
        // Run simulation
        SimulationResultDTO result = simulationService.simulate(id, request, simulatedBy);
        
        return ResponseEntity.ok(
            ApiResponse.success("Simulation completed successfully", result)
        );
    }

    /**
     * Get simulation by ID
     * 
     * GET /api/v1/simulations/{id}
     */
    @GetMapping("/simulations/{id}")
    @Operation(
        summary = "Get simulation details",
        description = "Retrieve detailed results of a specific simulation"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Simulation retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<ApiResponse<SimulationResultDTO>> getSimulation(
        @Parameter(description = "Simulation ID")
        @PathVariable UUID id
    ) {
        logger.info()
            .message("Retrieving simulation")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowSimulation")
            .objectId(id.toString())
            .log();
        
        SimulationResultDTO result = simulationService.getSimulation(id);
        return ResponseEntity.ok(
            ApiResponse.success("Simulation retrieved successfully", result)
        );
    }

    /**
     * Get simulation history for a workflow
     * 
     * GET /api/v1/workflows/{id}/simulations
     */
    @GetMapping("/workflows/{id}/simulations")
    @Operation(
        summary = "Get workflow simulation history",
        description = "Retrieve all past simulations for a specific workflow, ordered by most recent first"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Simulations retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<ApiResponse<List<SimulationResultDTO>>> getWorkflowSimulations(
        @Parameter(description = "Workflow ID")
        @PathVariable UUID id
    ) {
        logger.info()
            .message("Retrieving workflow simulation history")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .log();
        
        List<SimulationResultDTO> simulations = simulationService.getSimulationHistory(id);
        return ResponseEntity.ok(
            ApiResponse.success("Simulation history retrieved successfully", simulations)
        );
    }

    /**
     * Get simulations by user
     * 
     * GET /api/v1/users/{userId}/simulations
     */
    @GetMapping("/users/{userId}/simulations")
    @Operation(
        summary = "Get user's simulations",
        description = "Retrieve all simulations created by a specific user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Simulations retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<SimulationResultDTO>>> getUserSimulations(
        @Parameter(description = "User ID")
        @PathVariable UUID userId
    ) {
        logger.info()
            .message("Retrieving user simulations")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("User")
            .objectId(userId.toString())
            .log();
        
        List<SimulationResultDTO> simulations = simulationService.getSimulationsByUser(userId);
        return ResponseEntity.ok(
            ApiResponse.success("User simulations retrieved successfully", simulations)
        );
    }

    /**
     * Compare simulation with real execution
     * 
     * GET /api/v1/simulations/{simulationId}/compare/{executionId}
     */
    @GetMapping("/simulations/{simulationId}/compare/{executionId}")
    @Operation(
        summary = "Compare simulation with real execution",
        description = "Compare the results of a simulation with a real execution to identify differences"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comparison completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Simulation or execution not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot compare - different workflows")
    })
    public ResponseEntity<ApiResponse<SimulationComparisonDTO>> compareSimulationWithExecution(
        @Parameter(description = "Simulation ID")
        @PathVariable UUID simulationId,
        @Parameter(description = "Real execution ID")
        @PathVariable UUID executionId
    ) {
        logger.info()
            .message("Comparing simulation with real execution")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("simulationId", simulationId.toString())
            .detail("executionId", executionId.toString())
            .log();
        
        SimulationComparisonDTO comparison = simulationService.compareWithRealExecution(simulationId, executionId);
        return ResponseEntity.ok(
            ApiResponse.success("Comparison completed successfully", comparison)
        );
    }

    /**
     * Promote simulation to real execution
     * 
     * POST /api/v1/simulations/{id}/promote
     */
    @PostMapping("/simulations/{id}/promote")
    @Operation(
        summary = "Promote simulation to real execution",
        description = "Execute a workflow for real using the same scenario as a simulation. " +
                     "This creates a new execution with dryRun=false using the simulation's input scenario."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Real execution started successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> promoteSimulation(
        @Parameter(description = "Simulation ID")
        @PathVariable UUID id,
        @RequestHeader(value = HEADER_USER_ID, required = false) UUID promotedBy
    ) {
        logger.info()
            .message("Promoting simulation to real execution")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowSimulation")
            .objectId(id.toString())
            .detail("promotedBy", promotedBy != null ? promotedBy.toString() : "unknown")
            .log();
        
        WorkflowExecutionDTO execution = simulationService.promoteToRealExecution(id, promotedBy);
        
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success("Simulation promoted - real execution started", execution));
    }

    /**
     * Delete simulation
     * 
     * DELETE /api/v1/simulations/{id}
     */
    @DeleteMapping("/simulations/{id}")
    @Operation(
        summary = "Delete simulation",
        description = "Delete a simulation record. This does not affect the underlying execution record."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Simulation deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<Void> deleteSimulation(
        @Parameter(description = "Simulation ID")
        @PathVariable UUID id
    ) {
        logger.info()
            .message("Deleting simulation")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowSimulation")
            .objectId(id.toString())
            .log();
        
        simulationService.deleteSimulation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Approve or reject a simulation approval node
     * 
     * POST /api/v1/simulations/{executionId}/approve
     */
    @PostMapping("/simulations/{executionId}/approve")
    @Operation(
        summary = "Approve/reject simulation approval node",
        description = "During simulation, approve or reject an approval node to continue execution"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Approval processed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Execution not found")
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> approveSimulation(
        @Parameter(description = "Execution ID")
        @PathVariable UUID executionId,
        @RequestParam(name = "approved") boolean approved,
        @RequestParam(name = "reason", required = false) String reason,
        HttpServletRequest httpRequest
    ) {
        String userId = null;
        String username = null;
        try {
            userId = jwtUtils.extractUserId(httpRequest);
            username = jwtUtils.extractUsername(httpRequest);
        } catch (Exception e) {
            logger.warn()
                .message("Failed to extract user info for approval")
                .detail("error", e.getMessage())
                .log();
        }
        
        logger.info()
            .message("Processing simulation approval")
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("approved", approved)
            .detail("userId", userId)
            .detail("username", username)
            .log();
        
        WorkflowExecutionDTO result = simulationService.processSimulationApproval(
            executionId, 
            approved, 
            reason, 
            userId != null ? UUID.fromString(userId) : null
        );
        
        return ResponseEntity.ok(ApiResponse.success("Approval processed successfully", result));
    }
}
