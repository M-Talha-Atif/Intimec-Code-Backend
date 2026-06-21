package com.aidentx.iam.identity_management.aspect;

import com.aidentx.iam.config.audit.AuditEventPublisher;
import com.aidentx.iam.config.audit.SecurityContextService;
import com.aidentx.iam.identity_management.repository.ExtendedAttributeDefinitionRepository;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for audit aspects in the Identity service.
 * This centralizes the configuration of all audit-related aspects
 * and provides a single point for enabling/disabling audit functionality.
 *
 * @since 1.0
 */
@Configuration
public class AuditAspectConfiguration {

    /**
     * Creates and configures the extended attribute definition audit aspect.
     *
     * @param loggerFactory Factory for creating unified loggers
     * @param securityContextService Service for extracting security context information
     * @param auditPublisher Publisher for audit events to Kafka
     * @param definitionRepository Repository for extended attribute definitions
     * @param objectMapper Object mapper for JSON serialization
     * @return Configured audit aspect instance
     */
    @Bean
    public ExtendedAttributeDefinitionAuditAspect extendedAttributeDefinitionAuditAspect(
            UnifiedLoggerFactory loggerFactory,
            SecurityContextService securityContextService,
            AuditEventPublisher auditPublisher,
            ExtendedAttributeDefinitionRepository definitionRepository,
            ObjectMapper objectMapper) {
        return new ExtendedAttributeDefinitionAuditAspect(
            loggerFactory,
            securityContextService,
            auditPublisher,
            definitionRepository,
            objectMapper
        );
    }
}