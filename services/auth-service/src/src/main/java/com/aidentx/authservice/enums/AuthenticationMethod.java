package com.aidentx.authservice.enums;

/**
 * Enumeration of supported authentication methods in the system.
 *
 * <p>This enum defines the types of external or federated authentication mechanisms
 * that a user can use to authenticate within the platform. It is used to track and manage
 * authentication strategies, such as LDAP directory-based login, SAML-based SSO, or OAuth2/OpenID Connect.</p>
 *
 * <p>Typical usage includes recording which method was used during login,
 * configuring provider-specific settings, and enforcing method-specific policies.</p>
 */
public enum AuthenticationMethod {

    /**
     * Login via LDAP (Lightweight Directory Access Protocol).
     * Typically used in enterprise environments with Active Directory or OpenLDAP.
     */
    LDAP,

    /**
     * Login via SAML (Security Assertion Markup Language) Single Sign-On.
     * Commonly used for federated identity across organizations.
     */
    SAML,

    /**
     * Login via OAuth2 or OpenID Connect providers (e.g., Google, Microsoft Azure AD).
     */
    OAUTH2
}
