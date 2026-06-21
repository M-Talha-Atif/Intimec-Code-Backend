package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;

import java.util.Map;

/**
 * Data Transfer Object for testing email template processing and sending.
 * <p>
 * This DTO contains all the necessary information to test the complete
 * email workflow including template retrieval, variable processing,
 * and email sending using configured SMTP settings.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public class EmailTemplateTestDto {
    
    private String tenantId;
    private OperationType triggerType;
    private LocaleType language;
    private String testEmailAddress;
    private Map<String, Object> templateVariables;
    
    /**
     * Default constructor.
     */
    public EmailTemplateTestDto() {}
    
    /**
     * Constructor with all required fields.
     * 
     * @param tenantId the tenant identifier
     * @param triggerType the operation type that triggers the template
     * @param language the language/locale for the template
     * @param testEmailAddress the email address to send the test email to
     * @param templateVariables the variables to replace in the template
     */
    public EmailTemplateTestDto(String tenantId, OperationType triggerType, LocaleType language,
                               String testEmailAddress, Map<String, Object> templateVariables) {
        this.tenantId = tenantId;
        this.triggerType = triggerType;
        this.language = language;
        this.testEmailAddress = testEmailAddress;
        this.templateVariables = templateVariables;
    }
    
    /**
     * Gets the tenant identifier.
     * 
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }
    
    /**
     * Sets the tenant identifier.
     * 
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    /**
     * Gets the operation type that triggers the template.
     * 
     * @return the trigger type
     */
    public OperationType getTriggerType() {
        return triggerType;
    }
    
    /**
     * Sets the operation type that triggers the template.
     * 
     * @param triggerType the trigger type to set
     */
    public void setTriggerType(OperationType triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     * Gets the language/locale for the template.
     * 
     * @return the language
     */
    public LocaleType getLanguage() {
        return language;
    }
    
    /**
     * Sets the language/locale for the template.
     * 
     * @param language the language to set
     */
    public void setLanguage(LocaleType language) {
        this.language = language;
    }
    
    /**
     * Gets the test email address.
     * 
     * @return the test email address
     */
    public String getTestEmailAddress() {
        return testEmailAddress;
    }
    
    /**
     * Sets the test email address.
     * 
     * @param testEmailAddress the test email address to set
     */
    public void setTestEmailAddress(String testEmailAddress) {
        this.testEmailAddress = testEmailAddress;
    }
    
    /**
     * Gets the template variables.
     * 
     * @return the template variables map
     */
    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }
    
    /**
     * Sets the template variables.
     * 
     * @param templateVariables the template variables to set
     */
    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }
}
