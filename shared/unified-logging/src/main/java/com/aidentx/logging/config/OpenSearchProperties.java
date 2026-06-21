package com.aidentx.logging.config;

/**
 * Defines the configuration shape for the OpenSearch appender.
 * All properties are prefixed with 'unified.logging.opensearch'.
 */
public class OpenSearchProperties {

    /**
     * Set to true to enable sending logs to OpenSearch.
     */
    private boolean enabled = false;

    /**
     * The host of the OpenSearch cluster (e.g., 'my-opensearch.company.com').
     */
    private String host = "localhost";

    /**
     * The port of the OpenSearch cluster (e.g., 9200).
     */
    private int port = 9200;

    /**
     * The protocol to use (http or https).
     */
    private String protocol = "http";

    /**
     * The username for authenticating with OpenSearch.
     */
    private String username;

    /**
     * The password for authenticating with OpenSearch.
     */
    private String password;

    /**
     * The name of the OpenSearch index. Can include date patterns.
     */
    private String indexName = "microservice-logs";

    // --- Getters and Setters ---
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
}