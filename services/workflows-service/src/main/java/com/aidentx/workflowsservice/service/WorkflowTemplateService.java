package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.CreateWorkflowRequest;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.repository.WorkflowTemplateRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Service for managing workflow templates
 */
@Service
public class WorkflowTemplateService {

    private final IUnifiedLogger logger;
    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowTemplateService(
            WorkflowTemplateRepository templateRepository,
            WorkflowRepository workflowRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.templateRepository = templateRepository;
        this.workflowRepository = workflowRepository;
        this.logger = loggerFactory.getLogger(WorkflowTemplateService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Create a new workflow template
     */
    @Transactional
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        logger.info()
            .message(LOG_CREATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("name", template.getName())
            .log();

        // Validate name uniqueness
        templateRepository.findByName(template.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException(String.format(ERROR_TEMPLATE_NAME_EXISTS, template.getName()));
        });

        return templateRepository.save(template);
    }

    /**
     * Update an existing template
     */
    @Transactional
    public WorkflowTemplate updateTemplate(UUID id, WorkflowTemplate updatedTemplate) {
        logger.info()
            .message(LOG_UPDATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("id", id.toString())
            .log();

        WorkflowTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TEMPLATE_NOT_FOUND + id));

        // Prevent editing of system templates
        if (Boolean.TRUE.equals(existing.getIsSystemTemplate())) {
            throw new IllegalArgumentException(ERROR_CANNOT_EDIT_SYSTEM_TEMPLATE);
        }

        // Update fields
        existing.setName(updatedTemplate.getName());
        existing.setDescription(updatedTemplate.getDescription());
        existing.setCategory(updatedTemplate.getCategory());
        existing.setFramework(updatedTemplate.getFramework());
        existing.setComplexity(updatedTemplate.getComplexity());
        existing.setNodes(updatedTemplate.getNodes());
        existing.setEdges(updatedTemplate.getEdges());
        existing.setVariables(updatedTemplate.getVariables());
        existing.setTags(updatedTemplate.getTags());
        existing.setSteps(updatedTemplate.getSteps());
        existing.setEstimatedTime(updatedTemplate.getEstimatedTime());
        existing.setComplianceFrameworks(updatedTemplate.getComplianceFrameworks());
        existing.setPrerequisites(updatedTemplate.getPrerequisites());

        return templateRepository.save(existing);
    }

