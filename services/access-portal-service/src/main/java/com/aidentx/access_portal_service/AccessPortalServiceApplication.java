package com.aidentx.access_portal_service;

import com.aidentx.logging.config.UnifiedLoggingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * Access Portal Service - Production-grade
 */
@SpringBootApplication
@EnableAspectJAutoProxy
//@Import(UnifiedLoggingAutoConfiguration.class)
public class AccessPortalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessPortalServiceApplication.class, args);
    }
}
