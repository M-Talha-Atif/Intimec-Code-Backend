package com.aidentx.applicationpermissions.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for REST client beans.
 * 
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-12-11
 */
@Configuration
public class RestConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests to other services.
     * 
     * @param builder the RestTemplateBuilder for configuring the RestTemplate
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
