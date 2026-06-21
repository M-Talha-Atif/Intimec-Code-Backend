package com.aidentx.authservice.config;

import java.util.List;
import java.util.UUID;

/**
 * Configuration object representing the settings for an OAuth2 authentication
 * provider.
 * <p>
 * This class is used to transfer and validate OAuth2 connection and attribute
 * mapping parameters
 * for external authentication scenarios. It maps directly to the structure of
 * the OAuth2 provider
 * configuration JSON and supports all fields required for standard OAuth2 and
 * OpenID Connect flows.
 *
 * <p>
 * Fields:
 * <ul>
 * <li><b>enabled</b>: Whether this OAuth2 provider is enabled</li>
 * <li><b>name</b>: Display name for the provider (e.g., "Google OAuth2")</li>
 * <li><b>providerId</b>: Unique provider ID (e.g., "google")</li>
 * <li><b>clientId</b>: OAuth2 client ID</li>
 * <li><b>clientSecret</b>: OAuth2 client secret</li>
 * <li><b>authorizationUri</b>: Authorization endpoint URI</li>
 * <li><b>tokenUri</b>: Token endpoint URI</li>
 * <li><b>userInfoUri</b>: User info endpoint URI</li>
 * <li><b>jwkSetUri</b>: JWK set URI for public keys (for OIDC)</li>
 * <li><b>redirectUri</b>: Redirect URI after authentication</li>
 * <li><b>scopes</b>: List of OAuth2 scopes</li>
 * <li><b>userNameAttribute</b>: Attribute in user info response to use as
 * username</li>
 * <li><b>emailAttributeMapping</b>: JSON attribute mapping for email</li>
 * <li><b>firstNameAttributeMapping</b>: JSON attribute mapping for first
 * name</li>
 * <li><b>lastNameAttributeMapping</b>: JSON attribute mapping for last
 * name</li>
 * <li><b>displayNameAttributeMapping</b>: JSON attribute mapping for display
 * name</li>
 * <li><b>fullyEncrypted</b>: Whether all sensitive fields are encrypted at
 * rest</li>
 * </ul>
 *
 * <p>
 * All fields have standard getters and setters.
 */
public class OAuth2Config {
    private UUID id;

    /** Whether this OAuth2 provider is enabled. */
    private boolean enabled;
    /** Display name of the provider (e.g., Google OAuth2). */
    private String name;
    /** Unique provider ID (e.g., google). */
    private String providerId;
    /** OAuth2 client ID. */
    private String clientId;
    /** OAuth2 client secret. */
    private String clientSecret;
    /** Authorization endpoint URI. */
    private String authorizationUri;
    /** Token endpoint URI. */
    private String tokenUri;
    /** User info endpoint URI. */
    private String userInfoUri;
    /** JWK set URI for public keys. */
    private String jwkSetUri;
    /** Redirect URI after authentication. */
    private String redirectUri;
    /** List of OAuth2 scopes. */
    private List<String> scopes;
    /** The attribute in the user info response to use as username. */
    private String userNameAttribute;
    /** JSON attribute mapping for email. */
    private String emailAttributeMapping;
    /** JSON attribute mapping for first name. */
    private String firstNameAttributeMapping;
    /** JSON attribute mapping for last name. */
    private String lastNameAttributeMapping;
    /** JSON attribute mapping for display name. */
    private String displayNameAttributeMapping;
    /** Whether all sensitive fields are encrypted. */
    private boolean fullyEncrypted;

