package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import com.aidentx.workflowsservice.repository.WorkflowTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Initialize system templates on application startup
 * System templates are loaded from system-workflow-templates.json
 * These templates are extracted from production workflows
 */
@Component
@RequiredArgsConstructor
public class SystemTemplateInitializer {

    private final WorkflowTemplateRepository templateRepository;
    private final UnifiedLoggerFactory loggerFactory;
    private final ObjectMapper objectMapper;
    private IUnifiedLogger logger;

    @PostConstruct
    public void initializeSystemTemplates() {
        logger = loggerFactory.getLogger(SystemTemplateInitializer.class, LOG_CATEGORY_WORKFLOW_CONFIG);
        logger.info().message(LOG_INITIALIZING_SYSTEM_TEMPLATES).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();

        loadSystemTemplatesFromJson();

        logger.info().message(LOG_SYSTEM_TEMPLATES_INITIALIZED).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
    }

    private void loadSystemTemplatesFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("system-workflow-templates.json");
            
            if (!resource.exists()) {
                logger.error()
                    .message("System workflow templates JSON not found")
                    .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                    .log();
                return;
            }

            TemplateConfig config = objectMapper.readValue(resource.getInputStream(), TemplateConfig.class);
            
            if (config == null || config.getTemplates() == null) {
                logger.error()
                    .message("Template config is null or has no templates")
                    .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                    .log();
                return;
            }

            logger.info()
                .message("Loading system templates from JSON")
                .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                .detail("count", config.getTemplates().size())
                .log();

            for (TemplateDefinition templateDef : config.getTemplates()) {
                createTemplateFromDefinition(templateDef);
            }

        } catch (IOException e) {
            logger.error()
                .message("Failed to load system workflow templates")
                .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                .detail("error", e.getMessage())
                .log();
        }
    }

    private void createTemplateFromDefinition(TemplateDefinition templateDef) {
        try {
            // Check if template with this name already exists
            List<WorkflowTemplate> existingTemplates = templateRepository.findByIsSystemTemplate(true);
            boolean exists = existingTemplates.stream()
                .anyMatch(t -> t.getName().equals(templateDef.getName()));
            
            if (exists) {
                logger.info()
                    .message("System template already exists, skipping")
                    .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                    .detail("name", templateDef.getName())
                    .log();
                return;
            }

            WorkflowTemplate template = WorkflowTemplate.builder()
                    .name(templateDef.getName())
                    .description(templateDef.getDescription())
                    .category(templateDef.getCategory())
                    .framework(templateDef.getFramework())
                    .complexity(WorkflowTemplate.Complexity.valueOf(templateDef.getComplexity()))
                    .steps(templateDef.getSteps())
                    .estimatedTime(templateDef.getEstimatedTime())
                    .complianceFrameworks(templateDef.getComplianceFrameworks())
                    .prerequisites(templateDef.getPrerequisites())
                    .tags(templateDef.getTags())
                    .isSystemTemplate(true)
                    .version(templateDef.getVersion())
                    .usageCount(0)
                    .rating(new BigDecimal("4.5"))
                    .downloads(0)
                    .variables(new HashMap<>(templateDef.getVariables()))
                    .nodes(templateDef.getNodes())
                    .edges(templateDef.getEdges())
                    .build();

            templateRepository.save(template);
            
            logger.info()
                .message("Created system template")
                .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                .detail("name", templateDef.getName())
                .detail("version", templateDef.getVersion())
                .log();
                
        } catch (Exception e) {
            logger.error()
                .message("Failed to create system template")
                .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                .detail("name", templateDef.getName())
                .detail("error", e.getMessage())
                .log();
        }
    }

    @Data
    private static class TemplateConfig {
        private List<TemplateDefinition> templates;
    }

    @Data
    private static class TemplateDefinition {
        private String name;
        private String description;
        private String category;
        private String framework;
        private String complexity;
        private Integer steps;
        private String estimatedTime;
        private List<String> complianceFrameworks;
        private List<String> prerequisites;
        private List<String> tags;
        private String version;
        private Map<String, String> variables;
        private List<Map<String, Object>> nodes;
        private List<Map<String, Object>> edges;
    }
}
