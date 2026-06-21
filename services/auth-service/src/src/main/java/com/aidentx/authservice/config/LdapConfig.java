package com.aidentx.authservice.config;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the configuration required for connecting to an LDAP server for
 * authentication.
 * <p>
 * This class contains only the essential fields needed for basic LDAP
 * authentication,
 * including connection details, search parameters, and encryption flags. It is
 * designed
 * to be used by {@link DynamicLdapAuthenticationProvider} and related services.
 * <p>
 * <b>Note:</b> The implementation of {@link #equals(Object)} and
 * {@link #hashCode()} is
 * essential for the DynamicLdapAuthenticationProvider's caching mechanism to
 * detect
 * configuration changes and reload the provider as needed.
 *
 * <h2>Fields:</h2>
 * <ul>
 * <li><b>enabled</b>: Whether LDAP authentication is enabled.</li>
 * <li><b>name</b>: Display name for the LDAP configuration (e.g., "Company
 * Active Directory").</li>
 * <li><b>url</b>: LDAP server URL (e.g., "ldap://dc.company.com:389").</li>
 * <li><b>baseDn</b>: Base DN for user searches (e.g.,
 * "dc=company,dc=com").</li>
 * <li><b>bindDn</b>: Service account DN for binding (e.g.,
 * "cn=ldapuser,ou=service,dc=company,dc=com").</li>
 * <li><b>bindPassword</b>: Service account password for binding.</li>
 * <li><b>userSearchFilter</b>: LDAP user search filter (e.g.,
 * "sAMAccountName={0}" or "uid={0}").</li>
 * <li><b>fullyEncrypted</b>: Whether the entire configuration is encrypted at
 * rest.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * 
 * <pre>
 * LdapConfig config = new LdapConfig();
 * config.setEnabled(true);
 * config.setName("Company AD");
 * config.setUrl("ldap://dc.company.com:389");
 * config.setBaseDn("dc=company,dc=com");
 * config.setBindDn("cn=ldapuser,ou=service,dc=company,dc=com");
 * config.setBindPassword("secret");
 * config.setUserSearchFilter("sAMAccountName={0}");
 * config.setFullyEncrypted(false);
 * </pre>
 */
public class LdapConfig {
    private UUID id;
    private boolean enabled;
    private String name; // Display name (e.g., "Company Active Directory")
    private String url; // LDAP server URL (e.g., "ldap://dc.company.com:389")
    private String baseDn; // Base DN for searches (e.g., "dc=company,dc=com")
    private String bindDn; // Service account DN (e.g., "cn=ldapuser,ou=service,dc=company,dc=com")
    private String bindPassword; // Service account password
    private String userSearchFilter; // User search filter (e.g., "sAMAccountName={0}" or "uid={0}")
    private boolean encrypted; // Flag indicating if the entire configuration is encrypted

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return this.id;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean fullyEncrypted) {
        this.encrypted = fullyEncrypted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LdapConfig that = (LdapConfig) o;
        return enabled == that.enabled &&
                encrypted == that.encrypted &&
                Objects.equals(name, that.name) &&
                Objects.equals(url, that.url) &&
                Objects.equals(baseDn, that.baseDn) &&
                Objects.equals(bindDn, that.bindDn) &&
                Objects.equals(bindPassword, that.bindPassword) &&
                Objects.equals(userSearchFilter, that.userSearchFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, name, url, baseDn, bindDn, bindPassword, userSearchFilter, encrypted);
    }
}