    // Getters and setters


    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return this.id;
    }
    /**
     * Returns whether this OAuth2 provider is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this OAuth2 provider is enabled.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the display name for the provider.
     * 
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name for the provider.
     * 
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the unique provider ID.
     * 
     * @return the provider ID
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the unique provider ID.
     * 
     * @param providerId the provider ID
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * Gets the OAuth2 client ID.
     * 
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the OAuth2 client ID.
     * 
     * @param clientId the client ID
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the OAuth2 client secret.
     * 
     * @return the client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the OAuth2 client secret.
     * 
     * @param clientSecret the client secret
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Gets the authorization endpoint URI.
     * 
     * @return the authorization URI
     */
    public String getAuthorizationUri() {
        return authorizationUri;
    }

    /**
     * Sets the authorization endpoint URI.
     * 
     * @param authorizationUri the authorization URI
     */
    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    /**
     * Gets the token endpoint URI.
     * 
     * @return the token URI
     */
    public String getTokenUri() {
        return tokenUri;
    }

    /**
     * Sets the token endpoint URI.
     * 
     * @param tokenUri the token URI
     */
    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    /**
     * Gets the user info endpoint URI.
     * 
     * @return the user info URI
     */
    public String getUserInfoUri() {
        return userInfoUri;
    }

    /**
     * Sets the user info endpoint URI.
     * 
     * @param userInfoUri the user info URI
     */
    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    /**
     * Gets the JWK set URI for public keys.
     * 
     * @return the JWK set URI
     */
    public String getJwkSetUri() {
        return jwkSetUri;
    }

    /**
     * Sets the JWK set URI for public keys.
     * 
     * @param jwkSetUri the JWK set URI
     */
    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    /**
     * Gets the redirect URI after authentication.
     * 
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the redirect URI after authentication.
     * 
     * @param redirectUri the redirect URI
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the list of OAuth2 scopes.
     * 
     * @return the list of scopes
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Sets the list of OAuth2 scopes.
     * 
     * @param scopes the list of scopes
     */
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    /**
     * Gets the attribute in the user info response to use as username.
     * 
     * @return the user name attribute
     */
    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    /**
     * Sets the attribute in the user info response to use as username.
     * 
     * @param userNameAttribute the user name attribute
     */
    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    /**
     * Gets the JSON attribute mapping for email.
     * 
     * @return the email attribute mapping
     */
    public String getEmailAttributeMapping() {
        return emailAttributeMapping;
    }

    /**
     * Sets the JSON attribute mapping for email.
     * 
     * @param emailAttributeMapping the email attribute mapping
     */
    public void setEmailAttributeMapping(String emailAttributeMapping) {
        this.emailAttributeMapping = emailAttributeMapping;
    }

    /**
     * Gets the JSON attribute mapping for first name.
     * 
     * @return the first name attribute mapping
     */
    public String getFirstNameAttributeMapping() {
        return firstNameAttributeMapping;
    }

    /**
     * Sets the JSON attribute mapping for first name.
     * 
     * @param firstNameAttributeMapping the first name attribute mapping
     */
    public void setFirstNameAttributeMapping(String firstNameAttributeMapping) {
        this.firstNameAttributeMapping = firstNameAttributeMapping;
    }

    /**
     * Gets the JSON attribute mapping for last name.
     * 
     * @return the last name attribute mapping
     */
    public String getLastNameAttributeMapping() {
        return lastNameAttributeMapping;
    }

    /**
     * Sets the JSON attribute mapping for last name.
     * 
     * @param lastNameAttributeMapping the last name attribute mapping
     */
    public void setLastNameAttributeMapping(String lastNameAttributeMapping) {
        this.lastNameAttributeMapping = lastNameAttributeMapping;
    }

    /**
     * Gets the JSON attribute mapping for display name.
     * 
     * @return the display name attribute mapping
     */
    public String getDisplayNameAttributeMapping() {
        return displayNameAttributeMapping;
    }

    /**
     * Sets the JSON attribute mapping for display name.
     * 
     * @param displayNameAttributeMapping the display name attribute mapping
     */
    public void setDisplayNameAttributeMapping(String displayNameAttributeMapping) {
        this.displayNameAttributeMapping = displayNameAttributeMapping;
    }

    /**
     * Returns whether all sensitive fields are encrypted at rest.
     * 
     * @return true if fully encrypted, false otherwise
     */
    public boolean isFullyEncrypted() {
        return fullyEncrypted;
    }

    /**
     * Sets whether all sensitive fields are encrypted at rest.
     * 
     * @param fullyEncrypted true if fully encrypted, false otherwise
     */
    public void setFullyEncrypted(boolean fullyEncrypted) {
        this.fullyEncrypted = fullyEncrypted;
    }
}
