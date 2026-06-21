package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.CreateWorkflowRequest;
import com.aidentx.workflowsservice.dto.request.PatchWorkflowRequest;
import com.aidentx.workflowsservice.dto.request.UpdateWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.WorkflowListResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowResponseDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import com.aidentx.workflowsservice.entity.WorkflowVersion;
import com.aidentx.workflowsservice.exception.ResourceNotFoundException;
import com.aidentx.workflowsservice.exception.ValidationException;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.repository.WorkflowTemplateRepository;
import com.aidentx.workflowsservice.repository.WorkflowVersionRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.scheduler.WorkflowSchedulerService;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for Workflow CRUD operations
 */
@Service
@Transactional
public class WorkflowService {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowSchedulerService schedulerService;

    public WorkflowService(
            WorkflowRepository workflowRepository,
            WorkflowTemplateRepository templateRepository,
            WorkflowVersionRepository workflowVersionRepository,
            WorkflowExecutionRepository workflowExecutionRepository,
            WorkflowSchedulerService schedulerService,
            UnifiedLoggerFactory loggerFactory) {
        this.workflowRepository = workflowRepository;
        this.templateRepository = templateRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.schedulerService = schedulerService;
        this.logger = loggerFactory.getLogger(WorkflowService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Create a new workflow
     */
    public WorkflowResponseDTO createWorkflow(CreateWorkflowRequest request, UUID createdBy) {
        logger.info()
            .message(LOG_CREATING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .detail("name", request.getName())
            .detail("createdBy", createdBy)
            .detail("triggerType", request.getTriggerType())
            .detail("category", request.getCategory())
            .log();

        // Validate workflow name uniqueness
        if (workflowRepository.existsByName(request.getName())) {
            throw new ValidationException(String.format(ERROR_WORKFLOW_ALREADY_EXISTS, request.getName()));
        }

        // Convert request to entity
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .description(request.getDescription())
                .version("1.0")
                .status(Workflow.WorkflowStatus.DRAFT)
                .category(parseCategory(request.getCategory()))
                .triggerType(parseTriggerType(request.getTriggerType()))
                .eventConfig(request.getEventConfig())
                .cronSchedule(request.getCronSchedule())
                .nodes(convertNodesFromRequest(request.getNodes()))
                .edges(convertEdgesFromRequest(request.getEdges()))
                .applicationScope(request.getApplicationScope())
                .tags(request.getTags())
                .complianceFrameworks(request.getComplianceFrameworks())
                .riskScore(calculateRiskScore(request.getRiskLevel(), request.getRiskScore()))
                .hasTimeLogic(request.getHasTimeLogic() != null ? request.getHasTimeLogic() : false)
                .hasSodViolation(request.getHasSodViolation() != null ? request.getHasSodViolation() : false)
                .roleSensitivity(parseRoleSensitivity(request.getRoleSensitivity()))
                .policyCompliance(parsePolicyCompliance(request.getPolicyCompliance()))
                .sourceTemplateId(parseUUID(request.getSourceTemplateId()))
                .isConnectedToTemplate(request.getSourceTemplateId() != null)
                .templateMetadata(buildTemplateMetadata(request.getSourceTemplateId()))
                .permissions(request.getPermissions())
                .createdBy(parseUUID(request.getOwner()) != null ? parseUUID(request.getOwner()) : createdBy)
                .lastModifiedBy(createdBy)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Save workflow
        workflow = workflowRepository.save(workflow);
        
        // Increment template usage count if created from template
        if (request.getSourceTemplateId() != null) {
            incrementTemplateUsageCount(request.getSourceTemplateId());
        }

        // Create initial version snapshot
        createVersionSnapshot(workflow, VERSION_INITIAL, createdBy);

        // Schedule if time-based and active
        if (workflow.getTriggerType() == Workflow.TriggerType.TIME_BASED 
                && workflow.getStatus() == Workflow.WorkflowStatus.ACTIVE) {
            schedulerService.scheduleWorkflow(workflow);
        }

        logger.info()
            .message(LOG_WORKFLOW_CREATED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflow.getId().toString())
            .detail("name", workflow.getName())
            .detail("workflowId", workflow.getId())
            .detail("version", workflow.getVersion())
            .detail("status", workflow.getStatus())
            .detail("triggerType", workflow.getTriggerType())
            .log();
        return WorkflowResponseDTO.from(workflow);
    }

    /**
     * Get workflow by ID
     */
    @Transactional(readOnly = true)
    public WorkflowResponseDTO getWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBJECT_TYPE_WORKFLOW, FIELD_ID, id));
        return WorkflowResponseDTO.from(workflow);
    }

