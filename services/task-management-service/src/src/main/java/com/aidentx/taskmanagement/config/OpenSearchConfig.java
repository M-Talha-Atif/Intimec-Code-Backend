package com.aidentx.taskmanagement.config;

import java.net.URI;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenSearch client. Sets up connection to OpenSearch for task execution
 * log storage and streaming. Uses the modern OpenSearch Java client API with Apache HttpClient 5
 * Transport.
 */
@Configuration
public class OpenSearchConfig {

  @Value("${opensearch.url}")
  private String opensearchUrl;

  @Value("${opensearch.username}")
  private String username;

  @Value("${opensearch.password}")
  private String password;

  @Bean
  public OpenSearchClient openSearchClient() {
    try {
      URI uri = URI.create(opensearchUrl);

      final HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
      final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      // Set credentials for authentication
      credentialsProvider.setCredentials(
          new AuthScope(host), new UsernamePasswordCredentials(username, password.toCharArray()));

      // Build the transport using Apache HttpClient 5
      final ApacheHttpClient5TransportBuilder builder =
          ApacheHttpClient5TransportBuilder.builder(host);
      builder.setHttpClientConfigCallback(
          httpClientBuilder -> {

            // TODO: In production, implement proper TLS strategy with certificate
            // validation

            // Configure connection manager with NO TLS strategy
            final PoolingAsyncClientConnectionManager connectionManager =
                PoolingAsyncClientConnectionManagerBuilder.create().build();

            // Set credentials and connection manager
            return httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(connectionManager);
          });

      // Build the transport and create the OpenSearch client
      final OpenSearchTransport transport = builder.build();
      return new OpenSearchClient(transport);

    } catch (Exception e) {
      throw new RuntimeException(TaskManagementConstants.ERR_FAILED_CREATE_OPENSEARCH_CLIENT, e);
    }
  }
}
