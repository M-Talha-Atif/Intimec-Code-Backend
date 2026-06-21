package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.service.VelocityTemplateService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of VelocityTemplateService for processing email templates.
 * <p>
 * This service uses Apache Velocity to process template strings by replacing
 * variables with actual values. It provides email-specific template processing
 * with proper error handling and logging.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
@Service
public class VelocityTemplateServiceImpl implements VelocityTemplateService {
    
    private final IUnifiedLogger logger;
    
    /**
     * Constructor for VelocityTemplateServiceImpl.
     * 
     * @param loggerFactory Factory for creating unified loggers
     */
    public VelocityTemplateServiceImpl(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("VelocityTemplateService", "velocity_template_service");
    }
    
    /**
     * Initializes the Velocity engine with default configuration.
     */
    @PostConstruct
    public void initVelocity() {
        try {
            // Initialize Velocity engine
            Velocity.init();
            logger.info().message("Velocity engine initialized successfully").log();
        } catch (Exception e) {
            logger.error(e).message("Failed to initialize Velocity engine: " + e.getMessage()).log();
            throw new RuntimeException("Failed to initialize Velocity engine", e);
        }
    }
    
    /**
     * Processes a template string by replacing Velocity variables with provided values.
     * 
     * @param template the template string containing Velocity variables
     * @param variables a map of variable names to their values
     * @return the processed template with variables replaced
     */
    @Override
    public String processTemplate(String template, Map<String, Object> variables) {
        logger.trace().message("Processing template with variables")
                .detail("variableCount", variables.size())
                .log();
        
        try {
            // Create Velocity context and add variables
            VelocityContext context = new VelocityContext();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
                logger.debug().message("Added variable to context")
                        .detail("variable", entry.getKey())
                        .detail("value", entry.getValue())
                        .log();
            }
            
            // Process template
            StringWriter writer = new StringWriter();
            Velocity.evaluate(context, writer, "EmailTemplate", template);
            
            String result = writer.toString();
            logger.debug().message("Template processed successfully")
                    .detail("originalLength", template.length())
                    .detail("processedLength", result.length())
                    .log();
            
            return result;
            
        } catch (Exception e) {
            logger.error(e).message("Failed to process template: " + e.getMessage())
                    .detail("template", template)
                    .log();
            throw new RuntimeException("Failed to process template", e);
        }
    }
    
    /**
     * Processes both subject and body templates with the same variables.
     * 
     * @param subject the subject template string
     * @param body the body template string
     * @param variables a map of variable names to their values
     * @return an array where [0] is processed subject and [1] is processed body
     */
    @Override
    public String[] processEmailTemplate(String subject, String body, Map<String, Object> variables) {
        // Handle null variables by using empty map
        if (variables == null) {
            variables = new java.util.HashMap<>();
        }
        
        logger.trace().message("Processing email template (subject and body)")
                .detail("variableCount", variables.size())
                .log();
        
        try {
            String processedSubject = processTemplate(subject, variables);
            String processedBody = processTemplate(body, variables);
            
            logger.info().message("Email template processed successfully")
                    .detail("subjectLength", processedSubject.length())
                    .detail("bodyLength", processedBody.length())
                    .log();
            
            return new String[]{processedSubject, processedBody};
            
        } catch (Exception e) {
            logger.error(e).message("Failed to process email template: " + e.getMessage()).log();
            throw e;
        }
    }
}
