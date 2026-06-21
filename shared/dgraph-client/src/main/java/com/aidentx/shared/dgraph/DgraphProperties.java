package com.aidentx.shared.dgraph;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dgraph")
public class DgraphProperties {
    /**
     * Comma-separated list of Dgraph host:port combinations.
     * Example: localhost:9080,anotherhost:9080
     */
    private String hosts;

    public String getHosts() { return hosts; }
    public void setHosts(String hosts) { this.hosts = hosts; }
}