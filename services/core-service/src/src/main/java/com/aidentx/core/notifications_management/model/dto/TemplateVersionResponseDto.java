package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;
import com.aidentx.core.notifications_management.model.entity.TemplateManagerVersion;

import java.util.UUID;

/**
 * Response DTO for template version information.
 * <p>
 * This DTO contains version-specific information about a template,
 * including the version number, change details, and historical data.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-09-18
 */
public class TemplateVersionResponseDto {
    
    /**
     * Unique identifier for this version record.
     */
    private UUID versionId;
    
    /**
     * Reference to the original template manager.
     */
    private UUID templateId;
    
    /**
     * Version number.
     */
    private Integer versionNumber;
    
    /**
     * Template name at this version.
     */
    private String templateName;
    
    /**
     * Operation type that triggered this template at this version.
     */
    private OperationType triggerType;
    
    /**
     * Enabled status at this version.
     */
    private Boolean isEnabled;
    
    /**
     * Language/locale for this template version.
     */
    private LocaleType language;
    
    /**
     * Subject line at this version.
     */
    private String templateSubject;
    
    /**
     * Email body content at this version.
     */
    private String emailBody;
    
    /**
     * Timestamp when this version was created (epoch time).
     */
    private Long createdAt;
    
    /**
     * ID of the user who created this version.
     */
    private String createdById;
    
    /**
     * Username of the user who created this version.
     */
    private String createdByName;
    
    /**
     * Comment describing the changes in this version.
     */
    private String changeComment;
    
    /**
     * Flag indicating if this is the current active version.
     */
    private Boolean isCurrent;
    
    /**
     * Type of change that created this version.
     */
    private TemplateManagerVersion.ChangeType changeType;
    
    /**
     * Default constructor.
     */
    public TemplateVersionResponseDto() {}
    
    /**
     * Creates a DTO from a TemplateManagerVersion entity.
     * 
     * @param version the template version entity
     */
    public TemplateVersionResponseDto(TemplateManagerVersion version) {
        this.versionId = version.getVersionId();
        this.templateId = version.getTemplateId();
        this.versionNumber = version.getVersionNumber();
        this.templateName = version.getTemplateName();
        this.triggerType = version.getTriggerType();
        this.isEnabled = version.getIsEnabled();
        this.language = version.getLanguage();
        this.templateSubject = version.getTemplateSubject();
        this.emailBody = version.getEmailBody();
        this.createdAt = version.getCreatedAt();
        this.createdById = version.getCreatedById();
        this.createdByName = version.getCreatedByName();
        this.changeComment = version.getChangeComment();
        this.isCurrent = version.getIsCurrent();
        this.changeType = version.getChangeType();
    }
    
    // Getters and Setters
    
    public UUID getVersionId() {
        return versionId;
    }
    
    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }
    
    public UUID getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }
    
    public Integer getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public OperationType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(OperationType triggerType) {
        this.triggerType = triggerType;
    }
    
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
    public LocaleType getLanguage() {
        return language;
    }
    
    public void setLanguage(LocaleType language) {
        this.language = language;
    }
    
    public String getTemplateSubject() {
        return templateSubject;
    }
    
    public void setTemplateSubject(String templateSubject) {
        this.templateSubject = templateSubject;
    }
    
    public String getEmailBody() {
        return emailBody;
    }
    
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedById() {
        return createdById;
    }
    
    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public String getChangeComment() {
        return changeComment;
    }
    
    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }
    
    public Boolean getIsCurrent() {
        return isCurrent;
    }
    
    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
    
    public TemplateManagerVersion.ChangeType getChangeType() {
        return changeType;
    }
    
    public void setChangeType(TemplateManagerVersion.ChangeType changeType) {
        this.changeType = changeType;
    }
}