package com.aidentx.workflowsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST client settings.
 * Provides RestTemplate bean for service-to-service REST communication.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Configuration
public class RestClientConfig {

    @Value("${rest.client.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${rest.client.read.timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
