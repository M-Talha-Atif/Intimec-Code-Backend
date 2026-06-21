package com.aidentx.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Identity Providers (IdPs).
 * <p>
 * Binds a list of IdP configurations from application properties using the prefix <b>idps</b>.
 * Used to support multiple external authentication providers (e.g., Google, Microsoft).
 */
@ConfigurationProperties(prefix = "idps")
public class IdpConfigProperties {

    /**
     * List of configured Identity Providers.
     * Each entry represents a single IdP and its connection details.
     */
    private List<Idp> idps;

    /**
     * Returns the list of configured IdPs.
     * @return list of IdP objects
     */
    public List<Idp> getIdps() {
        return idps;
    }

    /**
     * Sets the list of configured IdPs.
     * @param idps list of IdP objects
     */
    public void setIdps(List<Idp> idps) {
        this.idps = idps;
    }

    /**
     * Represents a single Identity Provider (e.g., Google, Microsoft).
     * Contains all required connection and identification details for the IdP.
     */
    public static class Idp {

        /** Unique identifier for the IdP (e.g., 'google', 'microsoft'). */
        private String id;
        /** Display name for the IdP (e.g., 'Google'). */
        private String name;
        /** Authorization endpoint URI for OAuth2/OpenID Connect. */
        private String authorizationUri;
        /** Client ID for OAuth2/OpenID Connect. */
        private String clientId;
        /** Redirect URI after authentication. */
        private String redirectUri;

        /**
         * Gets the unique identifier for the IdP.
         * @return the IdP id
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the unique identifier for the IdP.
         * @param id the IdP id
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Gets the display name for the IdP.
         * @return the IdP name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the display name for the IdP.
         * @param name the IdP name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the authorization endpoint URI.
         * @return the authorization URI
         */
        public String getAuthorizationUri() {
            return authorizationUri;
        }

        /**
         * Sets the authorization endpoint URI.
         * @param authorizationUri the authorization URI
         */
        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        /**
         * Gets the client ID for OAuth2/OpenID Connect.
         * @return the client ID
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Sets the client ID for OAuth2/OpenID Connect.
         * @param clientId the client ID
         */
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        /**
         * Gets the redirect URI after authentication.
         * @return the redirect URI
         */
        public String getRedirectUri() {
            return redirectUri;
        }

        /**
         * Sets the redirect URI after authentication.
         * @param redirectUri the redirect URI
         */
        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
