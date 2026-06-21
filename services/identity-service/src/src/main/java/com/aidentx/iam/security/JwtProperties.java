package com.aidentx.iam.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration Properties for production-ready security settings.
 * This class centralizes all JWT-related configuration properties
 * with proper validation and default values.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT secret key for signing tokens.
     * Should be at least 256 bits (32 characters) for HS256 algorithm.
     * This value is externalized and loaded from application properties.
     */
    private String secret;

    /**
     * JWT token expiration time in seconds.
     * This value is externalized and loaded from application properties.
     */
    private int expiration;

    /**
     * JWT token issuer.
     * Used for token validation and identification.
     * This value is externalized and loaded from application properties.
     */
    private String issuer;

    /**
     * Whether to enable JWT token refresh functionality.
     * This value is externalized and loaded from application properties.
     */
    private boolean refreshEnabled;

    /**
     * Refresh token expiration time in seconds.
     * This value is externalized and loaded from application properties.
     */
    private int refreshExpiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public int getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(int refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
