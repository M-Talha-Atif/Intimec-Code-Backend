package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;

import java.util.UUID;

/**
 * Data Transfer Object for template manager responses.
 * <p>
 * This DTO is used to return template manager information to clients.
 * It contains all the public information about a template manager.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public class TemplateManagerResponseDto {
    
    private UUID id;
    private String tenantId;
    private String templateName;
    private OperationType triggerType;
    private Boolean isEnabled;
    private LocaleType language;
    private String templateSubject;
    private String emailBody;
    private Long createdAt;
    private Long updatedAt;
    private String createdById;
    private String createdByName;
    private Integer currentVersion;
    private String updatedById;
    private String updatedByName;
    
    /**
     * Default constructor.
     */
    public TemplateManagerResponseDto() {}
    
    /**
     * Gets the unique identifier of the template manager.
     * 
     * @return the template ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the template manager.
     * 
     * @param id the template ID to set
     */
    public void setId(UUID id) {
        this.id = id;
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
    
    /**
     * Gets the timestamp when the template was created.
     * 
     * @return the creation timestamp (epoch time)
     */
    public Long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the timestamp when the template was created.
     * 
     * @param createdAt the creation timestamp (epoch time) to set
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the timestamp when the template was last updated.
     * 
     * @return the update timestamp (epoch time)
     */
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the timestamp when the template was last updated.
     * 
     * @param updatedAt the update timestamp (epoch time) to set
     */
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the ID of the user who created this template.
     * 
     * @return the creator user ID
     */
    public String getCreatedById() {
        return createdById;
    }
    
    /**
     * Sets the ID of the user who created this template.
     * 
     * @param createdById the creator user ID to set
     */
    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
    
    /**
     * Gets the username of the user who created this template.
     * 
     * @return the creator username
     */
    public String getCreatedByName() {
        return createdByName;
    }
    
    /**
     * Sets the username of the user who created this template.
     * 
     * @param createdByName the creator username to set
     */
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    /**
     * Gets the current version number of this template.
     * 
     * @return the current version number
     */
    public Integer getCurrentVersion() {
        return currentVersion;
    }
    
    /**
     * Sets the current version number of this template.
     * 
     * @param currentVersion the current version number to set
     */
    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }
    
    /**
     * Gets the ID of the user who last updated this template.
     * 
     * @return the updater user ID
     */
    public String getUpdatedById() {
        return updatedById;
    }
    
    /**
     * Sets the ID of the user who last updated this template.
     * 
     * @param updatedById the updater user ID to set
     */
    public void setUpdatedById(String updatedById) {
        this.updatedById = updatedById;
    }
    
    /**
     * Gets the username of the user who last updated this template.
     * 
     * @return the updater username
     */
    public String getUpdatedByName() {
        return updatedByName;
    }
    
    /**
     * Sets the username of the user who last updated this template.
     * 
     * @param updatedByName the updater username to set
     */
    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}
