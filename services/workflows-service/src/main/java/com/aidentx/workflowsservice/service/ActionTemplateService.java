package com.aidentx.workflowsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages action templates loaded from configuration
 * Templates define pre-configured HTTP actions (activate role, create user, etc.)
 * without needing code changes for each new action type
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class ActionTemplateService {
    
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final Map<String, ActionTemplate> templates = new HashMap<>();
    
    public ActionTemplateService(ObjectMapper objectMapper, UnifiedLoggerFactory loggerFactory) {
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(ActionTemplateService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
        logger.info()
            .message(LOG_ACTION_TEMPLATE_SERVICE_CONSTRUCTOR)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        loadTemplates();
    }
    
    private void loadTemplates() {
        logger.info()
            .message(LOG_STARTING_ACTION_TEMPLATE_INIT)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        try {
            ClassPathResource resource = new ClassPathResource("action-templates.json");
            logger.debug()
                .message(LOG_LOOKING_FOR_ACTION_TEMPLATES_JSON)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .log();
            
            if (!resource.exists()) {
                logger.error()
                    .message(LOG_ACTION_TEMPLATES_JSON_NOT_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .log();
                return;
            }
            
            logger.info()
                .message(LOG_FOUND_ACTION_TEMPLATES_JSON)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .log();
            TemplateConfig config = objectMapper.readValue(resource.getInputStream(), TemplateConfig.class);
            
            if (config == null || config.getTemplates() == null) {
                logger.error()
                    .message(LOG_TEMPLATE_CONFIG_NULL_OR_NO_TEMPLATES)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .log();
                return;
            }
            
            logger.info()
                .message(LOG_PARSING_TEMPLATES_FROM_CONFIG)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("count", config.getTemplates().size())
                .log();
            
            for (ActionTemplate template : config.getTemplates()) {
                templates.put(template.getType(), template);
                logger.debug()
                    .message(LOG_LOADED_ACTION_TEMPLATE)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("type", template.getType())
                    .detail("name", template.getName())
                    .detail("category", template.getCategory())
                    .log();
            }
            
            logger.info()
                .message(LOG_SUCCESSFULLY_LOADED_ACTION_TEMPLATES)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("count", templates.size())
                .log();
            
        } catch (IOException e) {
            logger.error()
                .message(LOG_FAILED_LOAD_ACTION_TEMPLATES)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            // Don't fail startup - workflows can still use direct HTTP nodes
        } catch (Exception e) {
            logger.error()
                .message(LOG_UNEXPECTED_ERROR_LOADING_TEMPLATES)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("errorType", e.getClass().getName())
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Get template by type (e.g., "action.role.activate")
     */
    public ActionTemplate getTemplate(String type) {
        return templates.get(type);
    }
    
    /**
     * Get all available templates (for frontend)
     */
    public List<ActionTemplate> getAllTemplates() {
        return List.copyOf(templates.values());
    }
    
    /**
     * Check if a node type is a templated action
     */
    public boolean isTemplatedAction(String nodeType) {
        return templates.containsKey(nodeType);
    }
    
    /**
     * Apply template to node configuration
     * Merges template with node data (node overrides take precedence)
     */
    public Map<String, Object> applyTemplate(String nodeType, Map<String, Object> nodeData) {
        ActionTemplate template = templates.get(nodeType);
        if (template == null) {
            return nodeData;
        }
        
        Map<String, Object> result = new HashMap<>(nodeData);
        result.put("template", template.getTemplate());
        
        return result;
    }
    
    // ==================== Data Classes ====================
    
    @Data
    public static class TemplateConfig {
        private List<ActionTemplate> templates;
    }
    
    @Data
    public static class ActionTemplate {
        private String type;
        private String name;
        private String description;
        private String category;
        private String icon;
        private List<String> requiredContext;
        private List<String> optionalContext;
        private Map<String, Object> template;
    }
}
