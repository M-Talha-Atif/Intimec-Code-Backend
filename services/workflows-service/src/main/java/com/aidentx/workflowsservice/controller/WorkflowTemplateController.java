package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.dto.request.CreateTemplateRequest;
import com.aidentx.workflowsservice.dto.request.InstantiateTemplateRequest;
import com.aidentx.workflowsservice.dto.request.UpdateTemplateRequest;
import com.aidentx.workflowsservice.dto.response.WorkflowResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowTemplateDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import com.aidentx.workflowsservice.service.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Workflow Templates
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Workflow Templates", description = "Manage reusable workflow templates")
public class WorkflowTemplateController {

    private final IUnifiedLogger logger;
    private final WorkflowTemplateService templateService;

    public WorkflowTemplateController(WorkflowTemplateService templateService, UnifiedLoggerFactory loggerFactory) {
        this.templateService = templateService;
        this.logger = loggerFactory.getLogger(WorkflowTemplateController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    /**
     * Create a new workflow template
     */
    @PostMapping
    @Operation(summary = "Create a new workflow template")
    public ResponseEntity<WorkflowTemplateDTO> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        logger.info()
            .message(LOG_CREATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("templateName", request.getName())
            .log();

        WorkflowTemplate template = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .framework(request.getFramework())
                .complexity(request.getComplexity())
                .nodes(request.getNodes())
                .edges(request.getEdges())
                .variables(request.getVariables())
                .tags(request.getTags())
                .steps(request.getSteps())
                .estimatedTime(request.getEstimatedTime())
                .complianceFrameworks(request.getComplianceFrameworks())
                .prerequisites(request.getPrerequisites())
                .isSystemTemplate(request.getIsSystemTemplate())
                .version("1.0")
                .usageCount(0)
                .rating(BigDecimal.ZERO)
                .downloads(0)
                .build();

        WorkflowTemplate created = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowTemplateDTO.from(created));
    }

    /**
     * Get all templates with optional filters
     */
    @GetMapping
    @Operation(summary = "List workflow templates with optional filters")
    public ResponseEntity<List<WorkflowTemplateDTO>> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String framework,
            @RequestParam(required = false) String complexity,
            @RequestParam(required = false) Boolean systemOnly,
            @RequestParam(required = false) String sort
    ) {
        logger.info()
            .message(LOG_LISTING_TEMPLATES)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("category", category)
            .detail("framework", framework)
            .detail("complexity", complexity)
            .log();

        List<WorkflowTemplate> templates;

        if (category != null) {
            templates = templateService.listByCategory(category);
        } else if (framework != null) {
            templates = templateService.listByFramework(framework);
        } else if (complexity != null) {
            templates = templateService.listByComplexity(WorkflowTemplate.Complexity.valueOf(complexity));
        } else if (Boolean.TRUE.equals(systemOnly)) {
            templates = templateService.listSystemTemplates();
        } else if ("popular".equals(sort)) {
            templates = templateService.getPopularTemplates(5);
        } else if ("rated".equals(sort)) {
            templates = templateService.getTopRatedTemplates();
        } else {
            templates = templateService.listTemplates();
        }

        List<WorkflowTemplateDTO> response = templates.stream()
                .map(WorkflowTemplateDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get template by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a workflow template by ID")
    public ResponseEntity<WorkflowTemplateDTO> getTemplate(@PathVariable UUID id) {
        logger.info()
            .message(LOG_GETTING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowTemplate")
            .objectId(id.toString())
            .log();

        WorkflowTemplate template = templateService.getTemplate(id);
        return ResponseEntity.ok(WorkflowTemplateDTO.from(template));
    }

    /**
     * Update an existing template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing workflow template")
    public ResponseEntity<WorkflowTemplateDTO> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request
    ) {
        logger.info()
            .message(LOG_UPDATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowTemplate")
            .objectId(id.toString())
            .log();

        WorkflowTemplate updatedTemplate = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .framework(request.getFramework())
                .complexity(request.getComplexity())
                .nodes(request.getNodes())
                .edges(request.getEdges())
                .variables(request.getVariables())
                .tags(request.getTags())
                .steps(request.getSteps())
                .estimatedTime(request.getEstimatedTime())
                .complianceFrameworks(request.getComplianceFrameworks())
                .prerequisites(request.getPrerequisites())
                .build();

        WorkflowTemplate updated = templateService.updateTemplate(id, updatedTemplate);
        return ResponseEntity.ok(WorkflowTemplateDTO.from(updated));
    }

    /**
     * Delete a template
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        logger.info()
            .message(LOG_DELETING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowTemplate")
            .objectId(id.toString())
            .log();

        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Instantiate a template into a new workflow
     */
    @PostMapping("/{id}/instantiate")
    @Operation(summary = "Instantiate a template as a new workflow")
    public ResponseEntity<WorkflowResponseDTO> instantiateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody InstantiateTemplateRequest request
    ) {
        logger.info()
            .message(LOG_INSTANTIATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType("WorkflowTemplate")
            .objectId(id.toString())
            .detail("workflowName", request.getWorkflowName())
            .log();

        Workflow workflow = templateService.instantiateTemplate(
                id,
                request.getWorkflowName(),
                request.getVariableValues() != null ? request.getVariableValues() : Map.of()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowResponseDTO.from(workflow));
    }
}
