package com.aidentx.access_portal_service.controller;


import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final IUnifiedLogger logger;

    @Autowired
    public HealthController(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("AccessPortalService", "health");
    }

    @GetMapping("/health")
    public String health() {
        logger.info()
                .message("Health check endpoint called")
                .detail("endpoint", "/health")
                .objectType("health-check")
                .log();

        return "Access Portal Service is running!";
    }
}