package com.aidentx.authservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Configuration object representing the settings for SAML authentication.
 * <p>
 * This class is used to transfer and validate SAML connection, identity
 * provider, service provider,
 * and attribute mapping parameters for enterprise authentication scenarios. It
 * maps directly to the
 * structure of the SAML provider configuration JSON and supports all fields
 * required for standard
 * SAML 2.0 flows.
 *
 * <p>
 * Fields:
 * <ul>
 * <li><b>enabled</b>: Whether SAML authentication is enabled</li>
 * <li><b>name</b>: Display name for the SAML provider</li>
 * <li><b>idpEntityId</b>: Entity ID / Issuer from the Identity Provider
 * (IdP)</li>
 * <li><b>idpSsoUrl</b>: SSO Login URL from the IdP</li>
 * <li><b>idpX509Certificate</b>: Public X.509 Certificate from the IdP (PEM
 * format)</li>
 * <li><b>entityId</b>: Entity ID / Issuer for our application (Service
 * Provider)</li>
 * <li><b>assertionConsumerServiceUrl</b>: SAML Assertion Consumer Service (ACS)
 * URL</li>
 * <li><b>samlBinding</b>: SAML binding type (POST or REDIRECT)</li>
 * <li><b>nameIdFormat</b>: SAML Name ID Format</li>
 * <li><b>correlationRule</b>: SAML Correlation Rule (optional)</li>
 * <li><b>singleLogoutServiceUrl</b>: Single Logout Service URL (optional)</li>
 * <li><b>metadataUrl</b>: Metadata URL (optional)</li>
 * <li><b>keyStoreLocation</b>: Keystore location for SAML signing</li>
 * <li><b>keyStorePassword</b>: Keystore password for SAML signing</li>
 * <li><b>keyAlias</b>: Key alias for SAML signing</li>
 * <li><b>emailAttribute</b>: Attribute mapping for email</li>
 * <li><b>firstNameAttribute</b>: Attribute mapping for first name</li>
 * <li><b>lastNameAttribute</b>: Attribute mapping for last name</li>
 * <li><b>displayNameAttribute</b>: Attribute mapping for display name</li>
 * <li><b>fullyEncrypted</b>: Whether all sensitive fields are encrypted at
 * rest</li>
 * </ul>
 *
 * <p>
 * All fields have standard getters and setters.
 */
public class SamlConfig {
    private UUID id;
    private boolean enabled;
    private String name;

    // Identity Provider (IdP) Settings
    private String idpEntityId; // Entity ID / Issuer from IdP
    private String idpSsoUrl; // SSO Login URL from IdP
    private String idpX509Certificate; // Public X.509 Certificate from IdP (PEM format)

    // Service Provider (Our Application) Settings
    private String entityId; // Entity ID / Issuer for our application
    private String assertionConsumerServiceUrl; // SAML URL (ACS) - where IdP sends responses
    private String samlBinding; // SAML Binding (POST or REDIRECT)
    private String nameIdFormat; // SAML Name ID Format
    private String correlationRule; // SAML Correlation Rule (optional)

    // Optional settings
    private String singleLogoutServiceUrl;
    private String metadataUrl;

    // Keystore settings for SAML signing
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyAlias;

    // Attribute mappings for user data
    @JsonProperty("emailAttributeMapping")
    private String emailAttribute;
    @JsonProperty("firstNameAttributeMapping")
    private String firstNameAttribute;
    @JsonProperty("lastNameAttributeMapping")
    private String lastNameAttribute;
    @JsonProperty("displayNameAttributeMapping")
    private String displayNameAttribute;

    // Encryption metadata
    private boolean fullyEncrypted;

    // Default constructor
    public SamlConfig() {
        this.enabled = true; // Default to enabled when creating new SAML config
        this.samlBinding = "POST"; // Default to POST binding
        this.nameIdFormat = "emailAddress"; // Default to email format
    }

    /**
     * Returns whether SAML authentication is enabled for this provider.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return this.id;
    }

    /**
     * Sets whether SAML authentication is enabled for this provider.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the display name for the SAML provider.
     * 
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name for the SAML provider.
     * 
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }

    // IdP Settings
    /**
     * Gets the Entity ID / Issuer from the Identity Provider (IdP).
     * 
     * @return the IdP Entity ID
     */
    public String getIdpEntityId() {
        return idpEntityId;
    }

    /**
     * Sets the Entity ID / Issuer from the Identity Provider (IdP).
     * 
     * @param idpEntityId the IdP Entity ID
     */
    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    /**
     * Gets the SSO Login URL from the Identity Provider (IdP).
     * 
     * @return the IdP SSO URL
     */
    public String getIdpSsoUrl() {
        return idpSsoUrl;
    }

    /**
     * Sets the SSO Login URL from the Identity Provider (IdP).
     * 
     * @param idpSsoUrl the IdP SSO URL
     */
    public void setIdpSsoUrl(String idpSsoUrl) {
        this.idpSsoUrl = idpSsoUrl;
    }

    /**
     * Gets the public X.509 certificate from the Identity Provider (IdP).
     * 
     * @return the IdP X.509 certificate (PEM format)
     */
    public String getIdpX509Certificate() {
        return idpX509Certificate;
    }

