package com.aidentx.workflowsservice.controller;

import com.aidentx.workflowsservice.service.ActionTemplateService;
import com.aidentx.workflowsservice.service.ActionTemplateService.ActionTemplate;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for action templates
 * Provides available templated actions to frontend
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@RestController
@RequestMapping("/api/v1/action-templates")
public class ActionTemplateController {
    
    private final IUnifiedLogger logger;
    private final ActionTemplateService actionTemplateService;

    public ActionTemplateController(ActionTemplateService actionTemplateService, UnifiedLoggerFactory loggerFactory) {
        this.actionTemplateService = actionTemplateService;
        this.logger = loggerFactory.getLogger(ActionTemplateController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }
    
    /**
     * Get all available action templates
     * Used by frontend to populate node palette
     */
    @GetMapping
    public ResponseEntity<List<ActionTemplate>> getAllTemplates() {
        logger.trace()
            .message(LOG_FETCHING_ALL_ACTION_TEMPLATES)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .log();
        List<ActionTemplate> templates = actionTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get specific template by type
     */
    @GetMapping("/{type}")
    public ResponseEntity<ActionTemplate> getTemplate(@PathVariable String type) {
        logger.trace()
            .message(LOG_FETCHING_ACTION_TEMPLATE)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail("type", type)
            .log();
        
        // Replace dots with slashes in path parameter (action.role.activate becomes action/role/activate in URL)
        String normalizedType = type.replace("/", ".");
        
        ActionTemplate template = actionTemplateService.getTemplate(normalizedType);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(template);
    }
    
    /**
     * Get templates grouped by category
     */
    @GetMapping("/by-category")
    public ResponseEntity<Map<String, List<ActionTemplate>>> getTemplatesByCategory() {
        logger.trace()
            .message(LOG_FETCHING_ACTION_TEMPLATES_GROUPED)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .log();
        
        List<ActionTemplate> allTemplates = actionTemplateService.getAllTemplates();
        
        Map<String, List<ActionTemplate>> grouped = allTemplates.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory() : "other"
            ));
        
        return ResponseEntity.ok(grouped);
    }
}
