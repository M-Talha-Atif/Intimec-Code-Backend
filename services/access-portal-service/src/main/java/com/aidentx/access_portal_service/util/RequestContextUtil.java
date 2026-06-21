package com.aidentx.access_portal_service.util;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;


@Slf4j
public final class RequestContextUtil {

    private RequestContextUtil() {}

    public static UUID resolveTenantId(String tenantIdHeader) {

        log.info("Resolving tenantId from header: '{}'", tenantIdHeader);

        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            log.warn("X-Tenant-ID header missing or blank. Falling back to DEFAULT_TENANT_ID={}",
                    DEFAULT_TENANT_ID);
            return UUID.fromString(DEFAULT_TENANT_ID);
        }

        try {
            UUID tenantId = UUID.fromString(tenantIdHeader);
            log.info("Resolved tenantId successfully: {}", tenantId);
            return tenantId;
        } catch (IllegalArgumentException ex) {
            log.error("Invalid X-Tenant-ID value '{}'. Falling back to DEFAULT_TENANT_ID={}",
                    tenantIdHeader, DEFAULT_TENANT_ID);
            return UUID.fromString(DEFAULT_TENANT_ID);
        }
    }


    public static UUID resolveUserId(String userIdHeader) {
        try {
            return (userIdHeader != null && !userIdHeader.isBlank())
                    ? UUID.fromString(userIdHeader)
                    : UUID.fromString(DEFAULT_TENANT_ID);
        } catch (IllegalArgumentException ex) {
            return UUID.fromString(DEFAULT_TENANT_ID);
        }
    }
}