    /**
     * Get template by ID
     */
    public WorkflowTemplate getTemplate(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TEMPLATE_NOT_FOUND + id));
    }

    /**
     * List all templates
     */
    public List<WorkflowTemplate> listTemplates() {
        return templateRepository.findAll();
    }

    /**
     * List templates by category
     */
    public List<WorkflowTemplate> listByCategory(String category) {
        return templateRepository.findByCategory(category);
    }

    /**
     * List templates by framework
     */
    public List<WorkflowTemplate> listByFramework(String framework) {
        return templateRepository.findByFramework(framework);
    }

    /**
     * List templates by complexity
     */
    public List<WorkflowTemplate> listByComplexity(WorkflowTemplate.Complexity complexity) {
        return templateRepository.findByComplexity(complexity);
    }

    /**
     * List system templates
     */
    public List<WorkflowTemplate> listSystemTemplates() {
        return templateRepository.findByIsSystemTemplate(true);
    }

    /**
     * Get popular templates
     */
    public List<WorkflowTemplate> getPopularTemplates(Integer minUsage) {
        return templateRepository.findPopularTemplates(minUsage != null ? minUsage : 5);
    }

    /**
     * Get top rated templates
     */
    public List<WorkflowTemplate> getTopRatedTemplates() {
        return templateRepository.findTopRatedTemplates();
    }

    /**
     * Delete a template
     */
    @Transactional
    public void deleteTemplate(UUID id) {
        logger.info()
            .message(LOG_DELETING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowTemplate")
            .objectId(id.toString())
            .log();

        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TEMPLATE_NOT_FOUND + id));

        // Prevent deletion of system templates
        if (Boolean.TRUE.equals(template.getIsSystemTemplate())) {
            throw new IllegalArgumentException(ERROR_CANNOT_DELETE_SYSTEM_TEMPLATE);
        }

        templateRepository.delete(template);
    }

    /**
     * Instantiate a template as a new workflow
     * Replaces template variables with provided values
     */
    @Transactional
    public Workflow instantiateTemplate(UUID templateId, String workflowName, Map<String, Object> variableValues) {
        logger.info()
            .message(LOG_INSTANTIATING_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowTemplate")
            .objectId(templateId.toString())
            .detail("workflowName", workflowName)
            .log();

        WorkflowTemplate template = getTemplate(templateId);

        // Ensure unique workflow name by appending timestamp
        String uniqueName = ensureUniqueWorkflowName(workflowName);

        // Create workflow from template
        Workflow workflow = new Workflow();
        workflow.setName(uniqueName);
        workflow.setDescription(TEMPLATE_DESCRIPTION_PREFIX + template.getName());
        workflow.setNodes(replaceVariables(template.getNodes(), template.getVariables(), variableValues));
        workflow.setEdges(template.getEdges());
        workflow.setVersion(DEFAULT_VERSION);

        // Link to source template
        workflow.setSourceTemplateId(templateId);
        workflow.setIsConnectedToTemplate(true);
        workflow.setUpstreamVersion(template.getVersion());

        // Store template metadata
        Map<String, Object> templateMetadata = new java.util.HashMap<>();
        templateMetadata.put(TEMPLATE_META_TEMPLATE_ID, templateId.toString());
        templateMetadata.put(TEMPLATE_META_TEMPLATE_NAME, template.getName());
        templateMetadata.put(TEMPLATE_META_TEMPLATE_VERSION, template.getVersion());
        templateMetadata.put(TEMPLATE_META_TEMPLATE_CATEGORY, template.getCategory());
        templateMetadata.put(TEMPLATE_META_INSTANTIATED_AT, java.time.OffsetDateTime.now().toString());
        templateMetadata.put(TEMPLATE_META_VARIABLES_USED, variableValues);
        workflow.setTemplateMetadata(templateMetadata);

        Workflow savedWorkflow = workflowRepository.save(workflow);

        // Increment template usage count
        template.setUsageCount(template.getUsageCount() != null ? template.getUsageCount() + 1 : 1);
        template.setDownloads(template.getDownloads() != null ? template.getDownloads() + 1 : 1);
        templateRepository.save(template);

        logger.info()
            .message(LOG_TEMPLATE_INSTANTIATED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("Workflow")
            .objectId(savedWorkflow.getId().toString())
            .log();
        return savedWorkflow;
    }

    /**
     * Replace template variables in nodes with actual values
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> replaceVariables(
            List<Map<String, Object>> nodes,
            Map<String, Object> templateVariables,
            Map<String, Object> variableValues) {

        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }

        // For each node, recursively replace variable placeholders
        return nodes.stream()
                .map(node -> replaceVariablesInMap(node, variableValues))
                .toList();
    }

    /**
     * Recursively replace variables in a map structure
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> replaceVariablesInMap(Map<String, Object> map, Map<String, Object> variableValues) {
        if (map == null) return null;

        return map.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Object value = entry.getValue();

                            if (value instanceof String) {
                                // Replace {{variable}} placeholders
                                String stringValue = (String) value;
                                for (Map.Entry<String, Object> var : variableValues.entrySet()) {
                                    String placeholder = TEMPLATE_VARIABLE_PLACEHOLDER_PREFIX + var.getKey() + TEMPLATE_VARIABLE_PLACEHOLDER_SUFFIX;
                                    if (stringValue.contains(placeholder)) {
                                        stringValue = stringValue.replace(placeholder, String.valueOf(var.getValue()));
                                    }
                                }
                                return stringValue;
                            } else if (value instanceof Map) {
                                return replaceVariablesInMap((Map<String, Object>) value, variableValues);
                            } else if (value instanceof List) {
                                return ((List<?>) value).stream()
                                        .map(item -> {
                                            if (item instanceof Map) {
                                                return replaceVariablesInMap((Map<String, Object>) item, variableValues);
                                            }
                                            return item;
                                        })
                                        .toList();
                            }

                            return value;
                        }
                ));
    }

    /**
     * Ensure workflow name is unique by appending timestamp if needed
     */
    private String ensureUniqueWorkflowName(String baseName) {
        String name = baseName;
        String version = DEFAULT_VERSION;

        // Check if name+version already exists
        if (workflowRepository.findByNameAndVersion(name, version).isPresent()) {
            // Append timestamp to make it unique
            name = baseName + TEMPLATE_NAME_SEPARATOR + System.currentTimeMillis();
            logger.info()
                .message(LOG_WORKFLOW_NAME_EXISTS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("baseName", baseName)
                .detail("uniqueName", name)
                .log();
        }

        return name;
    }
}