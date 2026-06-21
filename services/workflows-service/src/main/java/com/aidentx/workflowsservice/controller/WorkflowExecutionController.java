package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.ApiResponse;
import com.aidentx.workflowsservice.dto.response.WorkflowExecutionDTO;
import com.aidentx.workflowsservice.service.WorkflowExecutionService;
import com.aidentx.workflowsservice.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST Controller for Workflow Execution Operations
 * Handles workflow execution, monitoring, and control
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = API_TAG_WORKFLOW_EXECUTION, description = API_DESC_WORKFLOW_EXECUTION)
public class WorkflowExecutionController {

    private final IUnifiedLogger logger;
    private final WorkflowExecutionService executionService;
    private final JwtUtils jwtUtils;

    public WorkflowExecutionController(
            WorkflowExecutionService executionService,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.executionService = executionService;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger(WorkflowExecutionController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    /**
     * Execute a workflow
     * 
     * POST /api/v1/workflows/{id}/execute
     */
    @PostMapping("/workflows/{id}/execute")
    @Operation(summary = API_SUMMARY_EXECUTE_WORKFLOW, description = API_DESC_EXECUTE_WORKFLOW)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = API_RESPONSE_202_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_404_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = API_RESPONSE_400_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> executeWorkflow(
        @Parameter(description = API_DESC_WORKFLOW_ID_PARAM)
        @PathVariable UUID id,
        @Valid @RequestBody ExecuteWorkflowRequest request,
        @RequestHeader(value = HEADER_USER_ID, required = false) UUID triggeredBy,
        HttpServletRequest httpRequest
    ) {
        // Extract tenant and user info from JWT
        String tenantId = null;
        String userId = null;
        String username = null;
        String jwtToken = null;
        try {
            tenantId = jwtUtils.extractTenantId(httpRequest);
            userId = jwtUtils.extractUserId(httpRequest);
            username = jwtUtils.extractUsername(httpRequest);
            
            // Extract JWT token from Authorization header for downstream service calls
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }
            
            logger.info()
                .message(LOG_EXECUTING_WORKFLOW_CONTROLLER)
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(id.toString())
                .detail("tenantId", tenantId)
                .detail("username", username)
                .detail("userId", userId)
                .log();
        } catch (Exception e) {
            logger.warn()
                .message(LOG_FAILED_EXTRACT_JWT_CLAIMS)
                .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Add tenant and user context to execution context
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
            request.getContext().put("triggeredByUsername", username);
        }
        if (jwtToken != null) {
            request.getContext().put("jwtToken", jwtToken);
        }
        
        // Add client IP and user agent for audit logging
        String clientIp = extractClientIp(httpRequest);
        if (clientIp != null) {
            request.getContext().put("clientIp", clientIp);
        }
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent != null) {
            request.getContext().put("userAgent", userAgent);
        }
        
        WorkflowExecutionDTO execution = executionService.executeWorkflow(id, request, triggeredBy);
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(SUCCESS_WORKFLOW_EXECUTION_STARTED, execution));
    }

    /**
     * Get all executions across all workflows
     * 
     * GET /api/v1/executions
     */
    @GetMapping("/executions")
    @Operation(summary = "Get all executions", description = "Retrieve all workflow executions with optional filtering and pagination")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Executions retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<WorkflowExecutionDTO>>> getAllExecutions(
        @Parameter(description = API_DESC_PAGE_NUMBER_PARAM)
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = API_DESC_PAGE_SIZE_PARAM)
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = API_DESC_SORT_FIELD_PARAM)
        @RequestParam(defaultValue = "startedAt") String sortBy,
        @Parameter(description = API_DESC_SORT_DIRECTION_PARAM)
        @RequestParam(defaultValue = "DESC") String sortDir,
        @Parameter(description = API_DESC_FILTER_STATUS_PARAM)
        @RequestParam(required = false) String status,
        @Parameter(description = "Whether to include detailed node execution payloads (input/output) in the response")
        @RequestParam(defaultValue = "false") boolean includeDetails
    ) {
        logger.info()
            .message(LOG_GETTING_ALL_EXECUTIONS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("page", String.valueOf(page))
            .detail("size", String.valueOf(size))
            .detail("status", status)
            .log();
        
        Sort sort = Sort.by(
            sortDir.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<WorkflowExecutionDTO> executions = executionService.getAllExecutions(status, pageable, includeDetails);
        
        return ResponseEntity.ok(
            ApiResponse.success("Executions retrieved successfully", executions)
        );
    }

    /**
     * Get execution details
     * 
     * GET /api/v1/executions/{id}
     */
    @GetMapping("/executions/{id}")
    @Operation(summary = API_SUMMARY_GET_EXECUTION, description = API_DESC_GET_EXECUTION)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = API_RESPONSE_200_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_404_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> getExecution(
        @Parameter(description = API_DESC_EXECUTION_ID_PARAM)
        @PathVariable UUID id
    ) {
        logger.info()
            .message(LOG_GETTING_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(id.toString())
            .log();
        WorkflowExecutionDTO execution = executionService.getExecution(id);
        return ResponseEntity.ok(
            ApiResponse.success(SUCCESS_EXECUTION_RETRIEVED, execution)
        );
    }

    /**
     * Get execution trace (path through workflow)
     * 
     * GET /api/v1/executions/{id}/trace
     */
    @GetMapping("/executions/{id}/trace")
    @Operation(summary = API_SUMMARY_GET_EXECUTION_TRACE, description = API_DESC_GET_EXECUTION_TRACE)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = API_RESPONSE_200_TRACE_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_404_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionTrace(
        @Parameter(description = API_DESC_EXECUTION_ID_PARAM)
        @PathVariable UUID id
    ) {
        logger.info()
            .message(LOG_GETTING_EXECUTION_TRACE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(id.toString())
            .log();
        Map<String, Object> trace = executionService.getExecutionTrace(id);
        return ResponseEntity.ok(
            ApiResponse.success(SUCCESS_EXECUTION_TRACE_RETRIEVED, trace)
        );
    }

    /**
     * Cancel a running execution
     * 
     * POST /api/v1/executions/{id}/cancel
     */
    @PostMapping("/executions/{id}/cancel")
    @Operation(summary = API_SUMMARY_CANCEL_EXECUTION, description = API_DESC_CANCEL_EXECUTION)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = API_RESPONSE_EXECUTION_CANCELLED_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_EXECUTION_NOT_FOUND_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = API_RESPONSE_EXECUTION_CANNOT_CANCEL_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> cancelExecution(
        @Parameter(description = API_DESC_EXECUTION_ID_PARAM)
        @PathVariable UUID id,
        @RequestHeader(value = HEADER_USER_ID, required = false) UUID cancelledBy,
        @RequestHeader(value = HEADER_USERNAME, required = false) String cancelledByUsername
    ) {
        logger.info()
            .message(LOG_CANCELLING_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(id.toString())
            .detail("cancelledBy", cancelledBy != null ? cancelledBy.toString() : "unknown")
            .detail("cancelledByUsername", cancelledByUsername != null ? cancelledByUsername : "unknown")
            .log();
        WorkflowExecutionDTO execution = executionService.cancelExecution(id, cancelledBy, cancelledByUsername);
        return ResponseEntity.ok(
            ApiResponse.success(SUCCESS_EXECUTION_CANCELLED, execution)
        );
    }

    /**
     * Get all executions for a workflow
     * 
     * GET /api/v1/workflows/{id}/executions
     */
    @GetMapping("/workflows/{id}/executions")
    @Operation(summary = API_SUMMARY_GET_WORKFLOW_EXECUTIONS, description = API_DESC_GET_WORKFLOW_EXECUTIONS)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = API_RESPONSE_EXECUTIONS_RETRIEVED_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_404_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<Page<WorkflowExecutionDTO>>> getWorkflowExecutions(
        @Parameter(description = API_DESC_WORKFLOW_ID_PARAM)
        @PathVariable UUID id,
        @Parameter(description = API_DESC_PAGE_NUMBER_PARAM)
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = API_DESC_PAGE_SIZE_PARAM)
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = API_DESC_SORT_FIELD_PARAM)
        @RequestParam(defaultValue = "startedAt") String sortBy,
        @Parameter(description = API_DESC_SORT_DIRECTION_PARAM)
        @RequestParam(defaultValue = "DESC") String sortDir,
        @Parameter(description = API_DESC_FILTER_STATUS_PARAM)
        @RequestParam(required = false) String status,
        @Parameter(description = "Whether to include detailed node execution payloads (input/output) in the response")
        @RequestParam(defaultValue = "false") boolean includeDetails
    ) {
        logger.info()
            .message(LOG_GETTING_WORKFLOW_EXECUTIONS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .log();
        
        Sort sort = Sort.by(
            sortDir.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<WorkflowExecutionDTO> executions = executionService.getWorkflowExecutions(id, status, pageable, includeDetails);
        
        return ResponseEntity.ok(
            ApiResponse.success(SUCCESS_WORKFLOW_EXECUTIONS_RETRIEVED, executions)
        );
    }

    /**
     * Retry a failed execution
     * 
     * POST /api/v1/executions/{id}/retry
     */
    @PostMapping("/executions/{id}/retry")
    @Operation(summary = API_SUMMARY_RETRY_EXECUTION, description = API_DESC_RETRY_EXECUTION)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = API_RESPONSE_RETRY_STARTED_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = API_RESPONSE_EXECUTION_NOT_FOUND_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = API_RESPONSE_EXECUTION_CANNOT_RETRY_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<WorkflowExecutionDTO>> retryExecution(
        @Parameter(description = API_DESC_EXECUTION_ID_PARAM)
        @PathVariable UUID id,
        @RequestHeader(value = HEADER_USER_ID, required = false) UUID retriedBy
    ) {
        logger.info()
            .message(LOG_RETRYING_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(id.toString())
            .detail("retriedBy", retriedBy != null ? retriedBy.toString() : "unknown")
            .log();
        WorkflowExecutionDTO execution = executionService.retryExecution(id, retriedBy);
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(SUCCESS_EXECUTION_RETRY_STARTED, execution));
    }
    
    /**
     * Get global execution statistics
     * 
     * GET /api/v1/executions/statistics
     */
    @GetMapping("/executions/statistics")
    @Operation(summary = "Get execution statistics", description = "Retrieve global statistics for all workflow executions")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionStatistics() {
        logger.info()
            .message(LOG_GETTING_EXECUTION_STATISTICS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .log();
        Map<String, Object> stats = executionService.getExecutionStatistics();
        return ResponseEntity.ok(
            ApiResponse.success("Statistics retrieved successfully", stats)
        );
    }
    
    /**
     * Extract client IP address from HTTP request
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
