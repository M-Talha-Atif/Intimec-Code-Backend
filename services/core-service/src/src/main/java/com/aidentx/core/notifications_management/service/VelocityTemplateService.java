package com.aidentx.core.notifications_management.service;

import java.util.Map;

/**
 * Service interface for processing Velocity templates.
 * <p>
 * This service provides template processing capabilities using Apache Velocity
 * to replace variables in email templates with actual values.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public interface VelocityTemplateService {
    
    /**
     * Processes a template string by replacing Velocity variables with provided values.
     * 
     * @param template the template string containing Velocity variables (e.g., ${username})
     * @param variables a map of variable names to their values
     * @return the processed template with variables replaced
     * @throws RuntimeException if template processing fails
     */
    String processTemplate(String template, Map<String, Object> variables);
    
    /**
     * Processes both subject and body templates with the same variables.
     * 
     * @param subject the subject template string
     * @param body the body template string
     * @param variables a map of variable names to their values
     * @return an array where [0] is processed subject and [1] is processed body
     * @throws RuntimeException if template processing fails
     */
    String[] processEmailTemplate(String subject, String body, Map<String, Object> variables);
}
