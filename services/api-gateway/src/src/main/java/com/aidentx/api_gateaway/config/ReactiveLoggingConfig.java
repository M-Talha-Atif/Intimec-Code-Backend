package com.aidentx.api_gateaway.config;

import com.aidentx.logging.config.UnifiedLoggingProperties;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.logging.util.CorrelationIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for reactive-compatible unified logging components.
 * This configuration only imports the components that work with WebFlux/reactive environments.
 */
@Configuration
@EnableConfigurationProperties(UnifiedLoggingProperties.class)
public class ReactiveLoggingConfig {

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
     * Factory for creating unified loggers.
     */
    @Bean
    @ConditionalOnMissingBean
    public UnifiedLoggerFactory unifiedLoggerFactory(ObjectMapper objectMapper) {
        return new UnifiedLoggerFactory(objectMapper, "api-gateway");
    }

    /**
     * Generator for correlation IDs.
     */
    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdGenerator correlationIdGenerator() {
        return new CorrelationIdGenerator();
    }
}
