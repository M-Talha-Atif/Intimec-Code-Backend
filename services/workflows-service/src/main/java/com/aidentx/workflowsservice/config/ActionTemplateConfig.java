package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.service.ActionTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Explicit configuration to ensure ActionTemplateService is created
 */
@Configuration
public class ActionTemplateConfig {
    
    private final IUnifiedLogger logger;
    
    public ActionTemplateConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(ActionTemplateConfig.class, LOG_CATEGORY_WORKFLOW_CONFIG);
        logger.info().message(LOG_ACTION_TEMPLATE_CONFIG_CONSTRUCTOR).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
    }
    
    @Bean
    public ActionTemplateService actionTemplateService(ObjectMapper objectMapper, UnifiedLoggerFactory loggerFactory) {
        logger.info().message(LOG_CREATING_ACTION_TEMPLATE_SERVICE_BEAN).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
        ActionTemplateService service = new ActionTemplateService(objectMapper, loggerFactory);
        logger.info().message(LOG_ACTION_TEMPLATE_SERVICE_CREATED).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
        return service;
    }
}
