package com.aidentx.llmservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * ManualGrpcConfiguration disabled - gRPC configuration-service calls removed.
 * Kept as no-op to avoid bean configuration errors.
 */
@Configuration
public class ManualGrpcConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ManualGrpcConfiguration.class);

    // gRPC configuration-service integration removed
    // This class is kept as no-op to prevent Spring configuration errors
    
}
