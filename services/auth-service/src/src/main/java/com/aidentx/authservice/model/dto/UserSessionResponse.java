package com.aidentx.authservice.model.dto;

import java.util.UUID;

/**
 * DTO for returning user session information with user details.
 * Used in API responses to provide session data along with associated user information.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class UserSessionResponse {
    
    private UUID sessionId;
    private UserInfo user;
    private String refreshToken;
    private String ipAddress;
    private String location;
    private String userAgent;
    private String device;
    private Boolean isMobile;
    private Boolean isDesktop;
    private long createdAt;
    private long lastActivityAt;
    private String authMethod;
    private String tenantId;
    private Boolean isCurrent;
    
    /**
     * Nested class for user information.
     */
    public static class UserInfo {
        private UUID userId;
        private String username;
        private String email;
        
        public UserInfo() {
        }
        
        public UserInfo(UUID userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }
        
        public UUID getUserId() {
            return userId;
        }
        
        public void setUserId(UUID userId) {
            this.userId = userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
    
    // Constructors
    
    public UserSessionResponse() {
    }
    
    public UserSessionResponse(UUID sessionId, UserInfo user, String refreshToken, String ipAddress, 
                               String location, String userAgent, String device, Boolean isMobile, Boolean isDesktop,
                               long createdAt, long lastActivityAt, String authMethod, String tenantId, Boolean isCurrent) {
        this.sessionId = sessionId;
        this.user = user;
        this.refreshToken = refreshToken;
        this.ipAddress = ipAddress;
        this.location = location;
        this.userAgent = userAgent;
        this.device = device;
        this.isMobile = isMobile;
        this.isDesktop = isDesktop;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.authMethod = authMethod;
        this.tenantId = tenantId;
        this.isCurrent = isCurrent;
    }
    
    // Getters and Setters
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
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
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
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
    
    public Boolean getIsCurrent() {
        return isCurrent;
    }
    
    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
}
