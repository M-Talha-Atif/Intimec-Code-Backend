package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.dto.request.CreateWorkflowRequest;
import com.aidentx.workflowsservice.dto.request.PatchWorkflowRequest;
import com.aidentx.workflowsservice.dto.request.UpdateWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.ApiResponse;
import com.aidentx.workflowsservice.dto.response.WorkflowListResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowResponseDTO;
import com.aidentx.workflowsservice.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST Controller for Workflow operations
 */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = API_TAG_WORKFLOWS, description = API_DESC_WORKFLOWS)
public class WorkflowController {

    private final IUnifiedLogger logger;
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService, UnifiedLoggerFactory loggerFactory) {
        this.workflowService = workflowService;
        this.logger = loggerFactory.getLogger(WorkflowController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    /**
     * Create a new workflow
     * POST /api/v1/workflows
     */
    @PostMapping
    @Operation(
        summary = API_SUMMARY_CREATE_WORKFLOW,
        description = API_DESC_CREATE_WORKFLOW
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = API_RESPONSE_201_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = API_RESPONSE_400_DESCRIPTION),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = API_RESPONSE_409_DESCRIPTION)
    })
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> createWorkflow(
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = API_DESC_WORKFLOW_CREATION_REQUEST,
                required = true
            ) CreateWorkflowRequest request,
            @RequestHeader(value = HEADER_USER_ID, required = false) 
            @Parameter(description = API_DESC_USER_ID_PARAM) String userIdHeader) {

        UUID createdBy = parseUserID(userIdHeader);
        WorkflowResponseDTO workflow = workflowService.createWorkflow(request, createdBy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(SUCCESS_WORKFLOW_CREATED, workflow));
    }

    /**
     * Get workflow by ID
     * GET /api/v1/workflows/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> getWorkflow(@PathVariable UUID id) {
        WorkflowResponseDTO workflow = workflowService.getWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(workflow));
    }

    /**
     * List workflows with pagination and filters
     * GET /api/v1/workflows?page=0&size=10&status=DRAFT&category=ACCESS_REQUEST
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WorkflowListResponseDTO>> listWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        WorkflowListResponseDTO workflows = workflowService.listWorkflows(pageable, status, category);
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }

    /**
     * Update an existing workflow
     * PUT /api/v1/workflows/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> updateWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkflowRequest request,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader) {

        UUID modifiedBy = parseUserID(userIdHeader);
        WorkflowResponseDTO workflow = workflowService.updateWorkflow(id, request, modifiedBy);

        return ResponseEntity.ok(ApiResponse.success(SUCCESS_WORKFLOW_UPDATED, workflow));
    }

    /**
     * Partial update (PATCH) for auto-save functionality
     * PATCH /api/v1/workflows/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> patchWorkflow(
            @PathVariable UUID id,
            @RequestBody PatchWorkflowRequest request,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader) {

        UUID modifiedBy = parseUserID(userIdHeader);
        WorkflowResponseDTO workflow = workflowService.patchWorkflow(id, request, modifiedBy);

        return ResponseEntity.ok(ApiResponse.success(SUCCESS_WORKFLOW_UPDATED, workflow));
    }

    /**
     * Delete a workflow
     * DELETE /api/v1/workflows/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(SUCCESS_WORKFLOW_DELETED, null));
    }

    /**
     * Duplicate a workflow
     * POST /api/v1/workflows/{id}/duplicate?newName=Copy
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> duplicateWorkflow(
            @PathVariable UUID id,
            @RequestParam String newName,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader) {

        UUID createdBy = parseUserID(userIdHeader);
        WorkflowResponseDTO workflow = workflowService.duplicateWorkflow(id, newName, createdBy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(SUCCESS_WORKFLOW_DUPLICATED, workflow));
    }

    // Helper methods

    private UUID parseUserID(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            // Default user for testing (in production, this should come from JWT token)
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
}
