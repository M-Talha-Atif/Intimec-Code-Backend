package com.aidentx.core.notifications_management.model.entity;

import com.aidentx.core.notifications_management.enums.AuthMethod;
import com.aidentx.core.notifications_management.enums.EncryptionType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing email configuration settings for sending notifications.
 * <p>
 * This entity stores the SMTP and authentication configuration required
 * to send email notifications through various email providers. Each configuration
 * is tenant-specific to support multi-tenancy.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Entity
@Table(name = "email_configurations", 
       uniqueConstraints = @UniqueConstraint(columnNames = "tenant_id", name = "uk_email_config_tenant"))
public class EmailConfiguration {
    
    /**
     * Unique identifier for the email configuration.
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
     * SMTP server hostname for email sending.
     */
    @Column(name = "smtp_hostname", nullable = false)
    private String smtpHostname;
    
    /**
     * SMTP server port number.
     */
    @Column(name = "smtp_port", nullable = false)
    private Long smtpPort;
    
    /**
     * Encryption type used for email communication.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "encryption_type", nullable = false)
    private EncryptionType encryptionType;
    
    /**
     * Email address used as the sender for notifications.
     */
    @Column(name = "email_address", nullable = false)
    private String emailAddress;
    
    /**
     * Authentication method used for SMTP authentication.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false)
    private AuthMethod authMethod;
    
    /**
     * Username for authentication (used with USERNAME_PASSWORD auth method).
     */
    @Column(name = "username")
    private String username;
    
    /**
     * Password for authentication (used with USERNAME_PASSWORD auth method).
     */
    @Column(name = "password")
    private String password;
    
    /**
     * API key for authentication (used with API_KEY auth method).
     */
    @Column(name = "api_key")
    private String apiKey;
    
    /**
     * Timestamp when the configuration was created (epoch time).
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    /**
     * Timestamp when the configuration was last updated (epoch time).
     */
    @Column(name = "updated_at")
    private Long updatedAt;
    
    /**
     * ID of the user who created this configuration (from JWT sub field).
     */
    @Column(name = "created_by_id", nullable = false)
    private String createdById;
    
    /**
     * Username of the user who created this configuration (from JWT username field).
     */
    @Column(name = "created_by_name", nullable = false)
    private String createdByName;
    
    /**
     * Default constructor for JPA.
     */
    public EmailConfiguration() {}
    
    /**
     * Creates a new email configuration with the specified parameters.
     * 
     * @param tenantId the tenant identifier
     * @param smtpHostname the SMTP server hostname
     * @param smtpPort the SMTP server port
     * @param encryptionType the encryption type
     * @param emailAddress the sender email address
     * @param authMethod the authentication method
     * @param createdById the ID of the user who created this configuration
     * @param createdByName the username of the user who created this configuration
     */
    public EmailConfiguration(String tenantId, String smtpHostname, Long smtpPort, 
                             EncryptionType encryptionType, String emailAddress, AuthMethod authMethod,
                             String createdById, String createdByName) {
        this.tenantId = tenantId;
        this.smtpHostname = smtpHostname;
        this.smtpPort = smtpPort;
        this.encryptionType = encryptionType;
        this.emailAddress = emailAddress;
        this.authMethod = authMethod;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Gets the unique identifier of the email configuration.
     * 
     * @return the configuration ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the email configuration.
     * 
     * @param id the configuration ID to set
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
     * Gets the SMTP server hostname.
     * 
     * @return the SMTP hostname
     */
    public String getSmtpHostname() {
        return smtpHostname;
    }
    
    /**
     * Sets the SMTP server hostname.
     * 
     * @param smtpHostname the SMTP hostname to set
     */
    public void setSmtpHostname(String smtpHostname) {
        this.smtpHostname = smtpHostname;
    }
    
    /**
     * Gets the SMTP server port number.
     * 
     * @return the SMTP port
     */
    public Long getSmtpPort() {
        return smtpPort;
    }
    
    /**
     * Sets the SMTP server port number.
     * 
     * @param smtpPort the SMTP port to set
     */
    public void setSmtpPort(Long smtpPort) {
        this.smtpPort = smtpPort;
    }
    
    /**
     * Gets the encryption type.
     * 
     * @return the encryption type
     */
    public EncryptionType getEncryptionType() {
        return encryptionType;
    }
    
    /**
     * Sets the encryption type.
     * 
     * @param encryptionType the encryption type to set
     */
    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }
    
    /**
     * Gets the sender email address.
     * 
     * @return the email address
     */
    public String getEmailAddress() {
        return emailAddress;
    }
    
    /**
     * Sets the sender email address.
     * 
     * @param emailAddress the email address to set
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    /**
     * Gets the authentication method.
     * 
     * @return the authentication method
     */
    public AuthMethod getAuthMethod() {
        return authMethod;
    }
    
    /**
     * Sets the authentication method.
     * 
     * @param authMethod the authentication method to set
     */
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }
    
    /**
     * Gets the username for authentication.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username for authentication.
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the password for authentication.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the password for authentication.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Gets the API key for authentication.
     * 
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * Sets the API key for authentication.
     * 
     * @param apiKey the API key to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * Gets the timestamp when the configuration was created.
     * 
     * @return the creation timestamp (epoch time)
     */
    public Long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the timestamp when the configuration was created.
     * 
     * @param createdAt the creation timestamp (epoch time) to set
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the timestamp when the configuration was last updated.
     * 
     * @return the update timestamp (epoch time)
     */
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the timestamp when the configuration was last updated.
     * 
     * @param updatedAt the update timestamp (epoch time) to set
     */
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the ID of the user who created this configuration.
     * 
     * @return the creator user ID
     */
    public String getCreatedById() {
        return createdById;
    }
    
    /**
     * Sets the ID of the user who created this configuration.
     * 
     * @param createdById the creator user ID to set
     */
    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
    
    /**
     * Gets the username of the user who created this configuration.
     * 
     * @return the creator username
     */
    public String getCreatedByName() {
        return createdByName;
    }
    
    /**
     * Sets the username of the user who created this configuration.
     * 
     * @param createdByName the creator username to set
     */
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}
