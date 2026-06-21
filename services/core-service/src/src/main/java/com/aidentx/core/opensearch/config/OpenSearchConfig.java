package com.aidentx.core.opensearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * OpenSearch configuration for the core service.
 * 
 * <p>This configuration class provides all necessary beans and settings for connecting to OpenSearch.
 * It handles authentication, connection parameters, and provides RestTemplate for HTTP operations.
 * 
 * <h3>Configuration Properties:</h3>
 * <ul>
 *   <li><code>opensearch.url</code> - OpenSearch cluster URL (default: http://localhost:9200)</li>
 *   <li><code>opensearch.username</code> - Username for basic authentication (default: admin)</li>
 *   <li><code>opensearch.password</code> - Password for basic authentication (default: !Admin123456)</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * # application.properties for local development
 * opensearch.url=http://localhost:9200
 * opensearch.username=admin
 * opensearch.password=!Admin123456
 * 
 * # Docker environment variables
 * OPENSEARCH_URL=http://opensearch:9200
 * OPENSEARCH_USERNAME=admin
 * OPENSEARCH_PASSWORD=!Admin123456
 * </pre>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-07-14
 */
@Configuration
public class OpenSearchConfig {

    /**
     * OpenSearch cluster URL.
     * Can be overridden by setting the 'opensearch.url' property or 'OPENSEARCH_URL' environment variable.
     * Default value points to localhost for local development.
     */
    @Value("${opensearch.url:http://localhost:9200}")
    private String opensearchUrl;

    /**
     * Username for OpenSearch basic authentication.
     * Can be overridden by setting the 'opensearch.username' property or 'OPENSEARCH_USERNAME' environment variable.
     * Default is 'admin' which is the default OpenSearch admin user.
     */
    @Value("${opensearch.username:admin}")
    private String username;

    /**
     * Password for OpenSearch basic authentication.
     * Can be overridden by setting the 'opensearch.password' property or 'OPENSEARCH_PASSWORD' environment variable.
     * Default matches the OpenSearch default admin password.
     */
    @Value("${opensearch.password:!Admin123456}")
    private String password;

    /**
     * Creates a RestTemplate bean for HTTP operations with OpenSearch.
     * 
     * <p>This RestTemplate is used by OpenSearchService to perform HTTP requests
     * against the OpenSearch REST API. It handles all CRUD operations including
     * search, update, and document management.
     * 
     * @return configured RestTemplate instance
     * @see org.springframework.web.client.RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates an OpenSearchProperties bean containing all connection configuration.
     * 
     * <p>This bean encapsulates all OpenSearch connection parameters and is injected
     * into services that need to communicate with OpenSearch. It provides a centralized
     * way to manage OpenSearch configuration across the application.
     * 
     * <h3>Configuration Sources (in order of precedence):</h3>
     * <ol>
     *   <li>Environment variables (OPENSEARCH_URL, OPENSEARCH_USERNAME, OPENSEARCH_PASSWORD)</li>
     *   <li>System properties (opensearch.url, opensearch.username, opensearch.password)</li>
     *   <li>application.properties file</li>
     *   <li>Default values specified in @Value annotations</li>
     * </ol>
     * 
     * @return configured OpenSearchProperties instance with connection details
     * @see OpenSearchProperties
     */
    @Bean
    public OpenSearchProperties openSearchProperties() {
        OpenSearchProperties properties = new OpenSearchProperties();
        properties.setUrl(opensearchUrl);
        properties.setUsername(username);
        properties.setPassword(password);
        return properties;
    }

    /**
     * Configuration properties holder for OpenSearch connection details.
     * 
     * <p>This class encapsulates all the necessary connection parameters for OpenSearch
     * including URL, authentication credentials, and provides getter/setter methods
     * for configuration management.
     * 
     * <h3>Thread Safety:</h3>
     * This class is not thread-safe by design as it's intended to be configured once
     * during application startup and then used in read-only mode.
     * 
     * <h3>Example Usage:</h3>
     * <pre>
     * {@code
     * @Autowired
     * private OpenSearchProperties openSearchProperties;
     * 
     * public void connectToOpenSearch() {
     *     String url = openSearchProperties.getUrl();
     *     String auth = openSearchProperties.getUsername() + ":" + openSearchProperties.getPassword();
     *     // Use connection details...
     * }
     * }
     * </pre>
     * 
     * @see OpenSearchConfig#openSearchProperties()
     */
    public static class OpenSearchProperties {
        
        /** The OpenSearch cluster URL (e.g., http://localhost:9200) */
        private String url;
        
        /** Username for basic authentication */
        private String username;
        
        /** Password for basic authentication */
        private String password;

        /**
         * Gets the OpenSearch cluster URL.
         * 
         * @return the OpenSearch URL, never null after proper configuration
         */
        public String getUrl() {
            return url;
        }

        /**
         * Sets the OpenSearch cluster URL.
         * 
         * @param url the OpenSearch URL (e.g., http://localhost:9200, http://opensearch:9200)
         * @throws IllegalArgumentException if url is null or empty
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Gets the username for OpenSearch authentication.
         * 
         * @return the username, may be null if authentication is disabled
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the username for OpenSearch authentication.
         * 
         * @param username the username for basic auth, may be null
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Gets the password for OpenSearch authentication.
         * 
         * @return the password, may be null if authentication is disabled
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the password for OpenSearch authentication.
         * 
         * @param password the password for basic auth, may be null
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Checks if authentication is configured.
         * 
         * @return true if both username and password are set, false otherwise
         */
        public boolean isAuthenticationEnabled() {
            return username != null && password != null && 
                   !username.trim().isEmpty() && !password.trim().isEmpty();
        }

        /**
         * Returns a string representation of the OpenSearch configuration.
         * Note: Password is masked for security reasons.
         * 
         * @return string representation with masked password
         */
        @Override
        public String toString() {
            return "OpenSearchProperties{" +
                    "url='" + url + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + (password != null ? "****" : "null") + '\'' +
                    ", authEnabled=" + isAuthenticationEnabled() +
                    '}';
        }
    }
}
