package com.aidentx.core;

import com.aidentx.core.shared.security.JwtProperties;
import com.aidentx.logging.config.UnifiedLoggingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * Main application class for the aidentx Core Service.
 * 
 * This service provides audit logging and pinned logger functionality
 * with shared JWT-based authentication across all microservices.
 * 
 * Production-ready with proper configuration management and security.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableAspectJAutoProxy
@Import(UnifiedLoggingAutoConfiguration.class)
@ComponentScan(basePackages = {
        "com.aidentx.core",
        "com.aidentx.core.onboarding",
        "com.aidentx.shared",
        "com.aidentx.shared.dgraph",
        "com.aidentx.dal"
})
public class CoreServiceApplication {

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.json");

        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
