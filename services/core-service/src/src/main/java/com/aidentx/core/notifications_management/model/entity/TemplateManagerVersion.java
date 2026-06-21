package com.aidentx.core.notifications_management.model.entity;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing historical versions of email template configurations.
 * <p>
 * This entity stores all historical versions of templates, allowing users
 * to track changes over time and revert to previous versions when needed.
 * Each version maintains a complete snapshot of the template at a specific point in time.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-09-18
 */
@Entity
@Table(name = "template_manager_versions", 
       indexes = {
           @Index(name = "idx_template_id_version", columnList = "template_id, version_number"),
           @Index(name = "idx_template_tenant", columnList = "template_id, tenant_id"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_current_version", columnList = "template_id, is_current")
       })
public class TemplateManagerVersion {
    
    /**
     * Unique identifier for this version record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "version_id", columnDefinition = "UUID")
    private UUID versionId;
    
    /**
     * Reference to the original template manager.
     */
    @Column(name = "template_id", nullable = false, columnDefinition = "UUID")
    private UUID templateId;
    
    /**
     * Version number (incremental).
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    /**
     * Tenant identifier for multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    /**
     * Template name at this version.
     */
    @Column(name = "template_name", nullable = false)
    private String templateName;
    
    /**
     * Operation type that triggered this template at this version.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private OperationType triggerType;
    
    /**
     * Enabled status at this version.
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;
    
    /**
     * Language/locale for this template version.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private LocaleType language;
    
    /**
     * Subject line at this version.
     */
    @Column(name = "template_subject", nullable = false)
    private String templateSubject;
    
    /**
     * Email body content at this version.
     */
    @Column(name = "email_body", nullable = false, columnDefinition = "TEXT")
    private String emailBody;
    
    /**
     * Timestamp when this version was created (epoch time).
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    /**
     * ID of the user who created this version (from JWT sub field).
     */
    @Column(name = "created_by_id", nullable = false)
    private String createdById;
    
    /**
     * Username of the user who created this version (from JWT username field).
     */
    @Column(name = "created_by_name", nullable = false)
    private String createdByName;
    
    /**
     * Optional comment describing the changes in this version.
     */
    @Column(name = "change_comment", columnDefinition = "TEXT")
    private String changeComment;
    
    /**
     * Flag indicating if this is the current active version.
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;
    
    /**
     * Type of change that created this version.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;
    
    /**
     * Default constructor for JPA.
     */
    public TemplateManagerVersion() {}
    
    /**
     * Creates a new template version from an existing TemplateManager.
     * 
     * @param template the template to create a version from
     * @param versionNumber the version number
     * @param createdById the user ID creating this version
     * @param createdByName the username creating this version
     * @param changeComment optional comment about the changes
     * @param changeType the type of change
     */
    public TemplateManagerVersion(TemplateManager template, Integer versionNumber, 
                                 String createdById, String createdByName, 
                                 String changeComment, ChangeType changeType) {
        this.templateId = template.getId();
        this.versionNumber = versionNumber;
        this.tenantId = template.getTenantId();
        this.templateName = template.getTemplateName();
        this.triggerType = template.getTriggerType();
        this.isEnabled = template.getIsEnabled();
        this.language = template.getLanguage();
        this.templateSubject = template.getTemplateSubject();
        this.emailBody = template.getEmailBody();
        this.createdAt = System.currentTimeMillis();
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.changeComment = changeComment;
        this.changeType = changeType;
        this.isCurrent = true;
    }
    
    /**
     * Converts this version back to a TemplateManager entity.
     * 
     * @return a new TemplateManager instance with this version's data
     */
    public TemplateManager toTemplateManager() {
        TemplateManager template = new TemplateManager();
        template.setId(this.templateId);
        template.setTenantId(this.tenantId);
        template.setTemplateName(this.templateName);
        template.setTriggerType(this.triggerType);
        template.setIsEnabled(this.isEnabled);
        template.setLanguage(this.language);
        template.setTemplateSubject(this.templateSubject);
        template.setEmailBody(this.emailBody);
        template.setCreatedById(this.createdById);
        template.setCreatedByName(this.createdByName);
        template.setCurrentVersion(this.versionNumber);
        template.setUpdatedAt(this.createdAt);
        template.setUpdatedById(this.createdById);
        template.setUpdatedByName(this.createdByName);
        return template;
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
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
    
    public ChangeType getChangeType() {
        return changeType;
    }
    
    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
    
    /**
     * Enum for tracking types of changes made to templates.
     */
    public enum ChangeType {
        /**
         * Template was initially created.
         */
        CREATED,
        
        /**
         * Template content was updated.
         */
        UPDATED,
        
        /**
         * Template was enabled.
         */
        ENABLED,
        
        /**
         * Template was disabled.
         */
        DISABLED,
        
        /**
         * Template was restored from a previous version.
         */
        ROLLBACK
    }
}