    /**
     * List workflows with pagination and filters
     */
    @Transactional(readOnly = true)
    public WorkflowListResponseDTO listWorkflows(Pageable pageable, String status, String category) {
        Page<Workflow> page;

        if (status != null && category != null) {
            page = workflowRepository.findByStatusAndCategory(status, category, pageable);
        } else if (status != null) {
            page = workflowRepository.findByStatus(status, pageable);
        } else if (category != null) {
            page = workflowRepository.findByCategory(category, pageable);
        } else {
            page = workflowRepository.findAll(pageable);
        }

        // Enrich workflows with execution statistics
        WorkflowListResponseDTO response = WorkflowListResponseDTO.from(page);
        if (response.getContent() != null) {
            response.getContent().forEach(this::enrichWithExecutionStats);
        }
        
        return response;
    }
    
    /**
     * Enrich a workflow DTO with execution statistics
     */
    private void enrichWithExecutionStats(WorkflowResponseDTO workflowDTO) {
        try {
            UUID workflowId = workflowDTO.getId();
            
            // Get execution count
            Long executionCount = workflowExecutionRepository.countByWorkflowId(workflowId);
            workflowDTO.setExecutionCount(executionCount != null ? executionCount : 0L);
            
            // Get average duration in milliseconds, convert to hours
            Double avgDurationMs = workflowExecutionRepository.getAverageDurationByWorkflowId(workflowId);
            if (avgDurationMs != null && avgDurationMs > 0) {
                workflowDTO.setAverageCompletionTime(avgDurationMs / (1000.0 * 60.0 * 60.0)); // ms to hours
            } else {
                workflowDTO.setAverageCompletionTime(0.0);
            }
            
            // Get last execution time
            OffsetDateTime lastExecuted = workflowExecutionRepository.getLastExecutionTime(workflowId);
            workflowDTO.setLastExecuted(lastExecuted);
            
        } catch (Exception e) {
            logger.warn()
                .message(LOG_FAILED_ENRICH_WORKFLOW_STATS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowDTO.getId().toString())
                .detail("error", e.getMessage())
                .log();
            // Set defaults on error
            workflowDTO.setExecutionCount(0L);
            workflowDTO.setAverageCompletionTime(0.0);
        }
    }

