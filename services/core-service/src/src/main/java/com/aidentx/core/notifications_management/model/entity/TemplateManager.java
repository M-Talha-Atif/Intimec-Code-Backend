package com.aidentx.core.notifications_management.model.entity;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing email template configurations for different operations.
 * <p>
 * This entity manages email templates that are used when specific operations
 * are triggered. Each template is tenant-specific and supports internationalization
 * through different locale types. Templates can be enabled or disabled as needed.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Entity
@Table(name = "template_managers")
public class TemplateManager {
    
    /**
     * Unique identifier for the template manager.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * Tenant identifier for multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    /**
     * Human-readable name of the template.
     */
    @Column(name = "template_name", nullable = false)
    private String templateName;
    
    /**
     * Operation type that triggers this template.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private OperationType triggerType;
    
    /**
     * Flag indicating whether this template is currently enabled.
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
    
    /**
     * Language/locale for this template.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private LocaleType language;
    
    /**
     * Subject line for the email template.
     */
    @Column(name = "template_subject", nullable = false)
    private String templateSubject;
    
    /**
     * Email body content for the template.
     */
    @Column(name = "email_body", nullable = false, columnDefinition = "TEXT")
    private String emailBody;
    
    /**
     * Timestamp when the template was created (epoch time).
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    /**
     * Timestamp when the template was last updated (epoch time).
     */
    @Column(name = "updated_at")
    private Long updatedAt;
    
    /**
     * ID of the user who created this template (from JWT sub field).
     */
    @Column(name = "created_by_id", nullable = false)
    private String createdById;
    
    /**
     * Username of the user who created this template (from JWT username field).
     */
    @Column(name = "created_by_name", nullable = false)
    private String createdByName;
    
    /**
     * Current version number of this template.
     */
    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;
    
    /**
     * ID of the user who last updated this template (from JWT sub field).
     */
    @Column(name = "updated_by_id")
    private String updatedById;
    
    /**
     * Username of the user who last updated this template (from JWT username field).
     */
    @Column(name = "updated_by_name")
    private String updatedByName;
    
    /**
     * Default constructor for JPA.
     */
    public TemplateManager() {}
    
    /**
     * Creates a new template manager with the specified parameters.
     * The template is enabled by default.
     * 
     * @param tenantId the tenant identifier
     * @param templateName the name of the template
     * @param triggerType the operation type that triggers this template
     * @param language the language/locale for this template
     * @param templateSubject the email subject line
     * @param emailBody the email body content
     * @param createdById the ID of the user who created this template
     * @param createdByName the username of the user who created this template
     */
    public TemplateManager(String tenantId, String templateName, OperationType triggerType, 
                          LocaleType language, String templateSubject, String emailBody,
                          String createdById, String createdByName) {
        this.tenantId = tenantId;
        this.templateName = templateName;
        this.triggerType = triggerType;
        this.language = language;
        this.templateSubject = templateSubject;
        this.emailBody = emailBody;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.isEnabled = true;
        this.createdAt = System.currentTimeMillis();
    }
    
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