    /**
     * Sets the public X.509 certificate from the Identity Provider (IdP).
     * 
     * @param idpX509Certificate the IdP X.509 certificate (PEM format)
     */
    public void setIdpX509Certificate(String idpX509Certificate) {
        this.idpX509Certificate = idpX509Certificate;
    }

    // Service Provider Settings
    /**
     * Gets the Entity ID / Issuer for the service provider (our application).
     * 
     * @return the service provider Entity ID
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Sets the Entity ID / Issuer for the service provider (our application).
     * 
     * @param entityId the service provider Entity ID
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * Gets the SAML Assertion Consumer Service (ACS) URL.
     * 
     * @return the ACS URL
     */
    public String getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    /**
     * Sets the SAML Assertion Consumer Service (ACS) URL.
     * 
     * @param assertionConsumerServiceUrl the ACS URL
     */
    public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
    }

    /**
     * Gets the SAML binding type (POST or REDIRECT).
     * 
     * @return the SAML binding type
     */
    public String getSamlBinding() {
        return samlBinding;
    }

    /**
     * Sets the SAML binding type (POST or REDIRECT).
     * 
     * @param samlBinding the SAML binding type
     */
    public void setSamlBinding(String samlBinding) {
        this.samlBinding = samlBinding;
    }

    /**
     * Gets the SAML Name ID Format.
     * 
     * @return the SAML Name ID Format
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the SAML Name ID Format.
     * 
     * @param nameIdFormat the SAML Name ID Format
     */
    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    /**
     * Gets the SAML Correlation Rule (optional).
     * 
     * @return the SAML Correlation Rule
     */
    public String getCorrelationRule() {
        return correlationRule;
    }

    /**
     * Sets the SAML Correlation Rule (optional).
     * 
     * @param correlationRule the SAML Correlation Rule
     */
    public void setCorrelationRule(String correlationRule) {
        this.correlationRule = correlationRule;
    }

    // Optional Settings
    /**
     * Gets the Single Logout Service URL (optional).
     * 
     * @return the Single Logout Service URL
     */
    public String getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }

    /**
     * Sets the Single Logout Service URL (optional).
     * 
     * @param singleLogoutServiceUrl the Single Logout Service URL
     */
    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
    }

    /**
     * Gets the Metadata URL (optional).
     * 
     * @return the Metadata URL
     */
    public String getMetadataUrl() {
        return metadataUrl;
    }

    /**
     * Sets the Metadata URL (optional).
     * 
     * @param metadataUrl the Metadata URL
     */
    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    // Keystore Settings
    /**
     * Gets the keystore location for SAML signing.
     * 
     * @return the keystore location
     */
    public String getKeyStoreLocation() {
        return keyStoreLocation;
    }

    /**
     * Sets the keystore location for SAML signing.
     * 
     * @param keyStoreLocation the keystore location
     */
    public void setKeyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    /**
     * Gets the keystore password for SAML signing.
     * 
     * @return the keystore password
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Sets the keystore password for SAML signing.
     * 
     * @param keyStorePassword the keystore password
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Gets the key alias for SAML signing.
     * 
     * @return the key alias
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Sets the key alias for SAML signing.
     * 
     * @param keyAlias the key alias
     */
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    // Attribute Mappings
    /**
     * Gets the attribute mapping for email.
     * 
     * @return the email attribute mapping
     */
    public String getEmailAttribute() {
        return emailAttribute;
    }

    /**
     * Sets the attribute mapping for email.
     * 
     * @param emailAttribute the email attribute mapping
     */
    public void setEmailAttribute(String emailAttribute) {
        this.emailAttribute = emailAttribute;
    }

    /**
     * Gets the attribute mapping for first name.
     * 
     * @return the first name attribute mapping
     */
    public String getFirstNameAttribute() {
        return firstNameAttribute;
    }

    /**
     * Sets the attribute mapping for first name.
     * 
     * @param firstNameAttribute the first name attribute mapping
     */
    public void setFirstNameAttribute(String firstNameAttribute) {
        this.firstNameAttribute = firstNameAttribute;
    }

    /**
     * Gets the attribute mapping for last name.
     * 
     * @return the last name attribute mapping
     */
    public String getLastNameAttribute() {
        return lastNameAttribute;
    }

    /**
     * Sets the attribute mapping for last name.
     * 
     * @param lastNameAttribute the last name attribute mapping
     */
    public void setLastNameAttribute(String lastNameAttribute) {
        this.lastNameAttribute = lastNameAttribute;
    }

    /**
     * Gets the attribute mapping for display name.
     * 
     * @return the display name attribute mapping
     */
    public String getDisplayNameAttribute() {
        return displayNameAttribute;
    }

    /**
     * Sets the attribute mapping for display name.
     * 
     * @param displayNameAttribute the display name attribute mapping
     */
    public void setDisplayNameAttribute(String displayNameAttribute) {
        this.displayNameAttribute = displayNameAttribute;
    }

    // Encryption metadata
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
     * @param fullyEncrypted true to encrypt, false to not encrypt
     */
    public void setFullyEncrypted(boolean fullyEncrypted) {
        this.fullyEncrypted = fullyEncrypted;
    }
}
