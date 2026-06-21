package com.aidentx.core.notifications_management.enums;

/**
 * Enumeration representing different authentication methods for email services.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public enum AuthMethod {
    
    /**
     * Authentication using username and password credentials.
     */
    USERNAME_PASSWORD,
    
    /**
     * Authentication using API key.
     */
    API_KEY,
    
    /**
     * OAuth 2.0 authentication method.
     */
    OAUTH2
}
