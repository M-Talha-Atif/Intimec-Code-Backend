package com.aidentx.iam.shadow_management.controller;

import com.aidentx.iam.shadow_management.service.ApplicationPurgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST endpoints for cross-service operations.
 * These routes are excluded from JWT authentication (see SecurityConstants.PUBLIC_ENDPOINTS).
 * They must ONLY be called from trusted internal services (core-service), never from the public internet.
 */
@RestController
@RequestMapping("/api/v1/internal/applications")
public class InternalApplicationController {

    private final ApplicationPurgeService applicationPurgeService;

    public InternalApplicationController(ApplicationPurgeService applicationPurgeService) {
        this.applicationPurgeService = applicationPurgeService;
    }

    /**
     * Purges all identity-service data for the given application.
     * Called by core-service during application deletion.
     *
     * @param resourceId the application UUID
     * @param tenantId   tenant context, passed via X-Tenant-ID header
     */
    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> purgeApplicationData(
            @PathVariable String resourceId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        applicationPurgeService.purgeApplicationData(resourceId, tenantId);
        return ResponseEntity.noContent().build();
    }
}
