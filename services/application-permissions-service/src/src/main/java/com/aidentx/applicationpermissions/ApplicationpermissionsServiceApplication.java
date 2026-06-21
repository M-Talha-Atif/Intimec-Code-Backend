package com.aidentx.applicationpermissions;

import com.aidentx.applicationpermissions.shared.security.JwtProperties;
import com.aidentx.logging.config.UnifiedLoggingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Application Permissions Service.
 * 
 * This service provides role and organization management functionality
 * with shared JWT-based authentication and tenant isolation.
 * 
 * Production-ready with proper configuration management and security.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableAspectJAutoProxy
@EnableScheduling
@Import(UnifiedLoggingAutoConfiguration.class)
@ComponentScan(basePackages = {
        "com.aidentx.applicationpermissions",
        "com.aidentx.common",
        "com.aidentx.shared",
        "com.aidentx.shared.dgraph",
        "com.aidentx.dal"
})
public class ApplicationpermissionsServiceApplication {

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.json");

        SpringApplication.run(ApplicationpermissionsServiceApplication.class, args);
    }
}
