package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;

/**
 * Data Transfer Object for creating template managers.
 * <p>
 * This DTO contains all the necessary information to create a new template manager
 * for email notifications. It includes validation and tenant-specific configurations.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public class TemplateManagerCreateDto {
    
    private String tenantId;
    private String templateName;
    private OperationType triggerType;
    private Boolean isEnabled;
    private LocaleType language;
    private String templateSubject;
    private String emailBody;
    
    /**
     * Default constructor.
     */
    public TemplateManagerCreateDto() {}
    
    /**
     * Constructor with all required fields.
     * 
     * @param tenantId the tenant identifier
     * @param templateName the name of the template
     * @param triggerType the operation type that triggers this template
     * @param language the language/locale for this template
     * @param templateSubject the email subject line
     * @param emailBody the email body content
     */
    public TemplateManagerCreateDto(String tenantId, String templateName, OperationType triggerType,
                                   LocaleType language, String templateSubject, String emailBody) {
        this.tenantId = tenantId;
        this.templateName = templateName;
        this.triggerType = triggerType;
        this.language = language;
        this.templateSubject = templateSubject;
        this.emailBody = emailBody;
        this.isEnabled = true; // Default to enabled
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
     * Gets the template name.
     * 
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }
    
    /**
     * Sets the template name.
     * 
     * @param templateName the template name to set
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    /**
     * Gets the operation type that triggers this template.
     * 
     * @return the trigger type
     */
    public OperationType getTriggerType() {
        return triggerType;
    }
    
    /**
     * Sets the operation type that triggers this template.
     * 
     * @param triggerType the trigger type to set
     */
    public void setTriggerType(OperationType triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     * Gets whether this template is currently enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    /**
     * Sets whether this template is currently enabled.
     * 
     * @param isEnabled true if enabled, false otherwise
     */
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
    /**
     * Gets the language/locale for this template.
     * 
     * @return the language
     */
    public LocaleType getLanguage() {
        return language;
    }
    
    /**
     * Sets the language/locale for this template.
     * 
     * @param language the language to set
     */
    public void setLanguage(LocaleType language) {
        this.language = language;
    }
    
    /**
     * Gets the email subject line.
     * 
     * @return the template subject
     */
    public String getTemplateSubject() {
        return templateSubject;
    }
    
    /**
     * Sets the email subject line.
     * 
     * @param templateSubject the template subject to set
     */
    public void setTemplateSubject(String templateSubject) {
        this.templateSubject = templateSubject;
    }
    
    /**
     * Gets the email body content.
     * 
     * @return the email body
     */
    public String getEmailBody() {
        return emailBody;
    }
    
    /**
     * Sets the email body content.
     * 
     * @param emailBody the email body to set
     */
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }
}
