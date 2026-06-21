package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.AuthMethod;
import com.aidentx.core.notifications_management.enums.EncryptionType;

/**
 * DTO for creating email configuration.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public class EmailConfigurationCreateDto {
    
    private String tenantId;
    private String smtpHostname;
    private Long smtpPort;
    private EncryptionType encryptionType;
    private String emailAddress;
    private AuthMethod authMethod;
    private String username;
    private String password;
    private String apiKey;
    
    /**
     * Default constructor.
     */
    public EmailConfigurationCreateDto() {}
    
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
     * Gets the SMTP hostname.
     * 
     * @return the SMTP hostname
     */
    public String getSmtpHostname() {
        return smtpHostname;
    }
    
    /**
     * Sets the SMTP hostname.
     * 
     * @param smtpHostname the SMTP hostname to set
     */
    public void setSmtpHostname(String smtpHostname) {
        this.smtpHostname = smtpHostname;
    }
    
    /**
     * Gets the SMTP port.
     * 
     * @return the SMTP port
     */
    public Long getSmtpPort() {
        return smtpPort;
    }
    
    /**
     * Sets the SMTP port.
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
     * Gets the email address.
     * 
     * @return the email address
     */
    public String getEmailAddress() {
        return emailAddress;
    }
    
    /**
     * Sets the email address.
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
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username.
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the password.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Gets the API key.
     * 
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * Sets the API key.
     * 
     * @param apiKey the API key to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
