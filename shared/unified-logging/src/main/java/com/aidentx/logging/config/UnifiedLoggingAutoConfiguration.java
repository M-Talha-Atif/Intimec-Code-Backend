package com.aidentx.logging.config;

import com.aidentx.logging.interceptor.CorrelationIdInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for the Unified Logging library.
 */
@Configuration(proxyBeanMethods = false) // Use lite mode for configuration
@EnableConfigurationProperties(UnifiedLoggingProperties.class)
@ConditionalOnProperty(prefix = "unified.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.aidentx.logging")
public class UnifiedLoggingAutoConfiguration {

    /**
     * Provides a centrally configured ObjectMapper for JSON serialization in logs.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper unifiedLoggingObjectMapper(UnifiedLoggingProperties properties) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        if (properties.getJsonFormat().isPrettyPrint()) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        if (!properties.getJsonFormat().isIncludeNulls()) {
            mapper.setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        }
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Nested configuration for RestTemplate integration.
     * This entire class will only be processed if RestTemplate is on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplate.class)
    protected static class RestTemplateCorrelationIdConfiguration {

        @Bean
        @ConditionalOnBean(RestTemplate.class)
        public RestTemplateCustomizer correlationIdRestTemplateCustomizer(UnifiedLoggingProperties properties) {
            return restTemplate -> {
                restTemplate.getInterceptors().add(
                        new CorrelationIdInterceptor(properties.getCorrelationId().getHeaderName())
                );
            };
        }
    }

    /**
     * Nested configuration for WebClient integration.
     * This entire class will only be processed if WebClient is on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebClient.class)
    protected static class WebClientCorrelationIdConfiguration {

        @Bean
        public org.springframework.boot.web.reactive.function.client.WebClientCustomizer correlationIdWebClientCustomizer(UnifiedLoggingProperties properties) {
            return webClientBuilder -> {
                webClientBuilder.defaultRequest(requestHeaders -> {
                    final String correlationId = MDC.get(com.aidentx.logging.filter.CorrelationIdFilter.MDC_KEY_CORRELATION_ID);
                    if (correlationId != null) {
                        requestHeaders.header(properties.getCorrelationId().getHeaderName(), correlationId);
                    }
                });
            };
        }
    }
}