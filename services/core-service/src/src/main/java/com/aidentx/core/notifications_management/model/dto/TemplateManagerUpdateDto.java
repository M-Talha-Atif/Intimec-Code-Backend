package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;

/**
 * Data Transfer Object for updating template managers.
 * <p>
 * This DTO contains optional fields for updating existing template managers.
 * Only non-null fields will be updated, allowing for partial updates.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public class TemplateManagerUpdateDto {
    
    private String templateName;
    private OperationType triggerType;
    private Boolean isEnabled;
    private LocaleType language;
    private String templateSubject;
    private String emailBody;
    private String changeComment;
    
    /**
     * Default constructor.
     */
    public TemplateManagerUpdateDto() {}
    
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
     * Gets the change comment describing the reason for this update.
     * 
     * @return the change comment
     */
    public String getChangeComment() {
        return changeComment;
    }
    
    /**
     * Sets the change comment describing the reason for this update.
     * 
     * @param changeComment the change comment to set
     */
    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }
}