    /**
     * Update an existing workflow
     */
    public WorkflowResponseDTO updateWorkflow(UUID id, UpdateWorkflowRequest request, UUID modifiedBy) {
        logger.info()
            .message(LOG_UPDATING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .detail("modifiedBy", modifiedBy)
            .log();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        boolean structuralChange = false;
        StringBuilder changes = new StringBuilder();

        // Update fields
        if (request.getName() != null && !request.getName().equals(workflow.getName())) {
            // Check name uniqueness
            if (workflowRepository.existsByName(request.getName())) {
                throw new ValidationException(String.format(ERROR_WORKFLOW_ALREADY_EXISTS, request.getName()));
            }
            workflow.setName(request.getName());
            changes.append(VERSION_NAME_CHANGED);
        }

        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
            changes.append(VERSION_DESCRIPTION_UPDATED);
        }

        if (request.getStatus() != null) {
            workflow.setStatus(Workflow.WorkflowStatus.valueOf(request.getStatus()));
            changes.append(VERSION_STATUS_CHANGED).append(request.getStatus()).append("; ");
        }

        if (request.getCategory() != null) {
            workflow.setCategory(parseCategory(request.getCategory()));
            changes.append(VERSION_CATEGORY_CHANGED);
        }

        if (request.getTriggerType() != null) {
            workflow.setTriggerType(parseTriggerType(request.getTriggerType()));
        }

        if (request.getEventConfig() != null) {
            workflow.setEventConfig(request.getEventConfig());
        }

        if (request.getCronSchedule() != null) {
            workflow.setCronSchedule(request.getCronSchedule());
        }

        if (request.getNodes() != null) {
            workflow.setNodes(convertNodesFromRequest(request.getNodes()));
            structuralChange = true;
            changes.append(VERSION_NODES_UPDATED);
        }

        if (request.getEdges() != null) {
            workflow.setEdges(convertEdgesFromRequest(request.getEdges()));
            structuralChange = true;
            changes.append(VERSION_EDGES_UPDATED);
        }

        if (request.getApplicationScope() != null) {
            workflow.setApplicationScope(request.getApplicationScope());
        }

        if (request.getTags() != null) {
            workflow.setTags(request.getTags());
        }

        if (request.getComplianceFrameworks() != null) {
            workflow.setComplianceFrameworks(request.getComplianceFrameworks());
        }

        if (request.getPermissions() != null) {
            workflow.setPermissions(request.getPermissions());
        }

        if (request.getRiskScore() != null) {
            workflow.setRiskScore(request.getRiskScore());
        }

        if (request.getHasTimeLogic() != null) {
            workflow.setHasTimeLogic(request.getHasTimeLogic());
        }

        if (request.getHasSodViolation() != null) {
            workflow.setHasSodViolation(request.getHasSodViolation());
        }

        if (request.getRoleSensitivity() != null) {
            workflow.setRoleSensitivity(parseRoleSensitivity(request.getRoleSensitivity()));
        }

        if (request.getPolicyCompliance() != null) {
            workflow.setPolicyCompliance(parsePolicyCompliance(request.getPolicyCompliance()));
        }

        // Increment version on structural changes
        if (structuralChange) {
            String[] versionParts = workflow.getVersion().split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
            workflow.setVersion(major + "." + (minor + 1));
        }

        workflow.setLastModifiedBy(modifiedBy);
        workflow.setUpdatedAt(OffsetDateTime.now());

        // Save workflow
        workflow = workflowRepository.save(workflow);

        // Create version snapshot if structural change
        if (structuralChange) {
            createVersionSnapshot(workflow, changes.toString(), modifiedBy);
        }

        // Handle scheduling changes
        handleSchedulingChanges(workflow);

        logger.info()
            .message(LOG_WORKFLOW_UPDATED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .detail("name", workflow.getName())
            .detail("version", workflow.getVersion())
            .detail("structuralChange", structuralChange)
            .detail("changes", changes.toString())
            .log();
        return WorkflowResponseDTO.from(workflow);
    }

    /**
     * Partial update (PATCH) for auto-save functionality
     * Only updates provided fields, doesn't create version snapshots
     */
    public WorkflowResponseDTO patchWorkflow(UUID id, PatchWorkflowRequest request, UUID modifiedBy) {
        logger.debug()
            .message(LOG_PARTIALLY_UPDATING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .detail("modifiedBy", modifiedBy)
            .log();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        // Update only provided fields (no version increment for patches)
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(Workflow.WorkflowStatus.valueOf(request.getStatus()));
        }
        if (request.getCategory() != null) {
            workflow.setCategory(parseCategory(request.getCategory()));
        }
        if (request.getTriggerType() != null) {
            workflow.setTriggerType(parseTriggerType(request.getTriggerType()));
        }
        if (request.getEventConfig() != null) {
            workflow.setEventConfig(request.getEventConfig());
        }
        if (request.getCronSchedule() != null) {
            workflow.setCronSchedule(request.getCronSchedule());
        }
        if (request.getNodes() != null) {
            workflow.setNodes(convertNodesFromRequest(request.getNodes()));
        }
        if (request.getEdges() != null) {
            workflow.setEdges(convertEdgesFromRequest(request.getEdges()));
        }
        if (request.getApplicationScope() != null) {
            workflow.setApplicationScope(request.getApplicationScope());
        }
        if (request.getTags() != null) {
            workflow.setTags(request.getTags());
        }
        if (request.getPermissions() != null) {
            workflow.setPermissions(request.getPermissions());
        }
        if (request.getRiskScore() != null) {
            workflow.setRiskScore(request.getRiskScore());
        }
        if (request.getHasTimeLogic() != null) {
            workflow.setHasTimeLogic(request.getHasTimeLogic());
        }
        if (request.getHasSodViolation() != null) {
            workflow.setHasSodViolation(request.getHasSodViolation());
        }
        if (request.getRoleSensitivity() != null) {
            workflow.setRoleSensitivity(Workflow.RoleSensitivity.valueOf(request.getRoleSensitivity()));
        }
        if (request.getPolicyCompliance() != null) {
            workflow.setPolicyCompliance(Workflow.PolicyCompliance.valueOf(request.getPolicyCompliance()));
        }

        workflow.setLastModifiedBy(modifiedBy);
        workflow.setUpdatedAt(OffsetDateTime.now());

        workflow = workflowRepository.save(workflow);

        // Handle scheduling changes for patch as well
        handleSchedulingChanges(workflow);

        logger.debug()
            .message(LOG_WORKFLOW_PARTIALLY_UPDATED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .detail("name", workflow.getName())
            .log();
        return WorkflowResponseDTO.from(workflow);
    }

    /**
     * Delete a workflow
     */
    public void deleteWorkflow(UUID id) {
        logger.info()
            .message(LOG_DELETING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .log();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBJECT_TYPE_WORKFLOW, FIELD_ID, id));

        // Unschedule if it's a time-based workflow
        if (workflow.getTriggerType() == Workflow.TriggerType.TIME_BASED) {
            schedulerService.unscheduleWorkflow(id);
        }

        // Delete version history
        workflowVersionRepository.deleteByWorkflowId(id);

        // Delete workflow
        workflowRepository.delete(workflow);

        logger.info()
            .message(LOG_WORKFLOW_DELETED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("workflowId", id)
            .detail("name", workflow.getName())
            .detail("triggerType", workflow.getTriggerType())
            .log();
    }

    /**
     * Duplicate a workflow
     */
    public WorkflowResponseDTO duplicateWorkflow(UUID id, String newName, UUID createdBy) {
        logger.info()
            .message(LOG_DUPLICATING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(id.toString())
            .detail("newName", newName)
            .log();

        Workflow original = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBJECT_TYPE_WORKFLOW, FIELD_ID, id));

        // Check name uniqueness
        if (workflowRepository.existsByName(newName)) {
            throw new ValidationException(String.format(ERROR_WORKFLOW_ALREADY_EXISTS, newName));
        }

        // Deep copy
        Workflow duplicate = Workflow.builder()
                .name(newName)
                .description(original.getDescription() + DEFAULT_DUPLICATE_SUFFIX)
                .version(DEFAULT_VERSION)
                .status(Workflow.WorkflowStatus.DRAFT)
                .category(original.getCategory())
                .triggerType(original.getTriggerType())
                .nodes(deepCopyNodes(original.getNodes()))
                .edges(deepCopyEdges(original.getEdges()))
                .applicationScope(original.getApplicationScope() != null ? new ArrayList<>(original.getApplicationScope()) : null)
                .tags(original.getTags() != null ? new ArrayList<>(original.getTags()) : null)
                .sourceTemplateId(original.getSourceTemplateId())
                .isConnectedToTemplate(false)
                .permissions(original.getPermissions() != null ? new HashMap<>(original.getPermissions()) : null)
                .createdBy(createdBy)
                .lastModifiedBy(createdBy)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        duplicate = workflowRepository.save(duplicate);
        createVersionSnapshot(duplicate, String.format(VERSION_DUPLICATED_FROM, original.getName()), createdBy);

        logger.info()
            .message(LOG_WORKFLOW_DUPLICATED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(duplicate.getId().toString())
            .detail("originalId", id.toString())
            .log();
        return WorkflowResponseDTO.from(duplicate);
    }

    /**
     * Create version snapshot
     */
    private void createVersionSnapshot(Workflow workflow, String changes, UUID createdBy) {
        WorkflowVersion version = WorkflowVersion.builder()
                .workflowId(workflow.getId())
                .version(workflow.getVersion())
                .nodes(workflow.getNodes())
                .edges(workflow.getEdges())
                .changes(changes)
                .isActive(true)
                .createdBy(createdBy)
                .createdAt(OffsetDateTime.now())
                .build();

        workflowVersionRepository.save(version);
    }

    // Helper methods

    private Workflow.WorkflowCategory parseCategory(String category) {
        if (category == null) return null;
        try {
            return Workflow.WorkflowCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format(ERROR_INVALID_CATEGORY, category));
        }
    }

    private Workflow.TriggerType parseTriggerType(String triggerType) {
        if (triggerType == null) return null;
        try {
            return Workflow.TriggerType.valueOf(triggerType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format(ERROR_INVALID_TRIGGER_TYPE, triggerType));
        }
    }

    private Workflow.RoleSensitivity parseRoleSensitivity(String roleSensitivity) {
        if (roleSensitivity == null) return null;
        try {
            return Workflow.RoleSensitivity.valueOf(roleSensitivity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format("Invalid role sensitivity: %s", roleSensitivity));
        }
    }

    private Workflow.PolicyCompliance parsePolicyCompliance(String policyCompliance) {
        if (policyCompliance == null) return null;
        try {
            return Workflow.PolicyCompliance.valueOf(policyCompliance.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format("Invalid policy compliance: %s", policyCompliance));
        }
    }

    private Integer calculateRiskScore(String riskLevel, Integer explicitScore) {
        // If explicit score provided, use it
        if (explicitScore != null) return explicitScore;
        
        // Otherwise convert riskLevel string to score
        if (riskLevel == null) return 0;
        
        return switch (riskLevel.toLowerCase()) {
            case "low" -> 2;
            case "medium" -> 5;
            case "high" -> 8;
            default -> 0;
        };
    }

    private UUID parseUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format(ERROR_INVALID_UUID, uuidStr));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertNodesFromRequest(List<CreateWorkflowRequest.NodeDTO> nodeDTOs) {
        if (nodeDTOs == null) return new ArrayList<>();

        return nodeDTOs.stream().map(nodeDTO -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", nodeDTO.getId());
            node.put("type", nodeDTO.getType());
            if (nodeDTO.getData() != null) {
                node.put("data", nodeDTO.getData());
            }
            if (nodeDTO.getPosition() != null) {
                Map<String, Object> position = new HashMap<>();
                position.put("x", nodeDTO.getPosition().getX());
                position.put("y", nodeDTO.getPosition().getY());
                node.put("position", position);
            }
            return node;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> convertEdgesFromRequest(List<CreateWorkflowRequest.EdgeDTO> edgeDTOs) {
        if (edgeDTOs == null) return new ArrayList<>();

        return edgeDTOs.stream().map(edgeDTO -> {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", edgeDTO.getId());
            edge.put("source", edgeDTO.getSource());
            edge.put("target", edgeDTO.getTarget());
            if (edgeDTO.getSourceHandle() != null) edge.put("sourceHandle", edgeDTO.getSourceHandle());
            if (edgeDTO.getTargetHandle() != null) edge.put("targetHandle", edgeDTO.getTargetHandle());
            if (edgeDTO.getLabel() != null) edge.put("label", edgeDTO.getLabel());
            if (edgeDTO.getCondition() != null) edge.put("condition", edgeDTO.getCondition());
            if (edgeDTO.getData() != null) edge.put("data", edgeDTO.getData());
            return edge;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deepCopyNodes(List<Map<String, Object>> nodes) {
        if (nodes == null) return null;
        return nodes.stream()
                .map(node -> new HashMap<>(node))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> deepCopyEdges(List<Map<String, Object>> edges) {
        if (edges == null) return null;
        return edges.stream()
                .map(edge -> new HashMap<>(edge))
                .collect(Collectors.toList());
    }

    /**
     * Handle scheduling changes based on workflow trigger type, status, and cron expression
     */
    private void handleSchedulingChanges(Workflow workflow) {
        if (workflow.getTriggerType() != Workflow.TriggerType.TIME_BASED) {
            // Unschedule if it was previously time-based
            if (schedulerService.isScheduled(workflow.getId())) {
                logger.info()
                    .message(LOG_WORKFLOW_TRIGGER_TYPE_CHANGED_UNSCHEDULING)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflow.getId().toString())
                    .log();
                schedulerService.unscheduleWorkflow(workflow.getId());
            }
            return;
        }

        // TIME_BASED workflow
        if (workflow.getStatus() == Workflow.WorkflowStatus.ACTIVE 
                && workflow.getCronSchedule() != null 
                && !workflow.getCronSchedule().isBlank()) {
            // Schedule or reschedule
            if (schedulerService.isScheduled(workflow.getId())) {
                logger.info()
                    .message(LOG_RESCHEDULING_TIME_BASED_WORKFLOW)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflow.getId().toString())
                    .log();
                schedulerService.rescheduleWorkflow(workflow);
            } else {
                logger.info()
                    .message(LOG_SCHEDULING_TIME_BASED_WORKFLOW)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflow.getId().toString())
                    .log();
                schedulerService.scheduleWorkflow(workflow);
            }
        } else {
            // Unschedule if status is not ACTIVE or no cron schedule
            if (schedulerService.isScheduled(workflow.getId())) {
                logger.info()
                    .message(LOG_UNSCHEDULING_TIME_BASED_WORKFLOW)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflow.getId().toString())
                    .detail("status", workflow.getStatus().toString())
                    .detail("hasCron", workflow.getCronSchedule() != null && !workflow.getCronSchedule().isBlank())
                    .log();
                schedulerService.unscheduleWorkflow(workflow.getId());
            }
        }
    }

    /**
     * Build template metadata from source template ID
     */
    private Map<String, Object> buildTemplateMetadata(String sourceTemplateId) {
        if (sourceTemplateId == null || sourceTemplateId.isBlank()) {
            return null;
        }

        try {
            UUID templateId = parseUUID(sourceTemplateId);
            if (templateId == null) {
                return null;
            }

            // Fetch the original template
            WorkflowTemplate template = templateRepository.findById(templateId)
                .orElse(null);

            if (template == null) {
                logger.warn()
                    .message("Template not found for sourceTemplateId: " + sourceTemplateId)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("sourceTemplateId", sourceTemplateId)
                    .log();
                return null;
            }

            // Build metadata map
            return Map.of(
                "templateId", template.getId().toString(),
                "templateName", template.getName(),
                "templateVersion", template.getVersion() != null ? template.getVersion() : "1.0",
                "templateCategory", template.getCategory() != null ? template.getCategory().toString() : "",
                "usageCount", template.getUsageCount() != null ? template.getUsageCount() : 0,
                "isSystemTemplate", template.getIsSystemTemplate() != null ? template.getIsSystemTemplate() : false
            );
        } catch (Exception e) {
            logger.error()
                .message("Failed to build template metadata")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("sourceTemplateId", sourceTemplateId)
                .detail("error", e.getMessage())
                .log();
            return null;
        }
    }

    /**
     * Increment template usage count when a workflow is created from it
     */
    private void incrementTemplateUsageCount(String sourceTemplateId) {
        try {
            UUID templateId = parseUUID(sourceTemplateId);
            if (templateId == null) {
                return;
            }

            WorkflowTemplate template = templateRepository.findById(templateId).orElse(null);
            if (template != null) {
                Integer currentCount = template.getUsageCount() != null ? template.getUsageCount() : 0;
                template.setUsageCount(currentCount + 1);
                templateRepository.save(template);
                
                logger.info()
                    .message("Incremented usage count for template: " + template.getName())
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("templateId", templateId.toString())
                    .detail("newCount", currentCount + 1)
                    .log();
            }
        } catch (Exception e) {
            logger.error()
                .message("Failed to increment template usage count")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("sourceTemplateId", sourceTemplateId)
                .detail("error", e.getMessage())
                .log();
            // Don't fail workflow creation if usage count increment fails
        }
    }
}
