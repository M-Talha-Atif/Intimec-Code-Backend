package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing an active user session.
 * 
 * <p>A session is created when a user logs in through any authentication method
 * (normal login, LDAP, OAuth2, SAML) and is deleted when the user logs out.</p>
 * 
 * <p>Each session is associated with a refresh token for token renewal.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "userId"),
    @Index(name = "idx_user_sessions_refresh_token", columnList = "refreshToken"),
    @Index(name = "idx_user_sessions_created_at", columnList = "createdAt")
})
public class UserSession {
    
    /**
     * Unique session identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private UUID sessionId;
    
    /**
     * The user who owns this session.
     */
    @Column(nullable = false)
    private UUID userId;
    
    /**
     * Refresh token associated with this session.
     * Used to generate new access tokens without re-authentication.
     */
    @Column(nullable = false, unique = true, length = 512)
    private String refreshToken;
    
    /**
     * IP address from which the user logged in.
     */
    @Column(length = 45) // IPv6 max length
    private String ipAddress;
    
    /**
     * User agent string containing browser or mobile device information.
     */
    @Column(length = 512)
    private String userAgent;
    
    /**
     * Parsed device and browser information.
     * Examples: "Chrome 120 on Windows 11", "Safari on iPhone 15"
     */
    @Column(length = 255)
    private String device;
    
    /**
     * Flag indicating if the session is from a mobile device.
     */
    @Column
    private Boolean isMobile;
    
    /**
     * Flag indicating if the session is from a desktop device.
     */
    @Column
    private Boolean isDesktop;
    
    /**
     * Timestamp when the session was created (in epoch milliseconds).
     */
    @Column(nullable = false)
    private long createdAt;
    
    /**
     * Timestamp of the last activity/request made by this session (in epoch milliseconds).
     * Updated automatically by the API Gateway on every authenticated request.
     */
    @Column(nullable = false)
    private long lastActivityAt;
    
    /**
     * Authentication method used to create this session.
     * Examples: PASSWORD, LDAP, OAUTH2, SAML
     */
    @Column(length = 50)
    private String authMethod;
    
    /**
     * Optional tenant ID for multi-tenant applications.
     */
    @Column(length = 100)
    private String tenantId;
    
    /**
     * Pre-persist callback to set creation timestamp.
     */
    public UserSession() {
    }
    
    /**
     * Pre-persist callback to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == 0) {
            createdAt = System.currentTimeMillis();
        }
        if (lastActivityAt == 0) {
            lastActivityAt = System.currentTimeMillis();
        }
    }

    // Getters and Setters
    
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Boolean getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(Boolean isMobile) {
        this.isMobile = isMobile;
    }

    public Boolean getIsDesktop() {
        return isDesktop;
    }

    public void setIsDesktop(Boolean isDesktop) {
        this.isDesktop = isDesktop;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(long lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
